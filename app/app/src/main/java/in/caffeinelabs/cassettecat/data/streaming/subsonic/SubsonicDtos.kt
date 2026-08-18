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
    val album: SubsonicAlbum? = null
)

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
    // Present (an ISO8601 timestamp) when starred, absent otherwise; only presence
    // is used, the timestamp value itself isn't needed.
    val starred: String? = null,
    val genre: String? = null
)
