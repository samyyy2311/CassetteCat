package `in`.caffeinelabs.cassettecat.data.library

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.io.IOException
import java.net.URLEncoder
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Request

// A recording this much longer or shorter than the file is another version, such as a live take or a radio edit.
private const val LENGTH_TOLERANCE_MS = 3_000L
private const val MINIMUM_SCORE = 90

/** A song's details on MusicBrainz; [album] and [year] are null when the release would be a guess. */
data class MusicBrainzTags(val title: String, val artist: String, val album: String? = null, val year: Int? = null)

@Serializable
private data class RecordingSearch(val recordings: List<Recording> = emptyList())

@Serializable
private data class Recording(
    val score: Int = 0,
    val title: String = "",
    val length: Long? = null,
    @SerialName("artist-credit") val artistCredit: List<ArtistCredit> = emptyList(),
    val releases: List<Release> = emptyList()
)

@Serializable
private data class ArtistCredit(val name: String = "", val joinphrase: String = "")

@Serializable
private data class Release(
    val title: String = "",
    val status: String? = null,
    val date: String? = null,
    @SerialName("release-group") val releaseGroup: ReleaseGroup? = null
)

@Serializable
private data class ReleaseGroup(
    @SerialName("primary-type") val primaryType: String? = null,
    @SerialName("secondary-types") val secondaryTypes: List<String> = emptyList()
)

private val Release.isStudioAlbum: Boolean
    get() = status == "Official" && releaseGroup?.primaryType == "Album" && releaseGroup.secondaryTypes.isEmpty()

/**
 * Picks the best confident match from a MusicBrainz recording search. MusicBrainz often has the same song as several
 * recordings, so every one that fits is searched for its album: the one the file already names, or else the earliest
 * studio album, where the song first came out.
 */
internal fun musicBrainzTagsFrom(json: String, durationMs: Long, album: String): MusicBrainzTags? {
    val fitting = sharedJson.decodeFromString<RecordingSearch>(json).recordings.filter { recording ->
        val length = recording.length ?: 0L
        recording.score >= MINIMUM_SCORE &&
            (durationMs <= 0 || length <= 0 || abs(length - durationMs) <= LENGTH_TOLERANCE_MS)
    }
    if (fitting.isEmpty()) return null
    val candidates = fitting.flatMap { recording -> recording.releases.map { recording to it } }
    val (recording, release) = candidates.firstOrNull { (_, release) ->
        album.isNotBlank() && release.title.equals(album.trim(), ignoreCase = true)
    } ?: candidates.filter { (_, release) -> release.isStudioAlbum }
        .minByOrNull { (_, release) -> release.date?.ifBlank { null } ?: "9999" }
        ?: (fitting.first() to null)
    return MusicBrainzTags(
        title = recording.title,
        artist = recording.artistCredit.joinToString("") { it.name + it.joinphrase },
        // Without the file's album or a studio album, the release would be a guess, such as one of many compilations.
        album = release?.title,
        year = release?.date?.take(4)?.toIntOrNull()
    )
}

/** Looks a song up on MusicBrainz; null without a confident match. Throws when MusicBrainz can't be reached. */
suspend fun findMusicBrainzTags(title: String, artist: String, album: String, durationMs: Long): MusicBrainzTags? =
    withContext(Dispatchers.IO) {
        fun quoted(value: String) = "\"" + value.trim().replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        val query = "recording:${quoted(title)}" + if (artist.isNotBlank()) " AND artist:${quoted(artist)}" else ""
        val url = "https://musicbrainz.org/ws/2/recording?fmt=json&limit=25&query=" + URLEncoder.encode(query, "UTF-8")
        MusicBrainzRateLimiter.await()
        val body = sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("MusicBrainz answered ${response.code}")
            response.body.string()
        }
        musicBrainzTagsFrom(body, durationMs, album)
    }
