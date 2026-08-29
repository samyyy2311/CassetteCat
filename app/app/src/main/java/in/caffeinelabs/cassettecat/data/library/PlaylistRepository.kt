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
import kotlinx.serialization.encodeToString
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson

private val Context.playlistDataStore by preferencesDataStore(name = "playlists")
private val PLAYLISTS_KEY = stringPreferencesKey("playlists_json")

enum class PlaylistCoverType { NONE, IMAGE, ICON, EMOJI }

@Serializable
enum class SmartRuleType(val label: String, val exclusivityGroup: Int? = null) {
    RECENTLY_ADDED("Recently Added (Last 30 Days)"),
    FAVORITES_ONLY("Favorited Tracks Only"),
    MIN_DURATION("Long Jams (> 5 min)", exclusivityGroup = 1),
    MAX_DURATION("Quick Bites (< 3 min)", exclusivityGroup = 1),
    DECADE_90S("90s Throwback", exclusivityGroup = 2),
    DECADE_2000S("2000s Hits", exclusivityGroup = 2),
    DECADE_2010S("2010s Hits", exclusivityGroup = 2),
    DECADE_2020S("2020s Contemporary", exclusivityGroup = 2)
}

@Serializable
data class SmartPlaylistCriteria(
    val rules: List<SmartRuleType> = emptyList(),
    val maxSongs: Int = 100
)

@Serializable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val songIds: List<String> = emptyList(),
    val coverType: PlaylistCoverType = PlaylistCoverType.NONE,
    // interpreted per coverType: app-private file path (IMAGE), a key into
    // PLAYLIST_ICON_OPTIONS (ICON), or the raw emoji character (EMOJI)
    val coverValue: String? = null,
    val isSmart: Boolean = false,
    val smartCriteria: SmartPlaylistCriteria? = null
)

class PlaylistRepository(private val context: Context) {
    private val coverStorage = PlaylistCoverStorage(context)

    val playlists: Flow<List<Playlist>> = context.playlistDataStore.data.map { prefs -> prefs.decode() }

    suspend fun create(
        name: String,
        songIds: List<String> = emptyList(),
        coverType: PlaylistCoverType = PlaylistCoverType.NONE,
        coverValue: String? = null
    ): Playlist {
        val playlist = Playlist(
            name = name,
            songIds = songIds.distinct(),
            coverType = coverType,
            coverValue = coverValue
        )
        update { it + playlist }
        return playlist
    }

    suspend fun createSmartPlaylist(name: String, criteria: SmartPlaylistCriteria): Playlist {
        val playlist = Playlist(
            name = name,
            isSmart = true,
            smartCriteria = criteria,
            coverType = PlaylistCoverType.ICON,
            coverValue = "sparkles"
        )
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
        context.playlistDataStore.edit { prefs -> prefs[PLAYLISTS_KEY] = sharedJson.encodeToString(transform(prefs.decode())) }
    }

    private fun Preferences.decode(): List<Playlist> =
        this[PLAYLISTS_KEY]?.let { runCatching { sharedJson.decodeFromString<List<Playlist>>(it) }.getOrNull() } ?: emptyList()
}

fun filterSongsForSmartCriteria(songs: List<Song>, favorites: Set<String>, criteria: SmartPlaylistCriteria): List<Song> {
    var filtered = songs
    for (rule in criteria.rules) {
        filtered = when (rule) {
            SmartRuleType.FAVORITES_ONLY -> filtered.filter { it.id in favorites }
            SmartRuleType.RECENTLY_ADDED -> {
                val thirtyDaysAgoMs = System.currentTimeMillis() - (30L * 86400_000L)
                filtered.filter { it.dateAddedMs >= thirtyDaysAgoMs }
            }
            SmartRuleType.MIN_DURATION -> filtered.filter { it.durationMs >= 300_000L }
            SmartRuleType.MAX_DURATION -> filtered.filter { it.durationMs in 1..180_000L }
            SmartRuleType.DECADE_90S -> filtered.filter { (it.releaseYear ?: 0) in 1990..1999 }
            SmartRuleType.DECADE_2000S -> filtered.filter { (it.releaseYear ?: 0) in 2000..2009 }
            SmartRuleType.DECADE_2010S -> filtered.filter { (it.releaseYear ?: 0) in 2010..2019 }
            SmartRuleType.DECADE_2020S -> filtered.filter { (it.releaseYear ?: 0) >= 2020 }
        }
    }
    return filtered.take(criteria.maxSongs)
}
