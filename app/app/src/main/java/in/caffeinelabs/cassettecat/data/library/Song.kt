package `in`.caffeinelabs.cassettecat.data.library

import android.net.Uri

sealed interface MusicSource {
    data object Local : MusicSource
    data object Subsonic : MusicSource
    data object Jellyfin : MusicSource
    data object ListeningRoomHost : MusicSource
    data object Radio : MusicSource
}

data class Song(
    // Source-prefixed to stay globally unique across local and remote libraries.
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val durationMs: Long,
    val contentUri: Uri,
    val source: MusicSource,
    val artUri: Uri? = null,
    val isFavorite: Boolean = false,
    val genres: List<String> = emptyList(),
    val releaseYear: Int? = null,
    val dateAddedMs: Long = 0L,
    val filePath: String? = null,
    val bitrateKbps: Int = 0,
    val country: String = ""
)
