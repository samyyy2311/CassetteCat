package `in`.caffeinelabs.cassettecat.data.playback

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Request

private val json = Json { ignoreUnknownKeys = true }
private val LRC_LINE_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.*)""")
private val CLEAN_TITLE_REGEX = Regex("""\s*[\(\[\{](?:feat\.?|ft\.?|remaster(?:ed)?|bonus|explicit|version|deluxe|edit|live|mono|stereo|single).*?[\)\]\}]""", RegexOption.IGNORE_CASE)

data class LyricLine(val timestampMs: Long, val text: String)

@Serializable
data class CachedLyrics(
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null
)

data class LyricsLookupResult(val syncedLyrics: List<LyricLine>? = null, val plainLyrics: String? = null)

@Serializable
private data class LrcLibResponse(
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    val albumName: String? = null
)

// Free, no-auth synced-lyrics lookup with disk caching and fallback query cleaning.
class LrcLibClient(private val cacheDir: File? = null) {
    private val lyricsCacheDir by lazy {
        cacheDir?.let { File(it, "lyrics").apply { mkdirs() } }
    }

    suspend fun fetchLyrics(artist: String, title: String, album: String): LyricsLookupResult? =
        withContext(Dispatchers.IO) {
            val cacheKey = hashKey("$artist|$title|$album".lowercase())
            readFromCache(cacheKey)?.let { return@withContext it }

            runCatching {
                val exactUrl = "https://lrclib.net/api/get?artist_name=${artist.urlEncode()}" +
                    "&track_name=${title.urlEncode()}&album_name=${album.urlEncode()}"
                val searchUrl = "https://lrclib.net/api/search?artist_name=${artist.urlEncode()}" +
                    "&track_name=${title.urlEncode()}"

                var candidates = (request(exactUrl) ?: emptyList()) + (request(searchUrl, isSearch = true) ?: emptyList())

                val cleanTitle = cleanTitle(title)
                if (candidates.isEmpty() && cleanTitle != title) {
                    val cleanSearchUrl = "https://lrclib.net/api/search?artist_name=${artist.urlEncode()}" +
                        "&track_name=${cleanTitle.urlEncode()}"
                    candidates = request(cleanSearchUrl, isSearch = true) ?: emptyList()
                }

                val best = candidates
                    .mapNotNull { response -> response.toResult()?.let { result -> result to response } }
                    .maxWithOrNull(
                        compareBy<Pair<LyricsLookupResult, LrcLibResponse>> { it.first.syncedLyrics != null }
                            .thenBy { it.second.albumName.equals(album, ignoreCase = true) }
                    )

                best?.let { (result, raw) ->
                    writeToCache(cacheKey, raw.syncedLyrics, raw.plainLyrics)
                    result
                }
            }.getOrNull()
        }

    private fun readFromCache(cacheKey: String): LyricsLookupResult? {
        val file = lyricsCacheDir?.let { File(it, "$cacheKey.json") } ?: return null
        if (!file.exists()) return null
        return runCatching {
            val cached = json.decodeFromString<CachedLyrics>(file.readText())
            val synced = cached.syncedLyrics?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
            val plain = cached.plainLyrics?.trim()?.ifEmpty { null }
            if (synced != null || plain != null) LyricsLookupResult(synced, plain) else null
        }.getOrNull()
    }

    private fun writeToCache(cacheKey: String, syncedLyrics: String?, plainLyrics: String?) {
        val dir = lyricsCacheDir ?: return
        runCatching {
            val file = File(dir, "$cacheKey.json")
            val payload = CachedLyrics(syncedLyrics = syncedLyrics, plainLyrics = plainLyrics)
            file.writeText(json.encodeToString(payload))
        }
    }

    private fun cleanTitle(title: String): String {
        return title.replace(CLEAN_TITLE_REGEX, "").trim()
    }

    private fun hashKey(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
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
