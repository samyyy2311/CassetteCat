package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val levelRangeMb: IntRange get() = EqualizerController.levelRangeMb
    val numberOfBands: Int get() = EqualizerController.numberOfBands
    val presetNames: List<String> get() = EqualizerController.presetNames

    fun centerFreqHz(band: Int): Int = EqualizerController.centerFreqHz(band)

    fun setBandLevel(band: Int, levelMb: Int) {
        EqualizerController.setBandLevel(band, levelMb)
        val padded = paddedLevels()
        val updated = padded.toMutableList().apply { this[band] = levelMb }
        viewModelScope.launch { repository.setBandLevels(updated) }
    }

    fun applyPreset(index: Int) {
        val newLevels = EqualizerController.applyPreset(index) ?: return
        viewModelScope.launch { repository.setBandLevels(newLevels) }
    }

    fun reset() {
        val flat = List(numberOfBands) { 0 }
        flat.forEachIndexed { band, level -> EqualizerController.setBandLevel(band, level) }
        viewModelScope.launch { repository.setBandLevels(flat) }
    }

    private fun paddedLevels(): List<Int> {
        val current = levels.value.bandLevelsMb
        return if (current.size < numberOfBands) List(numberOfBands) { current.getOrElse(it) { 0 } } else current
    }
}
