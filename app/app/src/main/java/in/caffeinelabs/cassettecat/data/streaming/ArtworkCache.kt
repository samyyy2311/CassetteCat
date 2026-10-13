package `in`.caffeinelabs.cassettecat.data.streaming

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_DIMENSION = 300
private const val FULL_DIMENSION = 1440

// Keep thumbnails when the UI is hidden; memory pressure still clears both caches.
@Suppress("DEPRECATION")
internal fun shouldClearArtworkThumbnails(level: Int): Boolean =
    level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
        level in ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW until ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN

// Thumbnails and full-size images live in separate caches so dropping the large ones keeps lists drawing.
internal class ArtworkCache(thumbnailBytes: Int, fullBytes: Int) {
    private val thumbnails = bitmapCache(thumbnailBytes)
    private val full = bitmapCache(fullBytes)

    fun get(key: String, thumbnail: Boolean): Bitmap? = cacheFor(thumbnail).get(key)

    suspend fun getOrLoad(key: String, thumbnail: Boolean, fetch: (maxDimension: Int) -> Bitmap?): Bitmap? {
        val cache = cacheFor(thumbnail)
        cache.get(key)?.let { return it }
        val maxDimension = if (thumbnail) THUMBNAIL_DIMENSION else FULL_DIMENSION
        val bitmap = withContext(Dispatchers.IO) { fetch(maxDimension) } ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    fun clear() {
        thumbnails.evictAll()
        full.evictAll()
    }

    fun trim(level: Int) {
        if (shouldClearArtworkThumbnails(level)) {
            clear()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            full.evictAll()
        }
    }

    private fun cacheFor(thumbnail: Boolean) = if (thumbnail) thumbnails else full
}

private fun bitmapCache(maxBytes: Int) = object : LruCache<String, Bitmap>(maxBytes) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}
