package `in`.caffeinelabs.cassettecat.data.streaming

import android.graphics.Bitmap
import android.net.Uri
import okhttp3.Request

private const val THUMBNAIL_CACHE_BYTES = 16 * 1024 * 1024
private const val FULL_CACHE_BYTES = 24 * 1024 * 1024

// In-memory two-tier cache (thumbnail and full-res) for remote artwork.
class RemoteAlbumArtLoader {
    private val cache = ArtworkCache(THUMBNAIL_CACHE_BYTES, FULL_CACHE_BYTES)

    fun clearCache() = cache.clear()

    fun trimCaches(level: Int) = cache.trim(level)

    fun peek(artUri: Uri, thumbnail: Boolean = true): Bitmap? = cache.get(artUri.toString(), thumbnail)

    suspend fun load(artUri: Uri, thumbnail: Boolean = true): Bitmap? {
        val url = artUri.toString()
        return cache.getOrLoad(url, thumbnail) { fetch(url, it) }
    }

    private fun fetch(url: String, maxDimension: Int): Bitmap? = runCatching {
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            response.body.bytes().let { decodeSampledBitmap(it, maxDimension = maxDimension) }
        }
    }.getOrNull()

    companion object {
        @Volatile
        private var instance: RemoteAlbumArtLoader? = null

        fun getInstance(): RemoteAlbumArtLoader =
            instance ?: synchronized(this) {
                instance ?: RemoteAlbumArtLoader().also { instance = it }
            }
    }
}
