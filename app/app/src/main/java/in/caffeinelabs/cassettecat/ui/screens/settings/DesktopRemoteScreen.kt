package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.device.DesktopQueueTrack
import `in`.caffeinelabs.cassettecat.data.device.DevicePlaybackStatus
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibrarySongRow
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongListRowContent
import `in`.caffeinelabs.cassettecat.ui.util.LocalPlayingSong
import `in`.caffeinelabs.cassettecat.ui.util.PlayingSong
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

// Desktop tracks are shown with the app's own song rows; they have no file on this phone.
private fun desktopSong(id: String, title: String, artist: String, durationMs: Long) = Song(
    id = "desktop:$id",
    title = title,
    artist = artist,
    album = "",
    albumId = "",
    durationMs = durationMs,
    contentUri = Uri.EMPTY,
    source = MusicSource.Local
)

private fun DesktopQueueTrack.toSong() = desktopSong(index.toString(), title, artist, durationMs)

private fun DevicePlaybackStatus.toSong() = desktopSong("current", trackTitle, trackArtist, durationMs)

@Composable
fun DesktopRemoteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp,
    viewModel: DesktopRemoteViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val artwork by viewModel.artwork.collectAsStateWithLifecycle()
    val upNext by viewModel.upNext.collectAsStateWithLifecycle()
    val title = stringResource(AppR.string.desktop_remote_title)

    when {
        !state.loaded -> Unit
        state.offlineBlackout -> Column(modifier.fillMaxSize()) {
            RemoteScreenHeader(title, onBack)
            EmptyState(
                iconRes = R.drawable.lucide_ic_monitor,
                title = title,
                message = stringResource(AppR.string.desktop_remote_offline),
                modifier = Modifier.weight(1f)
            )
        }
        state.address != null -> DeviceNowPlayingScreen(
            remote = viewModel,
            onBack = onBack,
            modifier = modifier,
            listBottomPadding = listBottomPadding,
            title = title,
            waitingMessage = stringResource(AppR.string.desktop_remote_unreachable),
            artwork = artwork
        ) {
            if (upNext.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SettingsSection(title = stringResource(AppR.string.now_playing_up_next)) {
                    upNext.forEach { track ->
                        LibrarySongRow(song = track.toSong(), onClick = { viewModel.playFromQueue(track.index) })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            SettingsSection {
                ActionRow(
                    title = stringResource(AppR.string.desktop_remote_forget),
                    subtitle = stringResource(AppR.string.desktop_remote_forget_description),
                    iconRes = R.drawable.lucide_ic_x,
                    onClick = viewModel::forget
                )
            }
        }
        else -> DesktopPairingForm(title, onBack, viewModel::pair, modifier)
    }
}

@Composable
private fun DesktopPairingForm(title: String, onBack: () -> Unit, onPair: (String) -> Boolean, modifier: Modifier) {
    var address by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        RemoteScreenHeader(title, onBack)
        SettingsSection {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    stringResource(AppR.string.desktop_remote_pair_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        invalid = false
                    },
                    label = { Text(stringResource(AppR.string.desktop_remote_address)) },
                    placeholder = { Text("192.168.1.20:47800#ABC234", maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_monitor),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isError = invalid,
                    supportingText = if (invalid) {
                        { Text(stringResource(AppR.string.desktop_remote_address_invalid)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = hapticClick { invalid = !onPair(address) },
                    enabled = address.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(AppR.string.desktop_remote_connect))
                }
            }
        }
    }
}

/** Shows what the paired desktop is playing, with play/pause; renders nothing when there is nothing to show. */
@Composable
fun DesktopNowPlayingCard(onOpen: () -> Unit, modifier: Modifier = Modifier, viewModel: DesktopRemoteViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.playbackStatus.collectAsStateWithLifecycle()
    val paired = state.address != null && !state.offlineBlackout

    if (paired) {
        LifecycleResumeEffect(Unit) {
            viewModel.startPlaybackPolling()
            onPauseOrDispose { viewModel.stopPlaybackPolling() }
        }
    }
    val current = status?.takeIf { paired && it.trackTitle.isNotEmpty() } ?: return
    val song = current.toSong()

    Column(modifier) {
        SettingsSection(title = stringResource(AppR.string.desktop_remote_on_computer)) {
            // Marks the desktop's song as playing with the same overlay the song lists use.
            CompositionLocalProvider(LocalPlayingSong provides PlayingSong(song.id, current.isPlaying)) {
                Row(
                    modifier = Modifier.fillMaxWidth().tapScale(onOpen).padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SongListRowContent(song = song)
                    PressDepthIconButton(
                        iconRes = if (current.isPlaying) R.drawable.lucide_ic_pause else R.drawable.lucide_ic_play,
                        contentDescription = stringResource(AppR.string.widget_play_pause),
                        onClick = { viewModel.sendPlaybackAction(if (current.isPlaying) "pause" else "play") },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
