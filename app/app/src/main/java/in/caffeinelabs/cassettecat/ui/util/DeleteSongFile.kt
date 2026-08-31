package `in`.caffeinelabs.cassettecat.ui.util

import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import `in`.caffeinelabs.cassettecat.data.library.Song
import java.io.File

fun deleteSongFile(context: Context, song: Song, recoveryLauncher: ActivityResultLauncher<IntentSenderRequest>) {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(song.contentUri))
        recoveryLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        return
    }
    try {
        resolver.delete(song.contentUri, null, null)
        song.filePath?.let { File(it).delete() }
    } catch (e: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
            recoveryLauncher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
        }
    }
}
