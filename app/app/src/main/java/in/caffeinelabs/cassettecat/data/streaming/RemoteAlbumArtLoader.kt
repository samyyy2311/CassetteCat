package `in`.caffeinelabs.cassettecat.data.streaming

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private const val MAX_CACHE_BYTES = 48 * 1024 * 1024

// Mirrors AlbumArtLoader but over HTTP for streamed sources (pre-authenticated Song.artUri).
// Blocking OkHttp call is fine since this already runs inside Dispatchers.IO.
class RemoteAlbumArtLoader {
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun peek(artUri: Uri): Bitmap? = cache.get(artUri.toString())

    suspend fun load(artUri: Uri): Bitmap? {
        val key = artUri.toString()
        cache.get(key)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) { fetch(key) } ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    private fun fetch(url: String): Bitmap? = runCatching {
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            response.body.bytes().let { decodeSampledBitmap(it) }
        }
    }.getOrNull()
}
