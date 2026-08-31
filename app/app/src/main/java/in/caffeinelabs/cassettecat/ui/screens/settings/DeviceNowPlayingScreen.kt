package `in`.caffeinelabs.cassettecat.ui.screens.settings

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.PlaybackControlsRow
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PairingViewModel

@Composable
fun DeviceNowPlayingScreen(
    pairingViewModel: PairingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status by pairingViewModel.playbackStatus.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        pairingViewModel.startPlaybackPolling()
        onPauseOrDispose { pairingViewModel.stopPlaybackPolling() }
    }

    var volumeOverride by remember { mutableStateOf<Float?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack
            )
            Text("Now Playing on Device", style = MaterialTheme.typography.headlineSmall)
        }

        if (status == null) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_music,
                title = "Not playing",
                message = "Waiting for the player to report its status.",
                modifier = Modifier.weight(1f)
            )
        } else {
            val current = status!!
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
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
                    positionMs = current.positionMs,
                    durationMs = current.durationMs,
                    onSeek = { pairingViewModel.seekDevicePlayback(it) },
                    isShuffleEnabled = current.shuffleEnabled,
                    onToggleShuffle = { pairingViewModel.sendPlaybackAction("toggle_shuffle") },
                    onSkipPrevious = { pairingViewModel.sendPlaybackAction("previous") },
                    isPlaying = current.isPlaying,
                    onTogglePlayPause = { pairingViewModel.sendPlaybackAction(if (current.isPlaying) "pause" else "play") },
                    onSkipNext = { pairingViewModel.sendPlaybackAction("next") },
                    repeatMode = current.repeatMode,
                    onCycleRepeatMode = { pairingViewModel.sendPlaybackAction("cycle_repeat") }
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
                            volumeOverride?.let { pairingViewModel.setDeviceVolume((it * 100).toInt()) }
                            volumeOverride = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
