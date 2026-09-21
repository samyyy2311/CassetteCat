package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.albumCoverDataStore by preferencesDataStore(name = "album_covers")
private val ALBUM_COVERS = stringPreferencesKey("album_covers_json")

class AlbumCoverRepository private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val albumCovers: StateFlow<Map<String, String>> = context.albumCoverDataStore.data
        .map { prefs ->
            prefs[ALBUM_COVERS]?.let {
                runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull()
            }.orEmpty()
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun getCoverPath(album: String, artist: String, albumId: String = ""): String? {
        val current = albumCovers.value
        val key = albumKey(album, artist)
        return current[key]
            ?: (if (albumId.isNotBlank()) current[albumId] else null)
            ?: (if (album.isNotBlank()) current[albumOnlyKey(album)] else null)
    }

    suspend fun setCover(album: String, artist: String, albumId: String = "", coverPath: String) {
        context.albumCoverDataStore.edit { prefs ->
            val current = prefs[ALBUM_COVERS]?.let {
                runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull()
            }.orEmpty()
            val updated = current.toMutableMap()
            val key = albumKey(album, artist)
            updated[key] = coverPath
            if (albumId.isNotBlank()) {
                updated[albumId] = coverPath
            }
            if (album.isNotBlank()) {
                updated[albumOnlyKey(album)] = coverPath
            }
            prefs[ALBUM_COVERS] = sharedJson.encodeToString(updated)
        }
    }

    suspend fun clearCover(album: String, artist: String, albumId: String = "") {
        context.albumCoverDataStore.edit { prefs ->
            val current = prefs[ALBUM_COVERS]?.let {
                runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull()
            }.orEmpty()
            val updated = current.toMutableMap()
            val key = albumKey(album, artist)
            updated.remove(key)
            if (albumId.isNotBlank()) {
                updated.remove(albumId)
            }
            if (album.isNotBlank()) {
                updated.remove(albumOnlyKey(album))
            }
            prefs[ALBUM_COVERS] = sharedJson.encodeToString(updated)
        }
    }

    suspend fun clearAllCovers() {
        context.albumCoverDataStore.edit { prefs ->
            prefs.remove(ALBUM_COVERS)
        }
    }

    companion object {
        fun albumKey(album: String, artist: String): String =
            "${album.trim().lowercase()}|${artist.trim().lowercase()}"

        fun albumOnlyKey(album: String): String =
            "album:${album.trim().lowercase()}"

        @Volatile private var instance: AlbumCoverRepository? = null

        fun getInstance(context: Context): AlbumCoverRepository =
            instance ?: synchronized(this) {
                instance ?: AlbumCoverRepository(context.applicationContext).also { instance = it }
            }
    }
}
