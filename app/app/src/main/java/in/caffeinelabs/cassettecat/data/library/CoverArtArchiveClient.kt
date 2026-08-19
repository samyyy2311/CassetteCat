package `in`.caffeinelabs.cassettecat.data.library

import android.graphics.Bitmap
import android.util.LruCache
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import okhttp3.Request

private const val MAX_CACHE_BYTES = 24 * 1024 * 1024

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
                .header("User-Agent", "CassetteCat/0.1.0 (https://github.com/samyyy2311/CassetteCat)")
                .build()
        ).execute().use {
            if (!it.isSuccessful) null else it.body.string()
        }

    private fun downloadImage(url: String): Bitmap? =
        sharedHttpClient.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "CassetteCat/0.1.0 (https://github.com/samyyy2311/CassetteCat)")
                .build()
        ).execute().use {
            if (!it.isSuccessful) return null
            it.body.bytes().let { bytes -> decodeSampledBitmap(bytes, maxDimension = 1440) }
        }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
}
