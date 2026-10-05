package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")
private val FAVORITE_IDS = stringSetPreferencesKey("favorite_song_ids")

// The liked set for every library source; a streaming server's likes are mirrored in on each library load.
class FavoritesRepository(private val context: Context) {
    val favoriteIds: Flow<Set<String>> = context.favoritesDataStore.data.map { it[FAVORITE_IDS] ?: emptySet() }

    suspend fun setFavorite(songId: String, favorite: Boolean) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[FAVORITE_IDS] ?: emptySet()
            prefs[FAVORITE_IDS] = if (favorite) current + songId else current - songId
        }
    }

    suspend fun mirror(sourceSongIds: Set<String>, likedIds: Set<String>) {
        context.favoritesDataStore.edit { prefs ->
            prefs[FAVORITE_IDS] = (prefs[FAVORITE_IDS] ?: emptySet()) - sourceSongIds + likedIds
        }
    }

    suspend fun replaceAll(songIds: Set<String>) {
        context.favoritesDataStore.edit { prefs -> prefs[FAVORITE_IDS] = songIds }
    }
}
