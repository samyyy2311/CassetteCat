package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.playback.AutoEqProfile
import `in`.caffeinelabs.cassettecat.data.playback.AutoEqProfiles
import `in`.caffeinelabs.cassettecat.data.playback.CustomEqualizerPreset
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerController
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerLevels
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EqualizerViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = EqualizerSettingsRepository(app)
    private val audioDspScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val levels: StateFlow<EqualizerLevels> =
        repository.levels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerLevels())

    val isAvailable: StateFlow<Boolean> = EqualizerController.isAvailable
    val isBassBoostSupported: StateFlow<Boolean> = EqualizerController.isBassBoostSupported
    val isVirtualizerSupported: StateFlow<Boolean> = EqualizerController.isVirtualizerSupported

    val levelRangeMb: IntRange get() = EqualizerController.levelRangeMb
    val numberOfBands: Int get() = EqualizerController.numberOfBands
    val presetNames: List<String> get() = EqualizerController.presetNames

    fun centerFreqHz(band: Int): Int = EqualizerController.centerFreqHz(band)

    fun setMasterEnabled(enabled: Boolean) {
        EqualizerController.setMasterEnabled(enabled)
        viewModelScope.launch { repository.setEnabled(enabled) }
    }

    fun setBandLevelLive(band: Int, levelMb: Int) {
        audioDspScope.launch { EqualizerController.setBandLevel(band, levelMb) }
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        EqualizerController.setBandLevel(band, levelMb)
        val padded = paddedLevels()
        val updated = padded.toMutableList().apply { this[band] = levelMb }
        viewModelScope.launch { repository.setBandLevels(updated, presetIndex = -1) }
    }

    fun setBassBoostStrengthLive(strength: Int) {
        audioDspScope.launch { EqualizerController.setBassBoostStrength(strength) }
    }

    fun setBassBoostStrength(strength: Int) {
        EqualizerController.setBassBoostStrength(strength)
        viewModelScope.launch { repository.setBassBoostStrength(strength) }
    }

    fun setVirtualizerStrengthLive(strength: Int) {
        audioDspScope.launch { EqualizerController.setVirtualizerStrength(strength) }
    }

    fun setVirtualizerStrength(strength: Int) {
        EqualizerController.setVirtualizerStrength(strength)
        viewModelScope.launch { repository.setVirtualizerStrength(strength) }
    }

    fun setPreampGainMbLive(gainMb: Int) {
        audioDspScope.launch { EqualizerController.setPreampGainMb(gainMb) }
    }

    fun applyPreset(index: Int) {
        val newLevels = EqualizerController.applyPreset(index) ?: return
        viewModelScope.launch { repository.setBandLevels(newLevels, presetIndex = index, customPresetId = null) }
    }

    fun applyCustomPreset(preset: CustomEqualizerPreset) {
        for (band in 0 until numberOfBands) {
            EqualizerController.setBandLevel(band, preset.bandLevelsMb.getOrElse(band) { 0 })
        }
        EqualizerController.setBassBoostStrength(preset.bassBoostStrength)
        EqualizerController.setVirtualizerStrength(preset.virtualizerStrength)
        EqualizerController.setPreampGainMb(preset.preampGainMb)
        viewModelScope.launch {
            repository.setBandLevels(preset.bandLevelsMb, presetIndex = -1, customPresetId = preset.id)
            repository.setBassBoostStrength(preset.bassBoostStrength)
            repository.setVirtualizerStrength(preset.virtualizerStrength)
            repository.setPreampGainMb(preset.preampGainMb)
        }
    }

    fun saveCurrentAsPreset(name: String) {
        val current = levels.value
        viewModelScope.launch {
            repository.saveCustomPreset(
                name = name,
                bandLevelsMb = paddedLevels(),
                bassBoost = current.bassBoostStrength,
                virtualizer = current.virtualizerStrength,
                preamp = current.preampGainMb
            )
        }
    }

    fun deleteCustomPreset(id: String) {
        viewModelScope.launch {
            repository.deleteCustomPreset(id)
        }
    }

    fun applyAutoEq(profile: AutoEqProfile) {
        val freqs = List(numberOfBands) { centerFreqHz(it) }
        val gains = AutoEqProfiles.calculateBandGains(profile, freqs, levelRangeMb)
        gains.forEachIndexed { band, level -> EqualizerController.setBandLevel(band, level) }
        viewModelScope.launch { repository.setBandLevels(gains, presetIndex = -1, customPresetId = null) }
    }

    fun setPreampGainMb(gainMb: Int) {
        EqualizerController.setPreampGainMb(gainMb)
        viewModelScope.launch { repository.setPreampGainMb(gainMb) }
    }

    fun setLoudnessNormalization(enabled: Boolean) {
        EqualizerController.setLoudnessNormalization(enabled, levels.value.preampGainMb)
        viewModelScope.launch { repository.setLoudnessNormalization(enabled) }
    }

    fun reset() {
        val flat = List(numberOfBands) { 0 }
        flat.forEachIndexed { band, level -> EqualizerController.setBandLevel(band, level) }
        EqualizerController.setBassBoostStrength(0)
        EqualizerController.setVirtualizerStrength(0)
        EqualizerController.setPreampGainMb(0)
        EqualizerController.setLoudnessNormalization(false, 0)
        viewModelScope.launch {
            repository.saveSettings(
                EqualizerLevels(
                    bandLevelsMb = flat,
                    enabled = levels.value.enabled,
                    bassBoostStrength = 0,
                    virtualizerStrength = 0,
                    selectedPresetIndex = -1,
                    selectedCustomPresetId = null,
                    preampGainMb = 0,
                    loudnessNormalization = false,
                    customPresets = levels.value.customPresets
                )
            )
        }
    }

    private fun paddedLevels(): List<Int> {
        val current = levels.value.bandLevelsMb
        return if (current.size < numberOfBands) List(numberOfBands) { current.getOrElse(it) { 0 } } else current
    }
}
