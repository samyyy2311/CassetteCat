package `in`.caffeinelabs.cassettecat.ui.widget

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackService
import `in`.caffeinelabs.cassettecat.data.playback.awaitController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var connectJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateTile()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        connectJob = scope.launch {
            val token = SessionToken(this@PlaybackTileService, ComponentName(this@PlaybackTileService, PlaybackService::class.java))
            runCatching {
                MediaController.Builder(this@PlaybackTileService, token).buildAsync().awaitController(this@PlaybackTileService)
            }.onSuccess { c ->
                controller = c
                c.addListener(playerListener)
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        connectJob?.cancel()
        connectJob = null
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    private fun updateTile() {
        val c = controller ?: return
        val isPlaying = c.isPlaying
        val title = c.mediaMetadata.title?.toString()
        qsTile?.apply {
            state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = title ?: "CassetteCat"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (isPlaying) "Playing" else "Paused"
            }
            icon = Icon.createWithResource(this@PlaybackTileService, if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
            updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
