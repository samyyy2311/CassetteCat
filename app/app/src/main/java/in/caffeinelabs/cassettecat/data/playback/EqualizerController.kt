package `in`.caffeinelabs.cassettecat.data.playback

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EqualizerController {
    private var equalizer: Equalizer? = null
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    val levelRangeMb: IntRange
        get() = equalizer?.bandLevelRange?.let { it[0].toInt()..it[1].toInt() } ?: 0..0

    // Device/DSP-dependent — commonly 5 or 6 on real hardware, not a fixed count.
    val numberOfBands: Int
        get() = equalizer?.numberOfBands?.toInt() ?: 0

    val presetNames: List<String>
        get() = equalizer?.let { eq -> (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) } } ?: emptyList()

    fun attach(audioSessionId: Int) {
        runCatching {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            _isAvailable.value = true
        }.onFailure { _isAvailable.value = false }
    }

    fun centerFreqHz(band: Int): Int = (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000

    fun setBandLevel(band: Int, levelMb: Int) {
        equalizer?.let { eq -> runCatching { eq.setBandLevel(band.toShort(), levelMb.toShort()) } }
    }

    // A preset changes every band at once; the caller needs the resulting levels to persist them.
    fun applyPreset(index: Int): List<Int>? = equalizer?.let { eq ->
        runCatching {
            eq.usePreset(index.toShort())
            (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()).toInt() }
        }.getOrNull()
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        _isAvailable.value = false
    }
}
