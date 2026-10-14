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
internal fun musicBrainzTagsFrom(json: String, title: String, durationMs: Long, album: String): MusicBrainzTags? {
    val confident = sharedJson.decodeFromString<RecordingSearch>(json).recordings.filter { recording ->
        val length = recording.length ?: 0L
        recording.score >= MINIMUM_SCORE &&
            (durationMs <= 0 || length <= 0 || abs(length - durationMs) <= LENGTH_TOLERANCE_MS)
    }
    // A recording with the song's own title wins over versions of it, such as a remix of the same length.
    val fitting = confident.filter { comparable(it.title) == comparable(title) }.ifEmpty { confident }
    if (fitting.isEmpty()) return null
    val candidates = fitting.flatMap { recording -> recording.releases.map { recording to it } }
    fun earliest(matching: (Release) -> Boolean) = candidates
        .filter { (_, release) -> matching(release) }
        .minByOrNull { (_, release) -> release.date?.ifBlank { null } ?: "9999" }
    // Of the releases with the song's album name, the earliest is the original rather than a reissue.
    val (recording, release) = earliest { album.isNotBlank() && it.title.equals(album.trim(), ignoreCase = true) }
        ?: earliest { it.isStudioAlbum }
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
        musicBrainzTagsFrom(body, title, durationMs, album)
    }

/** A song detail a MusicBrainz match would change. */
@Serializable
data class TagChange(val field: TagField, val from: String, val to: String)

enum class TagField { TITLE, ARTIST, ALBUM, YEAR }

// Spelling that differs only in case, quote or dash style, or spacing reads the same, so it isn't a change.
private fun comparable(value: String): String = value.lowercase()
    .replace(Regex("[\u2018\u2019\u02bc`\u00b4]"), "'")
    .replace(Regex("[\u201c\u201d]"), "\"")
    .replace(Regex("[\u2010-\u2015]"), "-")
    .replace(Regex("\\s+"), " ")
    .trim()

// "Currents (Deluxe)" extends "Currents" with more words, but "Lovely" doesn't extend "Love".
private fun extends(longer: String, shorter: String): Boolean {
    val (whole, start) = comparable(longer) to comparable(shorter)
    return whole.startsWith(start) && whole.getOrNull(start.length)?.isLetterOrDigit() == false
}

/**
 * What [tags] would change in a song with these details. The year is only filled in when missing, since editions of
 * an album share a name but not their year, and an album named as MusicBrainz's plus an edition, such as "(Deluxe)",
 * is kept.
 */
internal fun tagChanges(title: String, artist: String, album: String, year: Int?, tags: MusicBrainzTags): List<TagChange> {
    fun change(field: TagField, from: String, to: String?): TagChange? =
        to?.takeIf { comparable(from) != comparable(it) }?.let { TagChange(field, from, it) }
    return listOfNotNull(
        // A title that only gains words, such as " - Remix" or " (Live)", belongs to another version of the song.
        change(TagField.TITLE, title, tags.title)?.takeUnless { title.isNotBlank() && extends(it.to, title) },
        change(TagField.ARTIST, artist, tags.artist),
        change(TagField.ALBUM, album, tags.album)?.takeUnless { extends(album, it.to) },
        if (year == null) change(TagField.YEAR, "", tags.year?.toString()) else null
    )
}
