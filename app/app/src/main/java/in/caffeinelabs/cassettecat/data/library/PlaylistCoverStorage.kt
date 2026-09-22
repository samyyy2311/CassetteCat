package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_COVER_DIMENSION = 512
private const val JPEG_QUALITY = 85

// Copies selected covers into app-private storage since picker URIs do not survive process death.
class PlaylistCoverStorage(private val context: Context) {
    suspend fun save(playlistId: String, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return@runCatching null
            val scaled = decodeSampledBitmap(bytes, maxDimension = MAX_COVER_DIMENSION) ?: return@runCatching null
            val file = coverFile(playlistId)
            FileOutputStream(file).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            file.absolutePath
        }.getOrNull()
    }

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
}
