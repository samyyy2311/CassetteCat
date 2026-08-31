package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.device.SyncItemState
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PairingViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily

@Composable
fun DeviceSyncScreen(
    libraryViewModel: LibraryViewModel,
    pairingViewModel: PairingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val localSongs = remember(libraryState) {
        (libraryState as? LibraryUiState.Loaded)?.songs.orEmpty().filter { it.source == MusicSource.Local && it.filePath != null }
    }
    val syncStates by pairingViewModel.syncStates.collectAsStateWithLifecycle()
    val remoteManifest by pairingViewModel.remoteManifest.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { pairingViewModel.refreshSyncManifest() }

    val pending = remember(localSongs, remoteManifest) { pairingViewModel.pendingSyncSongs(localSongs) }
    val pendingIds = remember(pending) { pending.map { it.id }.toSet() }

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
            Column(modifier = Modifier.weight(1f)) {
                Text("Library Sync", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${localSongs.size - pending.size} of ${localSongs.size} songs on device",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (localSongs.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_cassette_tape,
                title = "No local songs",
                message = "Only songs stored on this phone can be synced to the player.",
                modifier = Modifier.weight(1f)
            )
        } else {
            if (pending.isNotEmpty()) {
                Button(
                    onClick = { pairingViewModel.syncSongs(pending) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text("Sync All Missing (${pending.size})")
                }
            }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(localSongs, key = { it.id }) { song ->
                    SyncSongRow(song = song, state = syncStates[song.id], isPending = song.id in pendingIds)
                }
            }
        }
    }
}

@Composable
private fun SyncSongRow(song: Song, state: SyncItemState?, isPending: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            when (state) {
                is SyncItemState.Uploading -> {
                    Text("Uploading", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
                is SyncItemState.Failed -> Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                SyncItemState.Queued -> Text("Queued", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SyncItemState.Done -> Text("On device", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                null -> Text(
                    if (isPending) "Not synced" else "On device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPending) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary
                )
            }
        }
        if (state == SyncItemState.Done || (state == null && !isPending)) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_circle_check_big),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
        } else if (state is SyncItemState.Failed) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_triangle_alert),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
