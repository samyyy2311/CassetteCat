package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_COVER_DIMENSION = 512
private const val JPEG_QUALITY = 85

class FolderCoverStorage(private val context: Context) {
    suspend fun save(folderPath: String, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return@runCatching null
            val scaled = decodeSampledBitmap(bytes, maxDimension = MAX_COVER_DIMENSION) ?: return@runCatching null
            val file = coverFile(folderPath)
            FileOutputStream(file).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            file.absolutePath
        }.getOrNull()
    }

    fun delete(folderPath: String) {
        coverFile(folderPath).delete()
    }

    private fun coverFile(folderPath: String): File {
        val dir = File(context.filesDir, "folder_covers").apply { mkdirs() }
        val digest = MessageDigest.getInstance("SHA-256").digest(folderPath.toByteArray())
        val id = digest.joinToString("") { "%02x".format(it) }
        return File(dir, "$id.jpg")
    }
}
