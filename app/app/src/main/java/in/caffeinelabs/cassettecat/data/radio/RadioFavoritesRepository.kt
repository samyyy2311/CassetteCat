package `in`.caffeinelabs.cassettecat.data.radio

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.radioFavoritesDataStore by preferencesDataStore(name = "radio_favorites")
private val FAVORITE_STATIONS_KEY = stringPreferencesKey("favorite_stations_json")

class RadioFavoritesRepository(private val context: Context) {
    val favoriteStations: Flow<List<RadioStation>> = context.radioFavoritesDataStore.data.map { it.decode() }

    suspend fun add(station: RadioStation) {
        update { list -> if (list.any { it.uuid == station.uuid }) list else list + station }
    }

    suspend fun remove(uuid: String) {
        update { list -> list.filterNot { it.uuid == uuid } }
    }

    private suspend fun update(transform: (List<RadioStation>) -> List<RadioStation>) {
        context.radioFavoritesDataStore.edit { prefs ->
            prefs[FAVORITE_STATIONS_KEY] = sharedJson.encodeToString(transform(prefs.decode()))
        }
    }

    private fun Preferences.decode(): List<RadioStation> =
        this[FAVORITE_STATIONS_KEY]?.let { runCatching { sharedJson.decodeFromString<List<RadioStation>>(it) }.getOrNull() } ?: emptyList()
}
