package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import `in`.caffeinelabs.cassettecat.data.streaming.shouldClearArtworkThumbnails
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_CACHE_BYTES = 16 * 1024 * 1024
private const val FULL_CACHE_BYTES = 24 * 1024 * 1024
private const val THUMBNAIL_DIMENSION = 300
private const val FULL_DIMENSION = 1440

class AlbumArtLoader(private val context: Context) {
    private val thumbnailCache = object : LruCache<String, Bitmap>(THUMBNAIL_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val fullCache = object : LruCache<String, Bitmap>(FULL_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val coverArtArchiveClient = CoverArtArchiveClient()
    private val albumCoverRepository = AlbumCoverRepository.getInstance(context)

    fun peek(song: Song, thumbnail: Boolean = true): Bitmap? = cacheFor(thumbnail).get(song.id)

    fun invalidate(songId: String) {
        thumbnailCache.remove(songId)
        fullCache.remove(songId)
    }

    fun clearCache() {
        thumbnailCache.evictAll()
        fullCache.evictAll()
    }

    @Suppress("DEPRECATION")
    fun trimCaches(level: Int) {
        if (shouldClearArtworkThumbnails(level)) {
            clearCache()
        } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            fullCache.evictAll()
        }
    }

    suspend fun load(song: Song, coverArtArchiveEnabled: Boolean = true, thumbnail: Boolean = true): Bitmap? {
        val cache = cacheFor(thumbnail)
        cache.get(song.id)?.let { return it }
        val maxDimension = if (thumbnail) THUMBNAIL_DIMENSION else FULL_DIMENSION
        val bitmap = withContext(Dispatchers.IO) {
            decodeCustomCover(song, maxDimension)
                ?: decode(song, maxDimension)
                ?: if (coverArtArchiveEnabled && song.album.isNotBlank() && song.artist.isNotBlank()) {
                    coverArtArchiveClient.fetchCoverArt(song.album, song.artist)
                } else null
        } ?: return null
        cache.put(song.id, bitmap)
        return bitmap
    }

    private fun decodeCustomCover(song: Song, maxDimension: Int): Bitmap? {
        val path = albumCoverRepository.getCoverPath(song.album, song.artist, song.albumId) ?: return null
        val file = java.io.File(path)
        return decodeSampledBitmap(file, maxDimension = maxDimension)
    }

    private fun cacheFor(thumbnail: Boolean) = if (thumbnail) thumbnailCache else fullCache

    private fun decode(song: Song, maxDimension: Int): Bitmap? {
        val embedded = runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, song.contentUri)
                retriever.embeddedPicture?.let { bytes ->
                    decodeSampledBitmap(bytes, maxDimension = maxDimension)
                }
            }
        }.getOrNull()
        if (embedded != null) return embedded

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.loadThumbnail(song.contentUri, Size(maxDimension, maxDimension), null)
            }.getOrNull()
        } else null
    }
}

// Closeable only from API 29+; this keeps `use { }` working uniformly down to minSdk 26.
private inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T {
    try {
        return block(this)
    } finally {
        release()
    }
}
