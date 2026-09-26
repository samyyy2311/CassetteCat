package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.background
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.formatTime
import `in`.caffeinelabs.cassettecat.data.device.DesktopQueueTrack
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

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
            headerAction = {
                TextButton(onClick = hapticClick(viewModel::forget)) {
                    Text(stringResource(AppR.string.desktop_remote_forget))
                }
            },
            artwork = artwork
        ) {
            if (upNext.isNotEmpty()) DesktopUpNext(upNext, viewModel::playFromQueue)
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
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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

@Composable
private fun DesktopUpNext(tracks: List<DesktopQueueTrack>, onPlay: (Int) -> Unit) {
    Spacer(Modifier.height(32.dp))
    Text(
        stringResource(AppR.string.now_playing_up_next),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    tracks.forEach { track ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = hapticClick { onPlay(track.index) })
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (track.durationMs > 0) {
                Text(
                    formatTime(track.durationMs),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

/** Shows what the paired desktop is playing, with play/pause; renders nothing when there is nothing to show. */
@Composable
fun DesktopNowPlayingCard(onOpen: () -> Unit, modifier: Modifier = Modifier, viewModel: DesktopRemoteViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.playbackStatus.collectAsStateWithLifecycle()
    val artwork by viewModel.artwork.collectAsStateWithLifecycle()
    val paired = state.address != null && !state.offlineBlackout

    if (paired) {
        LifecycleResumeEffect(Unit) {
            viewModel.startPlaybackPolling()
            onPauseOrDispose { viewModel.stopPlaybackPolling() }
        }
    }
    val current = status?.takeIf { paired && it.trackTitle.isNotEmpty() } ?: return

    Row(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = hapticClick(onOpen))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            val art = artwork
            if (art != null) {
                Image(bitmap = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_monitor),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                stringResource(AppR.string.desktop_remote_on_computer),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(current.trackTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                current.trackArtist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        PressDepthIconButton(
            iconRes = if (current.isPlaying) R.drawable.lucide_ic_pause else R.drawable.lucide_ic_play,
            contentDescription = stringResource(AppR.string.widget_play_pause),
            onClick = { viewModel.sendPlaybackAction(if (current.isPlaying) "pause" else "play") },
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

