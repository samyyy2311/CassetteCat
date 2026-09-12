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

private const val MAX_ALBUM_COVER_DIMENSION = 1440
private const val JPEG_QUALITY = 92

class AlbumCoverStorage(private val context: Context) {
    suspend fun save(albumKey: String, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = coverFile(albumKey)
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            file.absolutePath
        }.getOrNull()
    }

    suspend fun save(albumKey: String, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return@runCatching null
            val scaled = decodeSampledBitmap(bytes, maxDimension = MAX_ALBUM_COVER_DIMENSION) ?: return@runCatching null
            save(albumKey, scaled)
        }.getOrNull()
    }

    fun delete(path: String) {
        File(path).delete()
    }

    fun getStorageSizeBytes(): Long {
        val dir = File(context.filesDir, "album_covers")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearAll() {
        val dir = File(context.filesDir, "album_covers")
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun coverFile(albumKey: String): File {
        val dir = File(context.filesDir, "album_covers").apply { mkdirs() }
        val digest = MessageDigest.getInstance("SHA-256").digest(albumKey.toByteArray())
        val id = digest.joinToString("") { "%02x".format(it) }
        return File(dir, "$id-${System.currentTimeMillis()}.jpg")
    }
}
