package `in`.caffeinelabs.cassettecat.data.library

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EditableAudioTags(
    val title: String,
    val artist: String,
    val album: String,
    val releaseYear: Int? = null,
    val genre: String? = null,
    val trackNumber: Int? = null
)

object AudioTagEditor {
    suspend fun saveTags(context: Context, song: Song, newTags: EditableAudioTags): Boolean = withContext(Dispatchers.IO) {
        val filePath = song.filePath

        runCatching {
            val contentUri = song.contentUri
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, newTags.title.trim())
                put(MediaStore.Audio.Media.ARTIST, newTags.artist.trim())
                put(MediaStore.Audio.Media.ALBUM, newTags.album.trim())
                newTags.releaseYear?.let { put(MediaStore.Audio.Media.YEAR, it) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    context.contentResolver.update(contentUri, values, null, null)
                }
            }

            if (filePath != null) {
                MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
            }
            true
        }.getOrDefault(false)
    }
}
