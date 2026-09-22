@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.data.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioWaveformHolder {
    private val _amplitudes = MutableStateFlow(FloatArray(7) { 0f })
    val amplitudes: StateFlow<FloatArray> = _amplitudes.asStateFlow()

    fun update(values: FloatArray) {
        _amplitudes.value = values
    }

    fun reset() {
        _amplitudes.value = FloatArray(7) { 0f }
    }
}

class AudioWaveformProcessor private constructor() : BaseAudioProcessor() {
    private val currentBands = FloatArray(7) { 0f }
    private var lastUpdateMs = 0L

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

        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            val duplicate = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            process16BitPcm(duplicate, inputAudioFormat.channelCount)
        }

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

        val now = SystemClock.elapsedRealtime()
        if (now - lastUpdateMs >= 24L) {
            lastUpdateMs = now
            AudioWaveformHolder.update(currentBands.clone())
        }
    }

    override fun onReset() {
        lp0 = 0f
        lp1 = 0f
        lp2 = 0f
        lp3 = 0f
        lp4 = 0f
        lp5 = 0f
        currentBands.fill(0f)
        AudioWaveformHolder.reset()
    }

    companion object {
        val instance = AudioWaveformProcessor()
    }
}
