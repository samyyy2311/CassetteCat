package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.download.DEFAULT_MAX_CACHE_BYTES
import `in`.caffeinelabs.cassettecat.data.download.DOWNLOAD_CACHE_LIMIT_OPTIONS_MB
import `in`.caffeinelabs.cassettecat.data.download.DownloadSettingsRepository
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticToggle
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.launch

private val downloadLimitOptions = DOWNLOAD_CACHE_LIMIT_OPTIONS_MB.map { it * 1024L * 1024 }

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun DownloadsScreen(libraryViewModel: LibraryViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val downloadSettingsRepository = remember { DownloadSettingsRepository(context) }
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val downloads by downloadRepository.downloads.collectAsStateWithLifecycle()
    val maxCacheBytes by downloadSettingsRepository.maxCacheBytes.collectAsStateWithLifecycle(initialValue = DEFAULT_MAX_CACHE_BYTES)
    val appPreferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val librarySongs = (libraryState as? LibraryUiState.Loaded)?.songs.orEmpty()

    val trackedDownloads = remember(downloads, librarySongs) {
        downloads.values.mapNotNull { download ->
            librarySongs.find { it.id == download.request.id }?.let { it to download }
        }
    }
    val completed = trackedDownloads.filter { (_, download) -> download.state == Download.STATE_COMPLETED }
    val activeDownloads = trackedDownloads.filter { (_, download) ->
        download.state == Download.STATE_QUEUED ||
            download.state == Download.STATE_DOWNLOADING ||
            download.state == Download.STATE_RESTARTING
    }
    val favoritesRepository = remember { FavoritesRepository(context) }
    val favoriteIds by favoritesRepository.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val undownloadedLiked = remember(librarySongs, favoriteIds, downloads) {
        librarySongs.filter {
            (it.isFavorite || it.id in favoriteIds) &&
                it.source != MusicSource.Local &&
                downloads[it.id]?.state != Download.STATE_COMPLETED
        }
    }
    val totalBytes = completed.sumOf { it.second.bytesDownloaded }
    var showRemoveAllConfirm by remember { mutableStateOf(false) }
    var showRemoveOldestConfirm by remember { mutableStateOf(false) }
    var showStorageLimitPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = stringResource(AppR.string.action_back),
                onClick = onBack
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(AppR.string.downloads_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(
                        AppR.string.downloads_storage_used,
                        formatBytes(totalBytes),
                        formatBytes(maxCacheBytes)
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (completed.isNotEmpty()) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_trash_2,
                    contentDescription = stringResource(AppR.string.downloads_remove_all_description),
                    onClick = { showRemoveAllConfirm = true }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tapScale {
                    val next = !appPreferences.wifiOnlyDownloads
                    scope.launch { appPreferencesRepository.setWifiOnlyDownloads(next) }
                }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(AppR.string.downloads_wifi_only), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(AppR.string.downloads_wifi_only_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = appPreferences.wifiOnlyDownloads,
                onCheckedChange = hapticToggle { enabled ->
                    scope.launch { appPreferencesRepository.setWifiOnlyDownloads(enabled) }
                },
                colors = appSwitchColors()
            )
        }

        val autoDownloadFavorites by downloadSettingsRepository.autoDownloadFavorites.collectAsStateWithLifecycle(initialValue = false)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tapScale {
                    val next = !autoDownloadFavorites
                    scope.launch { downloadSettingsRepository.setAutoDownloadFavorites(next) }
                }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(AppR.string.downloads_auto_favorites), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(AppR.string.downloads_auto_favorites_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = autoDownloadFavorites,
                onCheckedChange = hapticToggle { enabled ->
                    scope.launch { downloadSettingsRepository.setAutoDownloadFavorites(enabled) }
                },
                colors = appSwitchColors()
            )
        }

        if (undownloadedLiked.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tapScale { undownloadedLiked.forEach(downloadRepository::download) }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.downloads_liked_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        pluralStringResource(
                            AppR.plurals.downloads_liked_available,
                            undownloadedLiked.size,
                            undownloadedLiked.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_download),
                    contentDescription = stringResource(AppR.string.downloads_liked_description),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if (completed.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tapScale { showRemoveOldestConfirm = true }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.downloads_remove_oldest), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(AppR.string.downloads_remove_oldest_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_trash_2),
                    contentDescription = stringResource(AppR.string.downloads_remove_oldest),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tapScale { showStorageLimitPicker = true }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(AppR.string.downloads_storage_limit), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(AppR.string.downloads_storage_limit_description, formatBytes(maxCacheBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(R.drawable.lucide_ic_chevron_right),
                contentDescription = stringResource(AppR.string.downloads_choose_storage_limit),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trackedDownloads.isEmpty()) {
            EmptyState(
                catRes = AppR.drawable.cat_calico_player,
                title = stringResource(AppR.string.downloads_empty_title),
                message = stringResource(AppR.string.downloads_empty_message),
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (activeDownloads.isNotEmpty()) {
                    item { DownloadSectionHeader(stringResource(AppR.string.downloads_in_progress)) }
                    items(activeDownloads, key = { (song, _) -> song.id }) { (song, download) ->
                        ActiveDownloadRow(song = song, download = download, onRemove = { downloadRepository.remove(song.id) })
                    }
                }
                if (completed.isNotEmpty()) {
                    item { DownloadSectionHeader(stringResource(AppR.string.downloads_downloaded)) }
                }
                items(completed, key = { (song, _) -> song.id }) { (song, download) ->
                    DownloadedSongRow(
                        song = song,
                        bytes = download.bytesDownloaded,
                        onRemove = { downloadRepository.remove(song.id) }
                    )
                }
            }
        }
    }

    if (showRemoveAllConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveAllConfirm = false },
            title = { Text(stringResource(AppR.string.downloads_remove_all_title)) },
            text = { Text(stringResource(AppR.string.downloads_remove_all_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveAllConfirm = false
                    completed.forEach { (song, _) -> downloadRepository.remove(song.id) }
                }) {
                    Text(stringResource(AppR.string.downloads_remove_all_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAllConfirm = false }) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        )
    }

    if (showRemoveOldestConfirm) {
        val oldest = completed.minByOrNull { (_, download) -> download.updateTimeMs }
        AlertDialog(
            onDismissRequest = { showRemoveOldestConfirm = false },
            title = { Text(stringResource(AppR.string.downloads_remove_oldest_title)) },
            text = {
                Text(
                    stringResource(
                        AppR.string.downloads_remove_oldest_message,
                        oldest?.first?.title.orEmpty()
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    oldest?.let { (song, _) -> downloadRepository.remove(song.id) }
                    showRemoveOldestConfirm = false
                }) {
                    Text(stringResource(AppR.string.action_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveOldestConfirm = false }) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        )
    }

    if (showStorageLimitPicker) {
        ModalBottomSheet(
            onDismissRequest = { showStorageLimitPicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
                Text(stringResource(AppR.string.downloads_limit_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(AppR.string.downloads_limit_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
                downloadLimitOptions.forEach { limit ->
                    val selected = limit == maxCacheBytes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .border(
                                if (selected) 1.dp else 0.5.dp,
                                if (selected) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(16.dp)
                            )
                            .tapScale {
                                scope.launch { downloadSettingsRepository.setMaxCacheBytes(limit) }
                                showStorageLimitPicker = false
                            }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                formatBytes(limit),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(
                                    if (selected) AppR.string.downloads_current_limit
                                    else AppR.string.downloads_offline_storage
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_check),
                                contentDescription = stringResource(AppR.string.downloads_selected),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    stringResource(AppR.string.downloads_limit_restart_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadSectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ActiveDownloadRow(song: Song, download: Download, onRemove: () -> Unit) {
    val progress = (download.percentDownloaded / 100f).takeIf { it in 0f..1f }
    val status = when (download.state) {
        Download.STATE_DOWNLOADING -> progress?.let {
            stringResource(AppR.string.downloads_status_downloading_percent, (it * 100).toInt())
        } ?: stringResource(AppR.string.downloads_status_downloading)
        Download.STATE_RESTARTING -> stringResource(AppR.string.downloads_status_restarting)
        else -> stringResource(AppR.string.downloads_status_queued)
    }
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
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_x,
            contentDescription = stringResource(AppR.string.downloads_cancel_download),
            onClick = onRemove
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun DownloadedSongRow(song: Song, bytes: Long, onRemove: () -> Unit) {
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
            Text(
                stringResource(AppR.string.downloads_song_storage, song.artist, formatBytes(bytes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_trash_2,
            contentDescription = stringResource(AppR.string.downloads_remove_download),
            onClick = onRemove
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.0f MB".format(bytes / (1024.0 * 1024))
    else -> "%.0f KB".format(bytes / 1024.0)
}
