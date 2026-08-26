package `in`.caffeinelabs.cassettecat.data.playback

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
    val plainLyrics: String? = null
)

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

// Free, no-auth synced-lyrics lookup with disk caching and fallback query cleaning.
class LrcLibClient(private val cacheDir: File? = null) {
    private val lyricsCacheDir by lazy {
        cacheDir?.let { File(it, "lyrics").apply { mkdirs() } }
    }

    suspend fun search(query: String): List<LrcLibSearchResultItem> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://lrclib.net/api/search?q=${query.urlEncode()}"
            val response = sharedHttpClient.newCall(Request.Builder().url(url).build()).execute()
            response.use {
                if (!it.isSuccessful) emptyList()
                else {
                    val body = it.body.string()
                    sharedJson.decodeFromString<List<LrcLibSearchResultItem>>(body)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveLyricsToCache(artist: String, title: String, album: String, syncedLyrics: String?, plainLyrics: String?) {
        val cacheKey = hashKey("$artist|$title|$album".lowercase())
        writeToCache(cacheKey, syncedLyrics, plainLyrics)
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
            val cached = sharedJson.decodeFromString<CachedLyrics>(file.readText())
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
            file.writeText(sharedJson.encodeToString(payload))
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
