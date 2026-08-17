package `in`.caffeinelabs.cassettecat.data.playback

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request

private val json = Json { ignoreUnknownKeys = true }
private val LRC_LINE_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.*)""")

data class LyricLine(val timestampMs: Long, val text: String)
data class LyricsLookupResult(val syncedLyrics: List<LyricLine>? = null, val plainLyrics: String? = null)

@Serializable
private data class LrcLibResponse(
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    val albumName: String? = null
)

// Free, no-auth synced-lyrics lookup, used only when the track has no embedded lyrics.
class LrcLibClient {
    suspend fun fetchLyrics(artist: String, title: String, album: String): LyricsLookupResult? =
        withContext(Dispatchers.IO) {
            runCatching {
                val exactUrl = "https://lrclib.net/api/get?artist_name=${artist.urlEncode()}" +
                    "&track_name=${title.urlEncode()}&album_name=${album.urlEncode()}"
                // Album metadata is frequently missing or formatted differently. Search gives
                // a useful fallback without broadening the query beyond artist + track title.
                val searchUrl = "https://lrclib.net/api/search?artist_name=${artist.urlEncode()}" +
                    "&track_name=${title.urlEncode()}"
                val candidates = (request(exactUrl) ?: emptyList()) + (request(searchUrl, isSearch = true) ?: emptyList())
                candidates
                    .mapNotNull { response -> response.toResult()?.let { result -> result to response } }
                    .maxWithOrNull(
                        compareBy<Pair<LyricsLookupResult, LrcLibResponse>> { it.first.syncedLyrics != null }
                            .thenBy { it.second.albumName.equals(album, ignoreCase = true) }
                    )
                    ?.first
            }.getOrNull()
        }

    private fun request(url: String, isSearch: Boolean = false): List<LrcLibResponse>? {
        val response = sharedHttpClient.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "CassetteCat/0.1.0")
                .build()
        ).execute()
        response.use {
            if (!it.isSuccessful) return null
            val body = it.body.string()
            return if (isSearch) json.decodeFromString<List<LrcLibResponse>>(body) else listOf(json.decodeFromString(body))
        }
    }

    private fun LrcLibResponse.toResult(): LyricsLookupResult? {
        val synced = syncedLyrics?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
        val plain = plainLyrics?.trim()?.ifEmpty { null }
        return if (synced != null || plain != null) LyricsLookupResult(synced, plain) else null
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
}

fun parseLrc(lrc: String): List<LyricLine> =
    lrc.lineSequence()
        .flatMap { line -> LRC_LINE_REGEX.findAll(line).asSequence() }
        .mapNotNull { match ->
            val (min, sec, fraction, text) = match.destructured
            if (text.isBlank()) return@mapNotNull null
            val millis = fraction.ifBlank { "0" }.padEnd(3, '0').take(3).toLong()
            LyricLine(min.toLong() * 60_000 + sec.toLong() * 1000 + millis, text.trim())
        }
        .sortedBy { it.timestampMs }
        .toList()
