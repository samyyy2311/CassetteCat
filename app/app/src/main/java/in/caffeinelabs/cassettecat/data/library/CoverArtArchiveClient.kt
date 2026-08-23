package `in`.caffeinelabs.cassettecat.data.library

import android.graphics.Bitmap
import android.util.LruCache
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import okhttp3.Request

private const val MAX_CACHE_BYTES = 24 * 1024 * 1024

// MusicBrainz's usage policy caps unauthenticated requests at ~1/sec per IP; serialize
// search calls process-wide so concurrent album-art lookups don't burst past that.
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

        val bitmap = runCatching {
            val query = "release:${album.urlEncode()} AND artist:${artist.urlEncode()}"
            val mbUrl = "https://musicbrainz.org/ws/2/release/?query=$query&fmt=json&limit=3"
            MusicBrainzRateLimiter.await()
            val mbBody = getBody(mbUrl) ?: return@runCatching null
            val response = sharedJson.decodeFromString<MusicBrainzSearchResponse>(mbBody)
            val release = response.releases.firstOrNull() ?: return@runCatching null

            val caaUrl = "https://coverartarchive.org/release/${release.id}/front-500"
            downloadImage(caaUrl)
        }.getOrNull()

        if (bitmap != null) {
            cache.put(key, bitmap)
        }
        bitmap
    }

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
