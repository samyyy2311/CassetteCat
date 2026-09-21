package `in`.caffeinelabs.cassettecat.data.library

import android.net.Uri

// Bare marker: supported protocols talk to a single server instance.
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
    // content:// URI for local tracks; pre-authenticated https:// stream URL for remote tracks.
    val contentUri: Uri,
    val source: MusicSource,
    // Null for Local (resolved via AlbumArtLoader); authenticated URL for remote tracks.
    val artUri: Uri? = null,
    val isFavorite: Boolean = false,
    val genres: List<String> = emptyList(),
    val releaseYear: Int? = null,
    // Added timestamp from MediaStore; zero when remote servers do not expose one.
    val dateAddedMs: Long = 0L,
    // Local file path for sidecar .lrc lookup; null for remote tracks.
    val filePath: String? = null,
    // Radio metadata: stream bitrate in kbps and station country.
    val bitrateKbps: Int = 0,
    val country: String = ""
)
