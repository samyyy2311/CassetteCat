package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.equalizerDataStore by preferencesDataStore(name = "equalizer_settings")
private val BAND_LEVELS = stringPreferencesKey("band_levels_json")
private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class EqualizerLevels(val bandLevelsMb: List<Int> = emptyList())

class EqualizerSettingsRepository(private val context: Context) {
    val levels: Flow<EqualizerLevels> = context.equalizerDataStore.data.map { prefs -> prefs.decode() }

    suspend fun setBandLevels(levels: List<Int>) {
        context.equalizerDataStore.edit { prefs -> prefs[BAND_LEVELS] = json.encodeToString(EqualizerLevels(levels)) }
    }

    private fun Preferences.decode(): EqualizerLevels =
        this[BAND_LEVELS]?.let { runCatching { json.decodeFromString<EqualizerLevels>(it) }.getOrNull() } ?: EqualizerLevels()
}
