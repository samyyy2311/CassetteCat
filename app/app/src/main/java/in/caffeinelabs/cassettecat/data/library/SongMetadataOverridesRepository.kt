package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson

@Serializable
data class SongMetadataOverride(
    val songId: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val releaseYear: Int? = null
)

class SongMetadataOverridesRepository private constructor(context: Context) {
    private val file = File(context.filesDir, "song_metadata_overrides.json")
    private val writeMutex = Mutex()
    private val _overrides = MutableStateFlow<Map<String, SongMetadataOverride>>(emptyMap())
    val overrides: StateFlow<Map<String, SongMetadataOverride>> = _overrides.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!file.exists()) return
        runCatching {
            val list = sharedJson.decodeFromString<List<SongMetadataOverride>>(file.readText())
            _overrides.value = list.associateBy { it.songId }
        }
    }

    suspend fun saveOverride(override: SongMetadataOverride) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val updated = _overrides.value.toMutableMap()
            updated[override.songId] = override
            _overrides.value = updated
            runCatching {
                val tempFile = File(file.parentFile, "${file.name}.tmp")
                tempFile.writeText(sharedJson.encodeToString(updated.values.toList()))
                if (!tempFile.renameTo(file)) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
            }
        }
    }

    fun applyTo(song: Song): Song {
        val ov = _overrides.value[song.id] ?: return song
        return song.copy(
            title = ov.title?.ifBlank { null } ?: song.title,
            artist = ov.artist?.ifBlank { null } ?: song.artist,
            album = ov.album?.ifBlank { null } ?: song.album,
            releaseYear = ov.releaseYear ?: song.releaseYear
        )
    }

    fun applyTo(songs: List<Song>): List<Song> {
        if (_overrides.value.isEmpty()) return songs
        return songs.map { applyTo(it) }
    }

    companion object {
        @Volatile private var instance: SongMetadataOverridesRepository? = null

        fun getInstance(context: Context): SongMetadataOverridesRepository =
            instance ?: synchronized(this) {
                instance ?: SongMetadataOverridesRepository(context.applicationContext).also { instance = it }
            }
    }
}
