package `in`.caffeinelabs.cassettecat.data.streaming.subsonic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicEnvelope(@SerialName("subsonic-response") val response: SubsonicResponse)

@Serializable
data class SubsonicResponse(
    val status: String,
    val error: SubsonicError? = null,
    val albumList2: SubsonicAlbumList2? = null,
    val album: SubsonicAlbum? = null,
    val lyricsList: SubsonicLyricsList? = null,
    val playQueue: SubsonicPlayQueue? = null
)

@Serializable
data class SubsonicPlayQueue(
    val entry: List<SubsonicPlayQueueEntry> = emptyList(),
    val current: String? = null,
    val position: Long = 0L,
    val changedBy: String? = null
)

@Serializable
data class SubsonicPlayQueueEntry(val id: String)

// OpenSubsonic's getLyricsBySongId: a song can carry lyrics in several languages, timed or not.
@Serializable
data class SubsonicLyricsList(val structuredLyrics: List<SubsonicStructuredLyrics> = emptyList())

@Serializable
data class SubsonicStructuredLyrics(
    val synced: Boolean = false,
    val offset: Long = 0L,
    val line: List<SubsonicLyricLine> = emptyList()
)

@Serializable
data class SubsonicLyricLine(val start: Long? = null, val value: String = "")

@Serializable
data class SubsonicError(val code: Int, val message: String)

@Serializable
data class SubsonicAlbumList2(val album: List<SubsonicAlbumSummary> = emptyList())

@Serializable
data class SubsonicAlbumSummary(val id: String)

@Serializable
data class SubsonicAlbum(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val year: Int? = null,
    val song: List<SubsonicSong> = emptyList()
)

@Serializable
data class SubsonicSong(
    val id: String,
    val title: String,
    val artist: String? = null,
    val duration: Long = 0,
    val coverArt: String? = null,
    val starred: String? = null,
    val genre: String? = null
)
