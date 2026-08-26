package `in`.caffeinelabs.cassettecat.data.library

import android.graphics.Bitmap
import android.util.LruCache
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request

private const val MAX_CACHE_BYTES = 36 * 1024 * 1024

private object MusicBrainzRateLimiter {
    private const val MIN_INTERVAL_MS = 1100L
    private val mutex = Mutex()
    private var lastRequestAtMs = 0L

    suspend fun await() {
        mutex.withLock {
            val elapsed = System.currentTimeMillis() - lastRequestAtMs
            if (elapsed < MIN_INTERVAL_MS) delay(MIN_INTERVAL_MS - elapsed)
            lastRequestAtMs = System.currentTimeMillis()
        }
    }
}

@Serializable
private data class ITunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ITunesAlbum> = emptyList()
)

@Serializable
private data class ITunesAlbum(
    val collectionName: String? = null,
    val artistName: String? = null,
    val artworkUrl100: String? = null
)

@Serializable
private data class DeezerAlbumSearchResponse(
    val data: List<DeezerAlbum> = emptyList()
)

@Serializable
private data class DeezerAlbum(
    val title: String? = null,
    val cover_xl: String? = null,
    val cover_big: String? = null,
    val cover_medium: String? = null
)

@Serializable
private data class MusicBrainzSearchResponse(
    val releases: List<MusicBrainzRelease> = emptyList()
)

@Serializable
private data class MusicBrainzRelease(
    val id: String,
    val title: String? = null,
    val score: Int? = null
)

class CoverArtArchiveClient {
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun peek(album: String, artist: String): Bitmap? = cache.get("$album|$artist".lowercase())

    suspend fun fetchCoverArt(album: String, artist: String): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$album|$artist".lowercase()
        cache.get(key)?.let { return@withContext it }

        // Multi-source ultra-high-resolution cover lookup: iTunes (1400px) -> Deezer (1000px) -> MusicBrainz/CAA (1200px/original)
        val bitmap = fetchFromITunes(album, artist)
            ?: fetchFromDeezer(album, artist)
            ?: fetchFromMusicBrainz(album, artist)

        if (bitmap != null) {
            cache.put(key, bitmap)
        }
        bitmap
    }

    private fun fetchFromITunes(album: String, artist: String): Bitmap? = runCatching {
        val cleanAlbum = sanitizeQuery(album)
        val cleanArtist = sanitizeQuery(artist)
        val query = "$cleanAlbum $cleanArtist".trim()
        val url = "https://itunes.apple.com/search?term=${query.urlEncode()}&entity=album&limit=5"
        val body = getBody(url) ?: return@runCatching null
        val response = sharedJson.decodeFromString<ITunesSearchResponse>(body)
        val match = response.results.firstOrNull { it.artworkUrl100 != null } ?: return@runCatching null
        val url100 = match.artworkUrl100 ?: return@runCatching null
        val highResUrl = url100
            .replace(Regex("\\d+x\\d+bb[^\"]*"), "1400x1400bb.jpg")
            .replace(Regex("/\\d+x\\d+[^\"]*"), "/1400x1400bb.jpg")
        downloadImage(highResUrl) ?: downloadImage(url100)
    }.getOrNull()

    private fun fetchFromDeezer(album: String, artist: String): Bitmap? = runCatching {
        val cleanAlbum = sanitizeQuery(album)
        val cleanArtist = sanitizeQuery(artist)
        val query = "$cleanAlbum $cleanArtist".trim()
        val url = "https://api.deezer.com/search/album?q=${query.urlEncode()}&limit=5"
        val body = getBody(url) ?: return@runCatching null
        val response = sharedJson.decodeFromString<DeezerAlbumSearchResponse>(body)
        val match = response.data.firstOrNull() ?: return@runCatching null
        val coverUrl = match.cover_xl ?: match.cover_big ?: match.cover_medium ?: return@runCatching null
        downloadImage(coverUrl)
    }.getOrNull()

    private suspend fun fetchFromMusicBrainz(album: String, artist: String): Bitmap? = runCatching {
        val cleanAlbum = sanitizeQuery(album)
        val cleanArtist = sanitizeQuery(artist)
        val query = "release:\"${cleanAlbum}\" AND artist:\"${cleanArtist}\""
        val mbUrl = "https://musicbrainz.org/ws/2/release/?query=${query.urlEncode()}&fmt=json&limit=3"
        MusicBrainzRateLimiter.await()
        val mbBody = getBody(mbUrl) ?: return@runCatching null
        val response = sharedJson.decodeFromString<MusicBrainzSearchResponse>(mbBody)
        val release = response.releases.firstOrNull() ?: return@runCatching null

        // Try high-resolution 1200px first, fallback to original uncompressed scan, then 500px
        val caa1200 = "https://coverartarchive.org/release/${release.id}/front-1200"
        val caaOriginal = "https://coverartarchive.org/release/${release.id}/front"
        val caa500 = "https://coverartarchive.org/release/${release.id}/front-500"
        downloadImage(caa1200) ?: downloadImage(caaOriginal) ?: downloadImage(caa500)
    }.getOrNull()

    private fun sanitizeQuery(input: String): String =
        input.replace(Regex("(?i)\\[(remastered|deluxe|bonus|explicit|expanded|anniversary|edition|version|mono|stereo|reissue)[^\\]]*\\]"), "")
            .replace(Regex("(?i)\\((remastered|deluxe|bonus|explicit|expanded|anniversary|edition|version|mono|stereo|reissue|feat\\.?)[^\\)]*\\)"), "")
            .replace(Regex("(?i)-\\s*(remastered|deluxe|bonus|expanded|anniversary).*$"), "")
            .trim()

    private fun getBody(url: String): String? =
        sharedHttpClient.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).execute().use {
            if (!it.isSuccessful) null else it.body.string()
        }

    private fun downloadImage(url: String): Bitmap? =
        sharedHttpClient.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).execute().use {
            if (!it.isSuccessful) return null
            it.body.bytes().let { bytes -> decodeSampledBitmap(bytes, maxDimension = 1440) }
        }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
}
