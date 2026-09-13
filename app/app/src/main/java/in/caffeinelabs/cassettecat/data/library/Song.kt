package `in`.caffeinelabs.cassettecat.data.library

import android.net.Uri

// One server per protocol is supported, so a server id is not needed here.
sealed interface MusicSource {
    data object Local : MusicSource
    data object Subsonic : MusicSource
    data object Jellyfin : MusicSource
    data object ListeningRoomHost : MusicSource
    data object Radio : MusicSource
}

data class Song(
    // Prefixing keeps ids unique when local and remote libraries are mixed.
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val durationMs: Long,
    // Local songs use content:// URIs. Remote songs use stream URLs.
    val contentUri: Uri,
    val source: MusicSource,
    // Remote sources can provide cover art. Local art is loaded on-device.
    val artUri: Uri? = null,
    val isFavorite: Boolean = false,
    val genres: List<String> = emptyList(),
    val releaseYear: Int? = null,
    // Remote sources fall back to zero when there is no reliable added date.
    val dateAddedMs: Long = 0L,
    // Local path used for sidecar .lrc files. Remote songs leave this null.
    val filePath: String? = null,
    // Radio metadata. Other sources keep the defaults.
    val bitrateKbps: Int = 0,
    val country: String = ""
)
