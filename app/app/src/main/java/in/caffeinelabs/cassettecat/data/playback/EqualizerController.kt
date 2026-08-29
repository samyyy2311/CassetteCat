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
    private var sessionId: Int = 0
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
        get() = equalizer?.bandLevelRange?.let {
            val min = it[0].toInt()
            val max = it[1].toInt()
            if (min < max) min..max else -1500..1500
        } ?: -1500..1500

    // Device/DSP-dependent - commonly 5 or 6 on real hardware, not a fixed count.
    val numberOfBands: Int
        get() = equalizer?.numberOfBands?.toInt() ?: 0

    private val KNOWN_PRESETS = listOf(
        "Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal",
        "Hip Hop", "Jazz", "Pop", "Rock", "Acoustic", "Bass Boost",
        "Treble Boost", "Vocal", "Electronic", "Latin", "R&B", "Lounge",
        "Piano", "Spoken Word"
    )

    private var _presetNames: List<String> = emptyList()
    val presetNames: List<String> get() = _presetNames

    private fun computePresetNames(eq: Equalizer): List<String> =
            (0 until eq.numberOfPresets).map { index ->
                val raw = runCatching { eq.getPresetName(index.toShort()) }.getOrNull() ?: ""
                val nullIdx = raw.indexOf('\u0000')
                val clean = (if (nullIdx >= 0) raw.substring(0, nullIdx) else raw).trim()
                val matched = KNOWN_PRESETS.firstOrNull { preset -> clean.startsWith(preset, ignoreCase = true) }
                (matched ?: clean).ifBlank { "Preset ${index + 1}" }
            }

    fun attach(audioSessionId: Int, bassBoostActive: Boolean, virtualizerActive: Boolean, loudnessActive: Boolean) {
        runCatching {
            release()
            sessionId = audioSessionId

            val availableTypes = runCatching {
                AudioEffect.queryEffects()?.map { it.type }?.toSet()
            }.getOrNull() ?: emptySet()

            if (availableTypes.isEmpty() || availableTypes.contains(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
                equalizer = runCatching {
                    Equalizer(0, audioSessionId).apply { enabled = true }
                }.getOrNull()
            }
            _isAvailable.value = equalizer != null
            _presetNames = equalizer?.let { computePresetNames(it) } ?: emptyList()

            if (availableTypes.isEmpty() || availableTypes.contains(AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
                val bb = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
                _isBassBoostSupported.value = bb?.strengthSupported ?: false
                bassBoost = if (bassBoostActive) bb else { runCatching { bb?.release() }; null }
            }

            if (availableTypes.isEmpty() || availableTypes.contains(AudioEffect.EFFECT_TYPE_VIRTUALIZER)) {
                val virt = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
                _isVirtualizerSupported.value = virt?.strengthSupported ?: false
                virtualizer = if (virtualizerActive) virt else { runCatching { virt?.release() }; null }
            }

            if (availableTypes.isEmpty() || availableTypes.contains(AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER)) {
                val le = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
                _isLoudnessEnhancerSupported.value = le != null
                loudnessEnhancer = if (loudnessActive) le else { runCatching { le?.release() }; null }
            }
        }.onFailure {
            _isAvailable.value = false
            _isBassBoostSupported.value = false
            _isVirtualizerSupported.value = false
            _isLoudnessEnhancerSupported.value = false
            _presetNames = emptyList()
        }
    }

    fun centerFreqHz(band: Int): Int = (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000

    fun setBandLevel(band: Int, levelMb: Int) {
        equalizer?.let { eq -> runCatching { eq.setBandLevel(band.toShort(), levelMb.toShort()) } }
    }

    private var masterEnabled = true

    fun setMasterEnabled(enabled: Boolean) {
        masterEnabled = enabled
        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            loudnessEnhancer?.enabled = enabled
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val bb = bassBoost ?: if (strength > 0 && _isBassBoostSupported.value) {
            runCatching { BassBoost(0, sessionId) }.getOrNull()?.also { bassBoost = it }
        } else null
        bb?.let {
            runCatching {
                it.enabled = masterEnabled && strength > 0
                it.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val virt = virtualizer ?: if (strength > 0 && _isVirtualizerSupported.value) {
            runCatching { Virtualizer(0, sessionId) }.getOrNull()?.also { virtualizer = it }
        } else null
        virt?.let {
            runCatching {
                it.enabled = masterEnabled && strength > 0
                it.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }
    }

    fun setPreampGainMb(gainMb: Int) {
        val le = loudnessEnhancer ?: if (gainMb > 0) {
            runCatching { LoudnessEnhancer(sessionId) }.getOrNull()?.also { loudnessEnhancer = it }
        } else null
        le?.let {
            runCatching {
                it.enabled = masterEnabled && gainMb > 0
                it.setTargetGain(gainMb.coerceIn(0, 2000))
            }
        }
    }

    fun setLoudnessNormalization(enabled: Boolean, gainMb: Int = 0) {
        val le = loudnessEnhancer ?: if (enabled || gainMb > 0) {
            runCatching { LoudnessEnhancer(sessionId) }.getOrNull()?.also { loudnessEnhancer = it }
        } else null
        le?.let {
            runCatching {
                it.enabled = masterEnabled && (enabled || gainMb > 0)
                val target = if (gainMb > 0) gainMb else if (enabled) 600 else 0
                it.setTargetGain(target)
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
        masterEnabled = true
        _isAvailable.value = false
        _isBassBoostSupported.value = false
        _isVirtualizerSupported.value = false
        _isLoudnessEnhancerSupported.value = false
        _presetNames = emptyList()
    }
}
