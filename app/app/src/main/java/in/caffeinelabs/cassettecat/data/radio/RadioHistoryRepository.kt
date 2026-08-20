package `in`.caffeinelabs.cassettecat.data.radio

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val HISTORY_LIMIT = 10
private val Context.radioHistoryDataStore by preferencesDataStore(name = "radio_history")
private val RECENT_STATIONS_KEY = stringPreferencesKey("recent_stations_json")

class RadioHistoryRepository(private val context: Context) {
    val recentStations: Flow<List<RadioStation>> = context.radioHistoryDataStore.data.map { it.decode() }

    suspend fun recordPlayed(station: RadioStation) {
        context.radioHistoryDataStore.edit { prefs ->
            val withoutStation = prefs.decode().filterNot { it.uuid == station.uuid }
            prefs[RECENT_STATIONS_KEY] = sharedJson.encodeToString((listOf(station) + withoutStation).take(HISTORY_LIMIT))
        }
    }

    private fun Preferences.decode(): List<RadioStation> =
        this[RECENT_STATIONS_KEY]?.let { runCatching { sharedJson.decodeFromString<List<RadioStation>>(it) }.getOrNull() } ?: emptyList()
}
