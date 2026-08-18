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

// The app only holds READ_MEDIA_AUDIO, not broad storage access, so a sidecar .lrc file
// isn't reliably readable via a raw file path. Scanning its derived path indexes it (if
// present) and hands back a content:// Uri the app can actually open.
class LocalLrcLoader(private val context: Context) {
    suspend fun loadFor(song: Song): List<LyricLine>? = withContext(Dispatchers.IO) {
        val audioPath = song.filePath ?: return@withContext null
        val lrcPath = File(audioPath).let { File(it.parentFile, it.nameWithoutExtension + ".lrc") }.absolutePath

        val uri = suspendCancellableCoroutine<Uri?> { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(lrcPath), null) { _, scannedUri ->
                if (cont.isActive) cont.resume(scannedUri)
            }
        }

        val text = uri?.let {
            runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() } }
                .getOrNull()
        }
        text?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
    }
}
