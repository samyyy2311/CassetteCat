package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson

private val Context.equalizerDataStore by preferencesDataStore(name = "equalizer_settings")
private val EQUALIZER_STATE = stringPreferencesKey("equalizer_state_json")

@Serializable
data class EqualizerLevels(
    val bandLevelsMb: List<Int> = emptyList(),
    val enabled: Boolean = true,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val selectedPresetIndex: Int = -1,
    val preampGainMb: Int = 0,
    val loudnessNormalization: Boolean = false
)

class EqualizerSettingsRepository(private val context: Context) {
    val levels: Flow<EqualizerLevels> = context.equalizerDataStore.data.map { prefs -> prefs.decode() }

    suspend fun saveSettings(settings: EqualizerLevels) {
        context.equalizerDataStore.edit { prefs -> prefs[EQUALIZER_STATE] = sharedJson.encodeToString(settings) }
    }

    suspend fun setBandLevels(levels: List<Int>, presetIndex: Int = -1) {
        context.equalizerDataStore.edit { prefs ->
            val current = prefs.decode()
            val updated = current.copy(bandLevelsMb = levels, selectedPresetIndex = presetIndex)
            prefs[EQUALIZER_STATE] = sharedJson.encodeToString(updated)
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.equalizerDataStore.edit { prefs ->
            val current = prefs.decode()
            val updated = current.copy(enabled = enabled)
            prefs[EQUALIZER_STATE] = sharedJson.encodeToString(updated)
        }
    }

    suspend fun setBassBoostStrength(strength: Int) {
        context.equalizerDataStore.edit { prefs ->
            val current = prefs.decode()
            val updated = current.copy(bassBoostStrength = strength)
            prefs[EQUALIZER_STATE] = sharedJson.encodeToString(updated)
        }
    }

    suspend fun setVirtualizerStrength(strength: Int) {
        context.equalizerDataStore.edit { prefs ->
            val current = prefs.decode()
            val updated = current.copy(virtualizerStrength = strength)
            prefs[EQUALIZER_STATE] = sharedJson.encodeToString(updated)
        }
    }

    suspend fun setPreampGainMb(gainMb: Int) {
        context.equalizerDataStore.edit { prefs ->
            val current = prefs.decode()
            val updated = current.copy(preampGainMb = gainMb)
            prefs[EQUALIZER_STATE] = sharedJson.encodeToString(updated)
        }
    }

    suspend fun setLoudnessNormalization(enabled: Boolean) {
        context.equalizerDataStore.edit { prefs ->
            val current = prefs.decode()
            val updated = current.copy(loudnessNormalization = enabled)
            prefs[EQUALIZER_STATE] = sharedJson.encodeToString(updated)
        }
    }

    private fun Preferences.decode(): EqualizerLevels =
        this[EQUALIZER_STATE]?.let { runCatching { sharedJson.decodeFromString<EqualizerLevels>(it) }.getOrNull() } ?: EqualizerLevels()
}
