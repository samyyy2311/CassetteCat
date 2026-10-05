package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.util.deleteSongFile
import `in`.caffeinelabs.cassettecat.ui.util.retryDeleteAfterConsent

private const val DUPLICATE_DURATION_TOLERANCE_MS = 3_000L

internal fun findDuplicateGroups(songs: List<Song>): List<List<Song>> =
    songs.filter { it.source == MusicSource.Local }
        .groupBy { it.title.trim().lowercase() to it.artist.trim().lowercase() }
        .entries
        .sortedWith(compareBy({ it.key.first }, { it.key.second }))
        .flatMap { (_, group) -> splitByDuration(group) }
        .filter { it.size > 1 }

private fun splitByDuration(songs: List<Song>): List<List<Song>> {
    val clusters = mutableListOf<MutableList<Song>>()
    songs.sortedBy { it.durationMs }.forEach { song ->
        val cluster = clusters.lastOrNull()
        if (cluster != null && song.durationMs - cluster.last().durationMs <= DUPLICATE_DURATION_TOLERANCE_MS) cluster.add(song)
        else clusters.add(mutableListOf(song))
    }
    return clusters
}

@Composable
fun DuplicatesScreen(libraryViewModel: LibraryViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val context = LocalContext.current
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val songs = (uiState as? LibraryUiState.Loaded)?.songs.orEmpty()
    val groups = remember(songs) { findDuplicateGroups(songs) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var songPendingConsentRetry by remember { mutableStateOf<Song?>(null) }
    val deleteRecoveryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            songPendingConsentRetry?.let { retryDeleteAfterConsent(context, it) }
        }
        songPendingConsentRetry = null
        libraryViewModel.refresh()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
            Text(stringResource(AppR.string.duplicates_title), style = MaterialTheme.typography.headlineSmall)
        }
        if (groups.isEmpty()) {
            Text(
                stringResource(AppR.string.duplicates_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = listBottomPadding + 32.dp)) {
                items(groups, key = { it.first().id }) { group ->
                    SettingsSection(title = "${group.first().title} · ${group.first().artist}") {
                        group.forEachIndexed { index, song ->
                            if (index > 0) SettingsDivider()
                            ActionRow(
                                title = song.filePath?.substringAfterLast('/') ?: song.title,
                                subtitle = listOfNotNull(
                                    song.filePath?.substringBeforeLast('/', ""),
                                    song.bitrateKbps.takeIf { it > 0 }?.let { stringResource(AppR.string.duplicates_bitrate, it) }
                                ).filter { it.isNotEmpty() }.joinToString(" · "),
                                iconRes = R.drawable.lucide_ic_trash_2,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                onClick = { songToDelete = song }
                            )
                        }
                    }
                }
            }
        }
    }

    songToDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text(stringResource(AppR.string.duplicates_delete_title)) },
            text = { Text(stringResource(AppR.string.duplicates_delete_message, song.filePath ?: song.title)) },
            confirmButton = {
                TextButton(onClick = {
                    songToDelete = null
                    deleteSongFile(context, song, deleteRecoveryLauncher) { pending -> songPendingConsentRetry = pending }
                    libraryViewModel.refresh()
                }) {
                    Text(stringResource(AppR.string.action_delete), color = MaterialTheme.colorScheme.tertiary)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        )
    }
}
