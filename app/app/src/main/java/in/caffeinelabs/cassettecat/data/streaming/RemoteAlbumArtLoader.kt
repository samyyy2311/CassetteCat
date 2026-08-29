package `in`.caffeinelabs.cassettecat.data.streaming

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private const val THUMBNAIL_CACHE_BYTES = 16 * 1024 * 1024
private const val FULL_CACHE_BYTES = 24 * 1024 * 1024
private const val THUMBNAIL_DIMENSION = 300
private const val FULL_DIMENSION = 1440

// Mirrors AlbumArtLoader but over HTTP for streamed sources (pre-authenticated Song.artUri).
// Blocking OkHttp call is fine since this already runs inside Dispatchers.IO.
class RemoteAlbumArtLoader {
    private val thumbnailCache = object : LruCache<String, Bitmap>(THUMBNAIL_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val fullCache = object : LruCache<String, Bitmap>(FULL_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun peek(artUri: Uri, thumbnail: Boolean = true): Bitmap? = cacheFor(thumbnail).get(artUri.toString())

    suspend fun load(artUri: Uri, thumbnail: Boolean = true): Bitmap? {
        val cache = cacheFor(thumbnail)
        val key = artUri.toString()
        cache.get(key)?.let { return it }
        val maxDimension = if (thumbnail) THUMBNAIL_DIMENSION else FULL_DIMENSION
        val bitmap = withContext(Dispatchers.IO) { fetch(key, maxDimension) } ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    private fun cacheFor(thumbnail: Boolean) = if (thumbnail) thumbnailCache else fullCache

    private fun fetch(url: String, maxDimension: Int): Bitmap? = runCatching {
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            response.body.bytes().let { decodeSampledBitmap(it, maxDimension = maxDimension) }
        }
    }.getOrNull()
}
