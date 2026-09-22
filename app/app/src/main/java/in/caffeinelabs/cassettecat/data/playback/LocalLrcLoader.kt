package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import `in`.caffeinelabs.cassettecat.data.library.Song
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

// MediaStore indexes sidecar .lrc files into readable content URIs under READ_MEDIA_AUDIO permissions.
class LocalLrcLoader(private val context: Context) {
    suspend fun loadFor(song: Song): List<LyricLine>? = withContext(Dispatchers.IO) {
        val audioFile = song.filePath?.let(::File) ?: return@withContext null
        val dir = audioFile.parentFile ?: return@withContext null
        val candidates = listOf(
            File(dir, audioFile.nameWithoutExtension + ".lrc"),
            File(dir, "${song.artist} - ${song.title}.lrc"),
            File(dir, "${song.title}.lrc")
        )

        candidates.firstOrNull { it.exists() }?.let { loadLrc(it.absolutePath) }
    }

    private suspend fun loadLrc(path: String): List<LyricLine>? {
        val uri = suspendCancellableCoroutine<Uri?> { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, scannedUri ->
                if (cont.isActive) cont.resume(scannedUri)
            }
        }
        val text = uri?.let {
            runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() } }
                .getOrNull()
        }
        return text?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
    }
}
