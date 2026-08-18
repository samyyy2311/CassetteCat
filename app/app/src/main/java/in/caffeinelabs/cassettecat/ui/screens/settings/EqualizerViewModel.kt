package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.playback.AutoEqProfile
import `in`.caffeinelabs.cassettecat.data.playback.AutoEqProfiles
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerController
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerLevels
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EqualizerViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = EqualizerSettingsRepository(app)

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

    fun setBandLevel(band: Int, levelMb: Int) {
        EqualizerController.setBandLevel(band, levelMb)
        val padded = paddedLevels()
        val updated = padded.toMutableList().apply { this[band] = levelMb }
        viewModelScope.launch { repository.setBandLevels(updated, presetIndex = -1) }
    }

    fun setBassBoostStrength(strength: Int) {
        EqualizerController.setBassBoostStrength(strength)
        viewModelScope.launch { repository.setBassBoostStrength(strength) }
    }

    fun setVirtualizerStrength(strength: Int) {
        EqualizerController.setVirtualizerStrength(strength)
        viewModelScope.launch { repository.setVirtualizerStrength(strength) }
    }

    fun applyPreset(index: Int) {
        val newLevels = EqualizerController.applyPreset(index) ?: return
        viewModelScope.launch { repository.setBandLevels(newLevels, presetIndex = index) }
    }

    fun applyAutoEq(profile: AutoEqProfile) {
        val freqs = List(numberOfBands) { centerFreqHz(it) }
        val gains = AutoEqProfiles.calculateBandGains(profile, freqs, levelRangeMb)
        gains.forEachIndexed { band, level -> EqualizerController.setBandLevel(band, level) }
        viewModelScope.launch { repository.setBandLevels(gains, presetIndex = -1) }
    }

    fun reset() {
        val flat = List(numberOfBands) { 0 }
        flat.forEachIndexed { band, level -> EqualizerController.setBandLevel(band, level) }
        EqualizerController.setBassBoostStrength(0)
        EqualizerController.setVirtualizerStrength(0)
        viewModelScope.launch {
            repository.saveSettings(
                EqualizerLevels(
                    bandLevelsMb = flat,
                    enabled = true,
                    bassBoostStrength = 0,
                    virtualizerStrength = 0,
                    selectedPresetIndex = -1
                )
            )
        }
    }

    private fun paddedLevels(): List<Int> {
        val current = levels.value.bandLevelsMb
        return if (current.size < numberOfBands) List(numberOfBands) { current.getOrElse(it) { 0 } } else current
    }
}
