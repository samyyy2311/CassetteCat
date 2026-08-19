@file:Suppress("DEPRECATION")
package `in`.caffeinelabs.cassettecat.data.playback

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EqualizerController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _isBassBoostSupported = MutableStateFlow(false)
    val isBassBoostSupported: StateFlow<Boolean> = _isBassBoostSupported.asStateFlow()

    private val _isVirtualizerSupported = MutableStateFlow(false)
    val isVirtualizerSupported: StateFlow<Boolean> = _isVirtualizerSupported.asStateFlow()

    private val _isLoudnessEnhancerSupported = MutableStateFlow(false)
    val isLoudnessEnhancerSupported: StateFlow<Boolean> = _isLoudnessEnhancerSupported.asStateFlow()

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

            val availableTypes = runCatching {
                AudioEffect.queryEffects()?.map { it.type }?.toSet()
            }.getOrNull() ?: emptySet()

            if (availableTypes.isEmpty() || availableTypes.contains(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
                equalizer = runCatching {
                    Equalizer(0, audioSessionId).apply { enabled = true }
                }.getOrNull()
            }
            _isAvailable.value = equalizer != null

            if (availableTypes.isEmpty() || availableTypes.contains(AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
                bassBoost = runCatching {
                    BassBoost(0, audioSessionId).apply {
                        _isBassBoostSupported.value = strengthSupported
                    }
                }.getOrNull()
            }

            if (availableTypes.isEmpty() || availableTypes.contains(AudioEffect.EFFECT_TYPE_VIRTUALIZER)) {
                virtualizer = runCatching {
                    Virtualizer(0, audioSessionId).apply {
                        _isVirtualizerSupported.value = strengthSupported
                    }
                }.getOrNull()
            }

            if (availableTypes.isEmpty() || availableTypes.contains(AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER)) {
                loudnessEnhancer = runCatching {
                    LoudnessEnhancer(audioSessionId).apply {
                        _isLoudnessEnhancerSupported.value = true
                    }
                }.getOrNull()
            }
        }.onFailure {
            _isAvailable.value = false
            _isBassBoostSupported.value = false
            _isVirtualizerSupported.value = false
            _isLoudnessEnhancerSupported.value = false
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
            loudnessEnhancer?.enabled = enabled
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

    fun setPreampGainMb(gainMb: Int) {
        loudnessEnhancer?.let { le ->
            runCatching {
                le.enabled = gainMb > 0
                le.setTargetGain(gainMb.coerceIn(0, 2000))
            }
        }
    }

    fun setLoudnessNormalization(enabled: Boolean, gainMb: Int = 0) {
        loudnessEnhancer?.let { le ->
            runCatching {
                le.enabled = enabled || gainMb > 0
                val target = if (gainMb > 0) gainMb else if (enabled) 600 else 0
                le.setTargetGain(target)
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
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        _isAvailable.value = false
        _isBassBoostSupported.value = false
        _isVirtualizerSupported.value = false
        _isLoudnessEnhancerSupported.value = false
    }
}
