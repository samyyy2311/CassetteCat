@file:Suppress("DEPRECATION")
package `in`.caffeinelabs.cassettecat.data.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EqualizerController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _isBassBoostSupported = MutableStateFlow(false)
    val isBassBoostSupported: StateFlow<Boolean> = _isBassBoostSupported.asStateFlow()

    private val _isVirtualizerSupported = MutableStateFlow(false)
    val isVirtualizerSupported: StateFlow<Boolean> = _isVirtualizerSupported.asStateFlow()

    val levelRangeMb: IntRange
        get() = equalizer?.bandLevelRange?.let { it[0].toInt()..it[1].toInt() } ?: 0..0

    // Device/DSP-dependent - commonly 5 or 6 on real hardware, not a fixed count.
    val numberOfBands: Int
        get() = equalizer?.numberOfBands?.toInt() ?: 0

    val presetNames: List<String>
        get() = equalizer?.let { eq -> (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) } } ?: emptyList()

    fun attach(audioSessionId: Int) {
        runCatching {
            release()

            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            _isAvailable.value = true

            bassBoost = runCatching {
                BassBoost(0, audioSessionId).apply {
                    _isBassBoostSupported.value = strengthSupported
                }
            }.getOrNull()

            virtualizer = runCatching {
                Virtualizer(0, audioSessionId).apply {
                    _isVirtualizerSupported.value = strengthSupported
                }
            }.getOrNull()
        }.onFailure {
            _isAvailable.value = false
            _isBassBoostSupported.value = false
            _isVirtualizerSupported.value = false
        }
    }

    fun centerFreqHz(band: Int): Int = (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000

    fun setBandLevel(band: Int, levelMb: Int) {
        equalizer?.let { eq -> runCatching { eq.setBandLevel(band.toShort(), levelMb.toShort()) } }
    }

    fun setMasterEnabled(enabled: Boolean) {
        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        }
    }

    fun setBassBoostStrength(strength: Int) {
        bassBoost?.let { bb ->
            runCatching {
                if (bb.strengthSupported) {
                    bb.enabled = strength > 0
                    bb.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        virtualizer?.let { virt ->
            runCatching {
                if (virt.strengthSupported) {
                    virt.enabled = strength > 0
                    virt.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    // A preset changes every band at once; the caller needs the resulting levels to persist them.
    fun applyPreset(index: Int): List<Int>? = equalizer?.let { eq ->
        runCatching {
            eq.usePreset(index.toShort())
            (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()).toInt() }
        }.getOrNull()
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        _isAvailable.value = false
        _isBassBoostSupported.value = false
        _isVirtualizerSupported.value = false
    }
}
