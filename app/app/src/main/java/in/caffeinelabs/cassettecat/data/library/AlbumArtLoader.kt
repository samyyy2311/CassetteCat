package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_CACHE_BYTES = 48 * 1024 * 1024
private val ARTWORK_REQUEST_SIZE = Size(1440, 1440)

class AlbumArtLoader(private val context: Context) {
    // Sized in bytes, not entry count: shared between library rows and the near-full-width
    // Now Playing art, so a fixed entry count wouldn't bound actual memory.
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val coverArtArchiveClient = CoverArtArchiveClient()

    fun peek(song: Song): Bitmap? = cache.get(song.id)

    suspend fun load(song: Song, coverArtArchiveEnabled: Boolean = true): Bitmap? {
        cache.get(song.id)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            decode(song) ?: if (coverArtArchiveEnabled && song.album.isNotBlank() && song.artist.isNotBlank()) {
                coverArtArchiveClient.fetchCoverArt(song.album, song.artist)
            } else null
        } ?: return null
        cache.put(song.id, bitmap)
        return bitmap
    }

    private fun decode(song: Song): Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            // Covers are rendered at most roughly full-screen. Asking MediaStore for the
            // original-sized bitmap turns a small scrolling row into a multi-megabyte decode
            // and GPU upload, which is visible as dropped frames on high-refresh displays.
            context.contentResolver.loadThumbnail(song.contentUri, ARTWORK_REQUEST_SIZE, null)
        }.getOrNull()
    } else {
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, song.contentUri)
                retriever.embeddedPicture?.let { bytes ->
                    decodeSampledBitmap(bytes)
                }
            }
        }.getOrNull()
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
