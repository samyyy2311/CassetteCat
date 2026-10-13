@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.ui.widget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackService

/**
 * Carries out the widget's buttons other than play. It talks to the player through its media session, which Android
 * allows while the app is in the background, unlike starting the player's service directly.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        // A receiver's own context may not bind to services; the application's may.
        val appContext = context.applicationContext
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull()
            try {
                controller?.let { perform(it, action) }
            } finally {
                controller?.release()
                pending.finish()
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun perform(controller: MediaController, action: String) {
        when (action) {
            ACTION_NEXT -> controller.seekToNext()
            ACTION_PREVIOUS -> controller.seekToPrevious()
            ACTION_SHUFFLE -> controller.shuffleModeEnabled = !controller.shuffleModeEnabled
            ACTION_REPEAT -> controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            ACTION_LIKE -> controller.sendCustomCommand(
                SessionCommand(PlaybackService.ACTION_CUSTOM_FAVORITE, Bundle.EMPTY),
                Bundle.EMPTY
            )
        }
    }

    companion object {
        const val ACTION_NEXT = "in.caffeinelabs.cassettecat.widget.NEXT"
        const val ACTION_PREVIOUS = "in.caffeinelabs.cassettecat.widget.PREVIOUS"
        const val ACTION_SHUFFLE = "in.caffeinelabs.cassettecat.widget.SHUFFLE"
        const val ACTION_REPEAT = "in.caffeinelabs.cassettecat.widget.REPEAT"
        const val ACTION_LIKE = "in.caffeinelabs.cassettecat.widget.LIKE"
    }
}
