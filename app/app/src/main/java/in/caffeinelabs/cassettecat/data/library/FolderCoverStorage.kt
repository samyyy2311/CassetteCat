package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return@runCatching null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_COVER_DIMENSION) {
                sampleSize *= 2
            }
            val scaled = resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                )
            } ?: return@runCatching null

            val file = coverFile(folderPath)
            FileOutputStream(file).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            file.absolutePath
        }.getOrNull()
    }

    fun delete(coverPath: String) {
        File(coverPath).delete()
    }

    private fun coverFile(folderPath: String): File {
        val dir = File(context.filesDir, "folder_covers").apply { mkdirs() }
        val digest = MessageDigest.getInstance("SHA-256").digest(folderPath.toByteArray())
        val id = digest.joinToString("") { "%02x".format(it) }
        return File(dir, "$id-${System.currentTimeMillis()}.jpg")
    }
}
