package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.PlaybackControlsRow
import `in`.caffeinelabs.cassettecat.data.device.DevicePlaybackStatus
import kotlinx.coroutines.flow.StateFlow

/** A player that speaks the device playback API: the CassetteCat hardware or the desktop app. */
interface PlaybackRemote {
    val playbackStatus: StateFlow<DevicePlaybackStatus?>
    fun startPlaybackPolling()
    fun stopPlaybackPolling()
    fun sendPlaybackAction(action: String)
    fun setDeviceVolume(percent: Int)
    fun seekDevicePlayback(positionMs: Long)
}

@Composable
fun DeviceNowPlayingScreen(
    remote: PlaybackRemote,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp,
    title: String = stringResource(AppR.string.device_now_playing_title),
    waitingMessage: String = stringResource(AppR.string.device_now_playing_waiting),
    headerAction: @Composable () -> Unit = {},
    artwork: ImageBitmap? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val status by remote.playbackStatus.collectAsStateWithLifecycle()
    // Status arrives every couple of seconds; advance the position locally in between.
    var positionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(status) {
        val current = status ?: return@LaunchedEffect
        val receivedAt = SystemClock.elapsedRealtime()
        do {
            val elapsed = if (current.isPlaying) SystemClock.elapsedRealtime() - receivedAt else 0L
            positionMs = (current.positionMs + elapsed).coerceAtMost(current.durationMs)
            delay(250)
        } while (current.isPlaying)
    }

    LifecycleResumeEffect(remote) {
        remote.startPlaybackPolling()
        onPauseOrDispose { remote.stopPlaybackPolling() }
    }

    var volumeOverride by remember { mutableStateOf<Float?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        RemoteScreenHeader(title, onBack, headerAction)

        if (status == null) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_music,
                title = stringResource(AppR.string.widget_not_playing),
                message = waitingMessage,
                modifier = Modifier.weight(1f)
            )
        } else {
            val current = status!!
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                if (artwork != null) {
                    Image(
                        bitmap = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(24.dp))
                }
                Text(current.trackTitle, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    current.trackArtist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(24.dp))

                PlaybackControlsRow(
                    positionMs = positionMs,
                    durationMs = current.durationMs,
                    onSeek = { remote.seekDevicePlayback(it) },
                    isShuffleEnabled = current.shuffleEnabled,
                    onToggleShuffle = { remote.sendPlaybackAction("toggle_shuffle") },
                    onSkipPrevious = { remote.sendPlaybackAction("previous") },
                    isPlaying = current.isPlaying,
                    onTogglePlayPause = { remote.sendPlaybackAction(if (current.isPlaying) "pause" else "play") },
                    onSkipNext = { remote.sendPlaybackAction("next") },
                    repeatMode = current.repeatMode,
                    onCycleRepeatMode = { remote.sendPlaybackAction("cycle_repeat") }
                )

                Spacer(Modifier.height(32.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_volume_2),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        value = volumeOverride ?: (current.volumePercent / 100f),
                        onValueChange = { volumeOverride = it },
                        onValueChangeFinished = {
                            volumeOverride?.let { remote.setDeviceVolume((it * 100).toInt()) }
                            volumeOverride = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                content()

                Spacer(Modifier.height(listBottomPadding))
            }
        }
    }
}

@Composable
internal fun RemoteScreenHeader(title: String, onBack: () -> Unit, action: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_chevron_left,
            contentDescription = stringResource(AppR.string.action_back),
            onClick = onBack
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        action()
    }
}
