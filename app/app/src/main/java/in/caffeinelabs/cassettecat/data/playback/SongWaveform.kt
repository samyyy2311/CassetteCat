package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.LruCache
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import java.nio.ByteOrder
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

const val WAVEFORM_BARS = 64

// Samples decoded at each point: about a tenth of a second at 44.1 kHz, enough to tell loud from quiet.
private const val SAMPLES_PER_BAR = 4096
private const val DEQUEUE_TIMEOUT_US = 10_000L

// How loud a song is at evenly spaced points, from 0 to 1, for the waveform seek bar. Only files on
// the phone are read: a streamed song would have to be downloaded to read it.
object SongWaveforms {
    private val cache = LruCache<String, FloatArray>(48)

    suspend fun load(context: Context, song: Song): FloatArray? {
        if (song.source != MusicSource.Local) return null
        cache.get(song.id)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { read(context, song) }.getOrNull()?.also { cache.put(song.id, it) }
        }
    }

    // Decodes a short slice at each point instead of the whole song, so a long file reads in well under a second.
    private suspend fun read(context: Context, song: Song): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, song.contentUri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val durationUs = format.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 } ?: return null
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            val peaks = FloatArray(WAVEFORM_BARS)
            val info = MediaCodec.BufferInfo()
            for (bar in 0 until WAVEFORM_BARS) {
                currentCoroutineContext().ensureActive()
                extractor.seekTo(durationUs * bar / WAVEFORM_BARS, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                decoder.flush()
                var samples = 0
                var peak = 0
                var inputDone = false
                while (samples < SAMPLES_PER_BAR) {
                    if (!inputDone) {
                        val inIndex = decoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                        if (inIndex >= 0) {
                            val size = extractor.readSampleData(decoder.getInputBuffer(inIndex)!!, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val outIndex = decoder.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
                    if (outIndex < 0) {
                        if (inputDone && outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                        continue
                    }
                    val pcm = decoder.getOutputBuffer(outIndex)!!.order(ByteOrder.nativeOrder()).asShortBuffer()
                    while (pcm.hasRemaining() && samples < SAMPLES_PER_BAR) {
                        peak = maxOf(peak, abs(pcm.get().toInt()))
                        samples++
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
                peaks[bar] = peak / Short.MAX_VALUE.toFloat()
            }
            // Scaled to the loudest point, so quiet masters still fill the bar.
            val loudest = peaks.maxOrNull()?.takeIf { it > 0f } ?: return null
            return FloatArray(WAVEFORM_BARS) { (peaks[it] / loudest).coerceIn(0f, 1f) }
        } finally {
            codec?.run {
                runCatching { stop() }
                release()
            }
            extractor.release()
        }
    }
}
