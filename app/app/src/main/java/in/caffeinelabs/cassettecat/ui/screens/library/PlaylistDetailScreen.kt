package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.buildM3u
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

private enum class PlaylistSortOrder(val label: String) {
    PLAYLIST_ORDER("Playlist Order"), TITLE("Title"), ARTIST("Artist"), ALBUM("Album")
}

private fun List<Song>.sortedForPlaylist(order: PlaylistSortOrder, direction: SortDirection): List<Song> {
    val base = when (order) {
        PlaylistSortOrder.PLAYLIST_ORDER -> this
        PlaylistSortOrder.TITLE -> sortedWith(SongSortOrder.TITLE.comparator())
        PlaylistSortOrder.ARTIST -> sortedWith(SongSortOrder.ARTIST.comparator())
        PlaylistSortOrder.ALBUM -> sortedWith(SongSortOrder.ALBUM.comparator())
    }
    return if (direction == SortDirection.DESCENDING) base.reversed() else base
}

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    libraryViewModel: LibraryViewModel,
    playlistViewModel: PlaylistViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val libraryState by libraryViewModel.uiState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val allSongs = (libraryState as? LibraryUiState.Loaded)?.songs.orEmpty()
    val playlist = playlists.find { it.id == playlistId }
        // transiently null right after delete, mid pop-back-stack transition
        ?: return

    val songs = remember(playlist.songIds, allSongs) {
        playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
    }

    var sortOrder by rememberSaveable { mutableStateOf(PlaylistSortOrder.PLAYLIST_ORDER) }
    var sortDirection by rememberSaveable { mutableStateOf(SortDirection.ASCENDING) }
    val sortedSongs = remember(songs, sortOrder, sortDirection) { songs.sortedForPlaylist(sortOrder, sortDirection) }

    var showActionsSheet by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddSongsSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showCoverPickerSheet by remember { mutableStateOf(false) }
    var skippedExportCount by remember { mutableStateOf<Int?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        if (uri != null) {
            val (text, skipped) = buildM3u(sortedSongs)
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            if (skipped > 0) skippedExportCount = skipped
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (songs.size == 1) "1 song" else "${songs.size} songs",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (songs.isNotEmpty()) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_arrow_up_down,
                    contentDescription = "Sort by",
                    onClick = { showSortSheet = true }
                )
            }
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_ellipsis_vertical,
                contentDescription = "Playlist options",
                onClick = { showActionsSheet = true }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tapScale { showAddSongsSheet = true }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_plus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text("Add Songs", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.tertiary)
        }

        if (songs.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_music,
                title = "No songs yet",
                message = "Tap Add Songs to get started.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = listBottomPadding)) {
                items(sortedSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        onClick = {
                            val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                            val index = sortedSongs.indexOfFirst { it.id == song.id }
                            playbackViewModel.playQueue(sortedSongs, index)
                            if (wasIdle) onNavigateToNowPlaying()
                        }
                    )
                }
            }
        }
    }

    if (showActionsSheet) {
        PlaylistActionsSheet(
            onChangeCover = { showActionsSheet = false; showCoverPickerSheet = true },
            onDownloadAll = {
                showActionsSheet = false
                sortedSongs.filter { it.source != MusicSource.Local }.forEach(downloadRepository::download)
            },
            onExport = {
                showActionsSheet = false
                val safeName = playlist.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                exportLauncher.launch("$safeName.m3u8")
            },
            onRename = { showActionsSheet = false; showRenameSheet = true },
            onDelete = { showActionsSheet = false; showDeleteConfirm = true },
            onDismiss = { showActionsSheet = false }
        )
    }

    val skipped = skippedExportCount
    if (skipped != null) {
        AlertDialog(
            onDismissRequest = { skippedExportCount = null },
            title = { Text("Some songs weren't included") },
            text = { Text(if (skipped == 1) "1 streamed song wasn't included — only local files can be exported." else "$skipped streamed songs weren't included — only local files can be exported.") },
            confirmButton = {
                TextButton(onClick = { skippedExportCount = null }) { Text("OK") }
            }
        )
    }

    if (showCoverPickerSheet) {
        PlaylistCoverPickerSheet(
            playlist = playlist,
            onSetCover = { type, value -> playlistViewModel.setCover(playlist.id, type, value) },
            onClearCover = { playlistViewModel.clearCover(playlist.id) },
            onDismiss = { showCoverPickerSheet = false }
        )
    }

    if (showRenameSheet) {
        PlaylistNameSheet(
            title = "Rename Playlist",
            initialName = playlist.name,
            onConfirm = { name ->
                showRenameSheet = false
                playlistViewModel.rename(playlist.id, name)
            },
            onDismiss = { showRenameSheet = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${playlist.name}\"?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    playlistViewModel.delete(playlist.id)
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddSongsSheet) {
        AddSongsSheet(
            allSongs = allSongs,
            selectedIds = playlist.songIds.toSet(),
            onToggle = { songId, selected ->
                if (selected) playlistViewModel.addSong(playlist.id, songId) else playlistViewModel.removeSong(playlist.id, songId)
            },
            onDismiss = { showAddSongsSheet = false }
        )
    }

    if (showSortSheet) {
        SortOptionsSheet(
            options = PlaylistSortOrder.entries,
            labelOf = { it.label },
            selected = sortOrder,
            direction = sortDirection,
            onSelect = { order ->
                if (order == sortOrder) {
                    sortDirection = sortDirection.flipped()
                } else {
                    sortOrder = order
                    sortDirection = SortDirection.ASCENDING
                }
            },
            onDismiss = { showSortSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistActionsSheet(
    onChangeCover: () -> Unit,
    onDownloadAll: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            PlaylistActionRow(iconRes = R.drawable.lucide_ic_image, label = "Change Cover", destructive = false, onClick = onChangeCover)
            PlaylistActionRow(iconRes = R.drawable.lucide_ic_download, label = "Download All", destructive = false, onClick = onDownloadAll)
            PlaylistActionRow(iconRes = R.drawable.lucide_ic_upload, label = "Export as M3U8", destructive = false, onClick = onExport)
            PlaylistActionRow(iconRes = R.drawable.lucide_ic_pencil, label = "Rename", destructive = false, onClick = onRename)
            PlaylistActionRow(iconRes = R.drawable.lucide_ic_trash_2, label = "Delete", destructive = true, onClick = onDelete)
        }
    }
}

@Composable
private fun PlaylistActionRow(iconRes: Int, label: String, destructive: Boolean, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().tapScale(onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (destructive) tint else MaterialTheme.colorScheme.onSurface)
    }
}

// shared by "New Playlist" (LibraryScreen) and "Rename Playlist" (here) — same field, same shape
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistNameSheet(title: String, initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = hapticClick { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongsSheet(
    allSongs: List<Song>,
    selectedIds: Set<String>,
    onToggle: (songId: String, selected: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredSongs = if (query.isBlank()) {
        allSongs
    } else {
        allSongs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Add Songs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search songs") },
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.lucide_ic_search), contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        PressDepthIconButton(
                            iconRes = R.drawable.lucide_ic_x,
                            contentDescription = "Clear",
                            onClick = { query = "" }
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            LazyColumn {
                items(filteredSongs, key = { it.id }) { song ->
                    SongPickerRow(
                        song = song,
                        selected = song.id in selectedIds,
                        onToggle = { onToggle(song.id, song.id !in selectedIds) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongPickerRow(song: Song, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().tapScale(onToggle).padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            painter = painterResource(if (selected) R.drawable.lucide_ic_circle_check_big else R.drawable.lucide_ic_circle),
            contentDescription = if (selected) "Remove from playlist" else "Add to playlist",
            tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
