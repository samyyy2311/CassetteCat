package `in`.caffeinelabs.cassettecat.data.library

import android.net.Uri

sealed interface MusicSource {
    data object Local : MusicSource
    data object Subsonic : MusicSource
    data object Jellyfin : MusicSource
    data object ListeningRoomHost : MusicSource
    data object Radio : MusicSource
    // Playing on the paired computer: shown and controlled from the phone, never played on it.
    data object Desktop : MusicSource
    // A song from the paired computer's library, streamed to and played on this phone.
    data object Computer : MusicSource
}

/** Shown for music whose file lives on another device, so this phone's library actions do not apply to it. */
val Song.isFromAnotherDevice: Boolean
    get() = source == MusicSource.ListeningRoomHost || source == MusicSource.Desktop || source == MusicSource.Computer

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

/** What makes two songs the same song, here and on the computer: title and artist, ignoring case. */
fun songMatchKey(title: String, artist: String): String = title.trim().lowercase() + "\u001f" + artist.trim().lowercase()
