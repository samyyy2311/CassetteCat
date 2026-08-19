package `in`.caffeinelabs.cassettecat.data.library

import android.net.Uri

// Which repository a Song came from. Bare marker, no serverId: only one server per
// protocol is supported now, so the API client instance already knows which server it talks to.
sealed interface MusicSource {
    data object Local : MusicSource
    data object Subsonic : MusicSource
    data object Jellyfin : MusicSource
    // A track relayed live from a Listening Room host because the guest has no local or
    // server copy of it. contentUri points at the host's own ephemeral relay, not a
    // persistent server, so it isn't a real download/share/edit target.
    data object ListeningRoomHost : MusicSource
}

data class Song(
    // Source-prefixed ("local:<id>", "subsonic:<id>", "jellyfin:<id>") so ids stay
    // unique across sources once a library can mix local + streamed songs.
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val durationMs: Long,
    // Local: content:// URI. Streamed: pre-authenticated https:// stream URL (query-param
    // auth), ExoPlayer plays either the same way.
    val contentUri: Uri,
    val source: MusicSource,
    // Null for Local (uses the on-device AlbumArtLoader instead); populated for
    // streamed sources with a fully-authenticated cover-art URL.
    val artUri: Uri? = null,
    val isFavorite: Boolean = false,
    val genres: List<String> = emptyList(),
    // Release year from the file/server metadata. It is null when the source doesn't provide it.
    val releaseYear: Int? = null,
    // MediaStore provides this for local files; remote sources fall back to zero when their
    // server does not expose a reliable library-added timestamp.
    val dateAddedMs: Long = 0L,
    // Local only: on-device file path (MediaStore DATA column), used to look up sidecar
    // .lrc files. Always null for streamed sources.
    val filePath: String? = null
)
