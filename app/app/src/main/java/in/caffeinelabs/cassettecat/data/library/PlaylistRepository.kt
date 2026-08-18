package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.playlistDataStore by preferencesDataStore(name = "playlists")
private val PLAYLISTS_KEY = stringPreferencesKey("playlists_json")
private val json = Json { ignoreUnknownKeys = true }

enum class PlaylistCoverType { NONE, IMAGE, ICON, EMOJI }

@Serializable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val songIds: List<String> = emptyList(),
    val coverType: PlaylistCoverType = PlaylistCoverType.NONE,
    // interpreted per coverType: app-private file path (IMAGE), a key into
    // PLAYLIST_ICON_OPTIONS (ICON), or the raw emoji character (EMOJI)
    val coverValue: String? = null
)

class PlaylistRepository(private val context: Context) {
    private val coverStorage = PlaylistCoverStorage(context)

    val playlists: Flow<List<Playlist>> = context.playlistDataStore.data.map { prefs -> prefs.decode() }

    suspend fun create(name: String): Playlist {
        val playlist = Playlist(name = name)
        update { it + playlist }
        return playlist
    }

    suspend fun rename(id: String, name: String) = update { list ->
        list.map { if (it.id == id) it.copy(name = name) else it }
    }

    suspend fun delete(id: String) {
        val target = playlists.first().find { it.id == id }
        if (target?.coverType == PlaylistCoverType.IMAGE) target.coverValue?.let { coverStorage.delete(it) }
        update { list -> list.filterNot { it.id == id } }
    }

    suspend fun setCover(id: String, type: PlaylistCoverType, value: String?) = update { list ->
        list.map { if (it.id == id) it.copy(coverType = type, coverValue = value) else it }
    }

    suspend fun clearCover(id: String) = setCover(id, PlaylistCoverType.NONE, null)

    // backup restore only: full replace, not a merge
    suspend fun replaceAll(playlists: List<Playlist>) = update { playlists }

    suspend fun addSong(playlistId: String, songId: String) = update { list ->
        list.map { if (it.id == playlistId && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it }
    }

    suspend fun addSongs(playlistId: String, songIds: List<String>) = update { list ->
        list.map { if (it.id == playlistId) it.copy(songIds = it.songIds + songIds.filterNot { id -> id in it.songIds }) else it }
    }

    suspend fun removeSong(playlistId: String, songId: String) = update { list ->
        list.map { if (it.id == playlistId) it.copy(songIds = it.songIds - songId) else it }
    }

    private suspend fun update(transform: (List<Playlist>) -> List<Playlist>) {
        context.playlistDataStore.edit { prefs -> prefs[PLAYLISTS_KEY] = json.encodeToString(transform(prefs.decode())) }
    }

    private fun Preferences.decode(): List<Playlist> =
        this[PLAYLISTS_KEY]?.let { runCatching { json.decodeFromString<List<Playlist>>(it) }.getOrNull() } ?: emptyList()
}
