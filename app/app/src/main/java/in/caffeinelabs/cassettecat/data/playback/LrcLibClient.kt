package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request

private val LRC_LINE_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.*)""")
private val CLEAN_TITLE_REGEX = Regex("""\s*[\(\[\{](?:feat\.?|ft\.?|remaster(?:ed)?|bonus|explicit|version|deluxe|edit|live|mono|stereo|single).*?[\)\]\}]""", RegexOption.IGNORE_CASE)

data class LyricLine(val timestampMs: Long, val text: String)

@Serializable
data class CachedLyrics(
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    // Set on a miss; the library fetch skips the song until MISSING_RETRY_MS has passed.
    val missingAt: Long? = null
)

enum class LibraryLookup { FOUND, MISSING, FAILED }

// Misses are retried after this, since LRCLIB keeps growing.
private const val MISSING_RETRY_MS = 30L * 24 * 60 * 60 * 1000

data class LyricsLookupResult(val syncedLyrics: List<LyricLine>? = null, val plainLyrics: String? = null)

@Serializable
data class LrcLibSearchResultItem(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

@Serializable
private data class LrcLibResponse(
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    val albumName: String? = null
)

// Free, no-auth synced-lyrics lookup with fallback query cleaning. Lyrics live in filesDir, not cacheDir: the system
// may clear the cache, and some were picked by hand.
class LrcLibClient(context: Context) {
    private val lyricsDir by lazy {
        File(context.filesDir, "lyrics").apply {
            mkdirs()
            File(context.cacheDir, "lyrics").listFiles()?.forEach { it.renameTo(File(this, it.name)) }
        }
    }

    suspend fun search(query: String, artist: String = ""): List<LrcLibSearchResultItem> = withContext(Dispatchers.IO) {
        runCatching {
            val url = if (artist.isNotBlank()) {
                "https://lrclib.net/api/search?track_name=${query.urlEncode()}&artist_name=${artist.urlEncode()}"
            } else {
                "https://lrclib.net/api/search?q=${query.urlEncode()}"
            }
            val response = sharedHttpClient.newCall(Request.Builder().url(url).build()).execute()
            val initialResults = response.use {
                if (!it.isSuccessful) emptyList()
                else sharedJson.decodeFromString<List<LrcLibSearchResultItem>>(it.body.string())
            }
            if (initialResults.isEmpty() && artist.isNotBlank()) {
                val fallbackUrl = "https://lrclib.net/api/search?q=${"$query $artist".urlEncode()}"
                val fallbackResponse = sharedHttpClient.newCall(Request.Builder().url(fallbackUrl).build()).execute()
                fallbackResponse.use {
                    if (!it.isSuccessful) emptyList()
                    else sharedJson.decodeFromString(it.body.string())
                }
            } else {
                initialResults
            }
        }.getOrDefault(emptyList())
    }

    fun saveLyricsToCache(artist: String, title: String, album: String, syncedLyrics: String?, plainLyrics: String?) {
        writeToCache(keyFor(artist, title, album), CachedLyrics(syncedLyrics, plainLyrics))
    }

    suspend fun fetchLyrics(artist: String, title: String, album: String): LyricsLookupResult? =
        withContext(Dispatchers.IO) {
            val cacheKey = keyFor(artist, title, album)
            readFromCache(cacheKey)?.let { return@withContext it }
            runCatching { lookUp(artist, title, album, cacheKey) }.getOrNull()
        }

    /** Stored lyrics as saved, synced or plain. */
    fun storedLyrics(artist: String, title: String, album: String): String? =
        readCached(keyFor(artist, title, album))?.let { it.syncedLyrics ?: it.plainLyrics }?.ifBlank { null }

    fun missingRecently(artist: String, title: String, album: String): Boolean {
        val missingAt = readCached(keyFor(artist, title, album))?.missingAt ?: return false
        return System.currentTimeMillis() - missingAt < MISSING_RETRY_MS
    }

    /** Like fetchLyrics, but tells a miss apart from a failed request. */
    suspend fun lookUpForLibrary(artist: String, title: String, album: String): LibraryLookup =
        withContext(Dispatchers.IO) {
            val cacheKey = keyFor(artist, title, album)
            runCatching { lookUp(artist, title, album, cacheKey) }.fold(
                onSuccess = { found ->
                    if (found != null) {
                        LibraryLookup.FOUND
                    } else {
                        writeToCache(cacheKey, CachedLyrics(missingAt = System.currentTimeMillis()))
                        LibraryLookup.MISSING
                    }
                },
                onFailure = { LibraryLookup.FAILED }
            )
        }

    // Throws when LRCLIB can't be reached; null when it has no lyrics for the song.
    private fun lookUp(artist: String, title: String, album: String, cacheKey: String): LyricsLookupResult? {
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

        if (candidates.isEmpty()) {
            val titleSearchUrl = "https://lrclib.net/api/search?q=${title.urlEncode()}"
            candidates = request(titleSearchUrl, isSearch = true) ?: emptyList()
        }
        if (candidates.isEmpty() && cleanTitle != title) {
            val cleanTitleSearchUrl = "https://lrclib.net/api/search?q=${cleanTitle.urlEncode()}"
            candidates = request(cleanTitleSearchUrl, isSearch = true) ?: emptyList()
        }

        val best = candidates
            .mapNotNull { response -> response.toResult()?.let { result -> result to response } }
            .maxWithOrNull(
                compareBy<Pair<LyricsLookupResult, LrcLibResponse>> { it.first.syncedLyrics != null }
                    .thenBy { it.second.albumName.equals(album, ignoreCase = true) }
            )

        return best?.let { (result, raw) ->
            writeToCache(cacheKey, CachedLyrics(raw.syncedLyrics, raw.plainLyrics))
            result
        }
    }

    private fun keyFor(artist: String, title: String, album: String) = hashKey("$artist|$title|$album".lowercase())

    private fun readCached(cacheKey: String): CachedLyrics? {
        val file = File(lyricsDir, "$cacheKey.json")
        if (!file.exists()) return null
        return runCatching { sharedJson.decodeFromString<CachedLyrics>(file.readText()) }.getOrNull()
    }

    private fun readFromCache(cacheKey: String): LyricsLookupResult? {
        val cached = readCached(cacheKey) ?: return null
        val synced = cached.syncedLyrics?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
        val plain = cached.plainLyrics?.trim()?.ifEmpty { null }
        return if (synced != null || plain != null) LyricsLookupResult(synced, plain) else null
    }

    private fun writeToCache(cacheKey: String, payload: CachedLyrics) {
        runCatching { File(lyricsDir, "$cacheKey.json").writeText(sharedJson.encodeToString(payload)) }
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
                .build()
        ).execute()
        response.use {
            if (!it.isSuccessful) return null
            val body = it.body.string()
            return if (isSearch) sharedJson.decodeFromString<List<LrcLibResponse>>(body) else listOf(sharedJson.decodeFromString(body))
        }
    }

    suspend fun publishLyrics(
        trackName: String,
        artistName: String,
        albumName: String,
        durationSeconds: Int,
        plainLyrics: String?,
        syncedLyrics: String?
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val challengeUrl = "https://lrclib.net/api/request-challenge"
            val challengeBody = sharedHttpClient.newCall(
                Request.Builder().url(challengeUrl).build()
            ).execute().use { if (!it.isSuccessful) null else it.body.string() } ?: return@withContext false

            val challenge = sharedJson.decodeFromString<LrcLibChallenge>(challengeBody)
            val token = solveChallenge(challenge.prefix, challenge.target)

            val publishUrl = "https://lrclib.net/api/publish"
            val payload = sharedJson.encodeToString(
                LrcLibPublishPayload(
                    trackName = trackName,
                    artistName = artistName,
                    albumName = albumName,
                    duration = durationSeconds,
                    plainLyrics = plainLyrics,
                    syncedLyrics = syncedLyrics
                )
            )

            val request = Request.Builder()
                .url(publishUrl)
                .header("User-Agent", "CassetteCat/1.5.0 (https://github.com/caffeinelabs/CassetteCat)")
                .header("X-Publish-Token", "${challenge.prefix}:$token")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            sharedHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun solveChallenge(prefix: String, target: String): String {
        var nonce = 0L
        val targetBytes = target.hexToByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        while (true) {
            val nonceStr = nonce.toString()
            val input = (prefix + nonceStr).toByteArray()
            val hash = md.digest(input)
            if (isHashValid(hash, targetBytes)) {
                return nonceStr
            }
            nonce++
            if (nonce > 5_000_000) break
        }
        return nonce.toString()
    }

    private fun isHashValid(hash: ByteArray, target: ByteArray): Boolean {
        for (i in 0 until minOf(hash.size, target.size)) {
            val h = hash[i].toInt() and 0xFF
            val t = target[i].toInt() and 0xFF
            if (h < t) return true
            if (h > t) return false
        }
        return true
    }

    private fun String.hexToByteArray(): ByteArray {
        val result = ByteArray(length / 2)
        for (i in result.indices) {
            result[i] = substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

    private fun LrcLibResponse.toResult(): LyricsLookupResult? {
        val synced = syncedLyrics?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
        val plain = plainLyrics?.trim()?.ifEmpty { null }
        return if (synced != null || plain != null) LyricsLookupResult(synced, plain) else null
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
}

@Serializable
private data class LrcLibChallenge(val prefix: String, val target: String)

@Serializable
private data class LrcLibPublishPayload(
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val duration: Int,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

fun adjustLyricsSync(syncedLyrics: List<LyricLine>, offsetMs: Long): List<LyricLine> =
    syncedLyrics.map { line ->
        line.copy(timestampMs = maxOf(0L, line.timestampMs + offsetMs))
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
