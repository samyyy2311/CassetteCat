@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.data.playback

import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt

// Audio is analysed before it reaches the speaker, about half a second early, so each set of bands
// keeps the song position it belongs to and the lyrics screen shows the set for what is playing now.
object AudioWaveformHolder {
    private const val CAPACITY = 256
    private val positions = LongArray(CAPACITY)
    private val bands = Array(CAPACITY) { FloatArray(BAND_COUNT) }
    private var size = 0
    private var next = 0
    private val watchers = AtomicInteger()

    // Only the lyrics screen draws the bands, so audio is analysed only while it is showing.
    val isWatched: Boolean get() = watchers.get() > 0

    fun watch() = watchers.incrementAndGet()

    fun unwatch() = watchers.decrementAndGet()

    @Synchronized
    fun record(positionMs: Long, values: FloatArray) {
        positions[next] = positionMs
        values.copyInto(bands[next])
        next = (next + 1) % CAPACITY
        if (size < CAPACITY) size++
    }

    // The latest set at or just before the position, or null when none was analysed for it.
    @Synchronized
    fun bandsAt(positionMs: Long): FloatArray? {
        var best = -1
        for (i in 0 until size) {
            val at = positions[i]
            if (at <= positionMs && positionMs - at < MAX_GAP_MS && (best < 0 || at > positions[best])) best = i
        }
        return if (best < 0) null else bands[best].copyOf()
    }

    @Synchronized
    fun clear() {
        size = 0
        next = 0
    }

    private const val MAX_GAP_MS = 150L
}

private const val BAND_COUNT = 7

class AudioWaveformProcessor private constructor() : BaseAudioProcessor() {
    private val currentBands = FloatArray(BAND_COUNT)
    // Song position of the next sample to arrive, in microseconds.
    private var positionUs = 0L
    private var timeline: Timeline = Timeline.EMPTY
    private var periodIndex = C.INDEX_UNSET
    private val period = Timeline.Period()

    private var lp0 = 0f
    private var lp1 = 0f
    private var lp2 = 0f
    private var lp3 = 0f
    private var lp4 = 0f
    private var lp5 = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && AudioWaveformHolder.isWatched) {
            val duplicate = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            process16BitPcm(duplicate, inputAudioFormat.channelCount)
        }
        advancePosition(remaining / inputAudioFormat.bytesPerFrame)

        val output = replaceOutputBuffer(remaining)
        output.put(inputBuffer)
        output.flip()
    }

    private fun process16BitPcm(buffer: ByteBuffer, channels: Int) {
        val shortBuffer = buffer.asShortBuffer()
        val totalShorts = shortBuffer.remaining()
        if (totalShorts < channels) return

        val frames = totalShorts / channels
        val step = maxOf(1, frames / 256)

        var sum0 = 0f
        var sum1 = 0f
        var sum2 = 0f
        var sum3 = 0f
        var sum4 = 0f
        var sum5 = 0f
        var sum6 = 0f
        var counted = 0

        var frameIndex = 0
        while (frameIndex < frames) {
            val sample = shortBuffer.get(frameIndex * channels) / 32768f

            lp0 += 0.02f * (sample - lp0)
            lp1 += 0.05f * (sample - lp1)
            lp2 += 0.12f * (sample - lp2)
            lp3 += 0.25f * (sample - lp3)
            lp4 += 0.45f * (sample - lp4)
            lp5 += 0.70f * (sample - lp5)

            val b0 = lp0
            val b1 = lp1 - lp0
            val b2 = lp2 - lp1
            val b3 = lp3 - lp2
            val b4 = lp4 - lp3
            val b5 = lp5 - lp4
            val b6 = sample - lp5

            sum0 += b0 * b0
            sum1 += b1 * b1
            sum2 += b2 * b2
            sum3 += b3 * b3
            sum4 += b4 * b4
            sum5 += b5 * b5
            sum6 += b6 * b6
            counted++

            frameIndex += step
        }

        if (counted == 0) return

        val rms0 = (sqrt(sum0 / counted) * 3.6f).coerceIn(0f, 1f)
        val rms1 = (sqrt(sum1 / counted) * 3.2f).coerceIn(0f, 1f)
        val rms2 = (sqrt(sum2 / counted) * 3.0f).coerceIn(0f, 1f)
        val rms3 = (sqrt(sum3 / counted) * 2.8f).coerceIn(0f, 1f)
        val rms4 = (sqrt(sum4 / counted) * 2.8f).coerceIn(0f, 1f)
        val rms5 = (sqrt(sum5 / counted) * 3.0f).coerceIn(0f, 1f)
        val rms6 = (sqrt(sum6 / counted) * 3.4f).coerceIn(0f, 1f)

        val target = floatArrayOf(rms0, rms1, rms2, rms3, rms4, rms5, rms6)
        for (i in 0 until 7) {
            val t = target[i]
            val c = currentBands[i]
            currentBands[i] = if (t > c) (c * 0.25f + t * 0.75f) else (c * 0.82f)
        }

        AudioWaveformHolder.record(positionUs / 1000, currentBands)
    }

    // Songs that run straight into the next aren't flushed between them, so the position rolls over
    // into the next song once it passes the current one's length.
    private fun advancePosition(frames: Int) {
        positionUs += frames * C.MICROS_PER_SECOND / inputAudioFormat.sampleRate
        if (periodIndex == C.INDEX_UNSET) return
        val durationUs = timeline.getPeriod(periodIndex, period).durationUs
        if (durationUs != C.TIME_UNSET && positionUs >= durationUs && periodIndex + 1 < timeline.periodCount) {
            positionUs -= durationUs
            periodIndex++
        }
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        positionUs = streamMetadata.positionOffsetUs
        timeline = streamMetadata.timeline
        periodIndex = streamMetadata.periodUid?.let { timeline.getIndexOfPeriod(it) } ?: C.INDEX_UNSET
        AudioWaveformHolder.clear()
    }

    override fun onReset() {
        lp0 = 0f
        lp1 = 0f
        lp2 = 0f
        lp3 = 0f
        lp4 = 0f
        lp5 = 0f
        currentBands.fill(0f)
        positionUs = 0L
        timeline = Timeline.EMPTY
        periodIndex = C.INDEX_UNSET
        AudioWaveformHolder.clear()
    }

    companion object {
        val instance = AudioWaveformProcessor()
    }
}
