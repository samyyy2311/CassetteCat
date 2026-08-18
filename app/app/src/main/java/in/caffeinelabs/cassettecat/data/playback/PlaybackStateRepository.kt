package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.playbackStateDataStore by preferencesDataStore(name = "playback_state")
private val PLAYBACK_STATE_KEY = stringPreferencesKey("playback_state_json")
private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class SavedPlaybackState(
    val queueSongIds: List<String>,
    val currentIndex: Int,
    val positionMs: Long
)

class PlaybackStateRepository(private val context: Context) {
    suspend fun save(state: SavedPlaybackState) {
        context.playbackStateDataStore.edit { prefs -> prefs[PLAYBACK_STATE_KEY] = json.encodeToString(state) }
    }

    suspend fun load(): SavedPlaybackState? =
        context.playbackStateDataStore.data.first()[PLAYBACK_STATE_KEY]
            ?.let { runCatching { json.decodeFromString<SavedPlaybackState>(it) }.getOrNull() }
}
