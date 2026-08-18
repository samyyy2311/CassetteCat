package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_COVER_DIMENSION = 512
private const val JPEG_QUALITY = 85

// Photo Picker read grants aren't guaranteed to survive process death, so a picked
// image is copied into app-private storage immediately rather than referenced by URI.
class PlaylistCoverStorage(private val context: Context) {
    suspend fun save(playlistId: String, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = context.contentResolver.openInputStream(sourceUri)
                ?.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
            val scaled = bitmap.scaledDownTo(MAX_COVER_DIMENSION)
            val file = coverFile(playlistId)
            FileOutputStream(file).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            file.absolutePath
        }.getOrNull()
    }

    // used by backup restore: the bytes are already a downscaled/compressed JPEG
    suspend fun restore(playlistId: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val file = coverFile(playlistId)
        FileOutputStream(file).use { it.write(bytes) }
        file.absolutePath
    }

    fun delete(path: String) {
        File(path).delete()
    }

    private fun coverFile(playlistId: String): File {
        val dir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
        return File(dir, "$playlistId.jpg")
    }

    private fun Bitmap.scaledDownTo(maxDimension: Int): Bitmap {
        val largestSide = maxOf(width, height)
        if (largestSide <= maxDimension) return this
        val scale = maxDimension.toFloat() / largestSide
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    }
}
