package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import android.text.format.DateFormat
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterMode
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomRole
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import `in`.caffeinelabs.cassettecat.data.update.UpdateCheckResult
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.ListeningRoomSheet
import `in`.caffeinelabs.cassettecat.ui.util.hapticToggle
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.util.Date

val externalServices = ExternalService.entries.filter { it != ExternalService.GITHUB_UPDATES }

@Composable
fun SettingsScreen(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    onConnectServer: (StreamingProtocol) -> Unit,
    onNavigateToStats: () -> Unit,
    onManageScanFolders: () -> Unit,
    onManageExternalServices: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSleepTimer: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCustomization: () -> Unit = {},
    onNavigateToPairing: () -> Unit = {},
    onNavigateToAboutLegal: () -> Unit = {},
    onNavigateToCredits: () -> Unit = {},
    onNavigateToScrobbling: () -> Unit = {},
    listBottomPadding: Dp = 0.dp,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateCheckResult by viewModel.updateCheckResult.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedApk by viewModel.downloadedApk.collectAsStateWithLifecycle()
    val listeningRoom by playbackViewModel.listeningRoom.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val lastRefreshAtMs by libraryViewModel.lastRefreshAtMs.collectAsStateWithLifecycle()
    var showListeningRoom by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        val enabledServices = externalServices.count { uiState.services.isEnabled(it) }

        SettingsHeader()
        Spacer(Modifier.height(12.dp))

        SettingsSection(title = stringResource(AppR.string.settings_audio_playback_section)) {
            NavigationRow(
                title = stringResource(AppR.string.settings_customisation),
                subtitle = stringResource(AppR.string.settings_customisation_description),
                iconRes = R.drawable.lucide_ic_palette,
                iconTint = Color(0xFF38BDF8),
                onClick = onNavigateToCustomization
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_sleep_timer),
                subtitle = stringResource(AppR.string.settings_sleep_timer_description),
                iconRes = R.drawable.lucide_ic_moon,
                iconTint = Color(0xFFA5B4FC),
                onClick = onNavigateToSleepTimer
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_equalizer),
                subtitle = stringResource(AppR.string.settings_equalizer_description),
                iconRes = R.drawable.lucide_ic_sliders_horizontal,
                iconTint = Color(0xFFF59E0B),
                onClick = onNavigateToEqualizer
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = stringResource(AppR.string.settings_library_hardware_section)) {
            NavigationRow(
                title = stringResource(AppR.string.settings_listening_record),
                subtitle = stringResource(AppR.string.settings_listening_record_description),
                iconRes = R.drawable.lucide_ic_disc_3,
                iconTint = Color(0xFFC23B30),
                onClick = onNavigateToStats
            )
            SettingsDivider()
            val folderCount = uiState.folderFilter.folders.size
            NavigationRow(
                title = stringResource(AppR.string.settings_scan_folders),
                subtitle = when (uiState.folderFilter.mode) {
                    FolderFilterMode.NONE -> stringResource(AppR.string.settings_scan_all_music)
                    FolderFilterMode.WHITELIST -> pluralStringResource(
                        AppR.plurals.settings_scan_folders_included,
                        folderCount,
                        folderCount
                    )
                    FolderFilterMode.BLACKLIST -> pluralStringResource(
                        AppR.plurals.settings_scan_folders_excluded,
                        folderCount,
                        folderCount
                    )
                },
                iconRes = R.drawable.lucide_ic_folder,
                iconTint = Color(0xFFC4C4C0),
                onClick = onManageScanFolders
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.downloads_title),
                subtitle = stringResource(AppR.string.settings_downloads_description),
                iconRes = R.drawable.lucide_ic_download,
                iconTint = Color(0xFF38BDF8),
                onClick = onNavigateToDownloads
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_player_name),
                subtitle = stringResource(AppR.string.settings_player_description),
                iconRes = R.drawable.lucide_ic_cassette_tape,
                iconTint = Color(0xFFF4F4F5),
                onClick = onNavigateToPairing
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = stringResource(AppR.string.settings_streaming_services_section)) {
            ToggleRow(
                title = stringResource(AppR.string.settings_offline_blackout),
                subtitle = stringResource(
                    if (uiState.services.offlineBlackoutMode) {
                        AppR.string.settings_offline_blackout_enabled
                    } else {
                        AppR.string.settings_offline_blackout_disabled
                    }
                ),
                checked = uiState.services.offlineBlackoutMode,
                onCheckedChange = { viewModel.setOfflineBlackoutMode(it) },
                iconRes = R.drawable.lucide_ic_radio,
                iconTint = if (uiState.services.offlineBlackoutMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
            )
            SettingsDivider()
            ServerRow(
                title = stringResource(AppR.string.settings_subsonic_name),
                subtitle = stringResource(AppR.string.settings_subsonic_description),
                config = uiState.subsonic,
                status = serverStatus(
                    config = uiState.subsonic,
                    source = MusicSource.Subsonic,
                    warningLabel = "Subsonic",
                    offline = uiState.services.offlineBlackoutMode,
                    libraryState = libraryState,
                    lastRefreshAtMs = lastRefreshAtMs
                ),
                isChecking = !uiState.services.offlineBlackoutMode && libraryState is LibraryUiState.Loading,
                onRetry = libraryViewModel::refresh,
                iconRes = AppR.drawable.ic_logo_subsonic,
                iconTint = Color.Unspecified,
                onConnect = { onConnectServer(StreamingProtocol.SUBSONIC) },
                onDisconnect = { viewModel.disconnect(StreamingProtocol.SUBSONIC) }
            )
            SettingsDivider()
            ServerRow(
                title = stringResource(AppR.string.settings_jellyfin_name),
                subtitle = stringResource(AppR.string.settings_jellyfin_description),
                config = uiState.jellyfin,
                status = serverStatus(
                    config = uiState.jellyfin,
                    source = MusicSource.Jellyfin,
                    warningLabel = "Jellyfin",
                    offline = uiState.services.offlineBlackoutMode,
                    libraryState = libraryState,
                    lastRefreshAtMs = lastRefreshAtMs
                ),
                isChecking = !uiState.services.offlineBlackoutMode && libraryState is LibraryUiState.Loading,
                onRetry = libraryViewModel::refresh,
                iconRes = AppR.drawable.ic_logo_jellyfin,
                iconTint = Color.Unspecified,
                onConnect = { onConnectServer(StreamingProtocol.JELLYFIN) },
                onDisconnect = { viewModel.disconnect(StreamingProtocol.JELLYFIN) }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_external_services),
                subtitle = stringResource(
                    AppR.string.settings_external_services_count,
                    enabledServices,
                    externalServices.size
                ),
                iconRes = R.drawable.lucide_ic_globe,
                iconTint = Color(0xFF38BDF8),
                onClick = onManageExternalServices
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.scrobbling_title),
                subtitle = stringResource(AppR.string.settings_scrobbling_description),
                iconRes = AppR.drawable.ic_logo_listenbrainz,
                iconTint = Color.Unspecified,
                onClick = onNavigateToScrobbling
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_listening_room),
                subtitle = listeningRoomStatus(listeningRoom),
                iconRes = R.drawable.lucide_ic_users,
                iconTint = Color(0xFFC23B30),
                onClick = { showListeningRoom = true }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = stringResource(AppR.string.settings_data_updates_section)) {
            NavigationRow(
                title = stringResource(AppR.string.settings_privacy_security),
                subtitle = stringResource(AppR.string.settings_privacy_security_description),
                iconRes = R.drawable.lucide_ic_shield,
                iconTint = Color(0xFF10B981),
                onClick = onNavigateToPrivacy
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_backup_restore),
                subtitle = stringResource(AppR.string.settings_backup_restore_description),
                iconRes = R.drawable.lucide_ic_archive_restore,
                iconTint = Color(0xFF60A5FA),
                onClick = onNavigateToBackupRestore
            )
            SettingsDivider()
            ServiceToggleRow(
                service = ExternalService.GITHUB_UPDATES,
                enabled = uiState.services.githubUpdatesEnabled,
                isBlackedOut = uiState.services.offlineBlackoutMode,
                onToggle = { enabled -> viewModel.setServiceEnabled(ExternalService.GITHUB_UPDATES, enabled) },
                iconRes = AppR.drawable.ic_logo_github,
                iconTint = Color.Unspecified
            )
            if (uiState.services.isEnabled(ExternalService.GITHUB_UPDATES)) {
                SettingsDivider(startPadding = 24.dp)
                UpdateCheckRow(
                    result = updateCheckResult,
                    downloadProgress = downloadProgress,
                    downloadedApk = downloadedApk,
                    checkEnabled = true,
                    onCheck = { viewModel.checkForUpdate() },
                    onDownloadAndInstall = { available -> viewModel.downloadAndInstallUpdate(context, available) },
                    onInstall = { viewModel.installDownloadedApk(context) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = stringResource(AppR.string.settings_support_about_section)) {
            NavigationRow(
                title = stringResource(AppR.string.settings_about_legal),
                subtitle = stringResource(AppR.string.settings_about_legal_description),
                iconRes = R.drawable.lucide_ic_file_text,
                iconTint = Color(0xFF38BDF8),
                onClick = onNavigateToAboutLegal
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_credits),
                subtitle = stringResource(AppR.string.settings_credits_description),
                iconRes = R.drawable.lucide_ic_heart,
                iconTint = Color(0xFFC23B30),
                onClick = onNavigateToCredits
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_github_sponsors),
                subtitle = stringResource(AppR.string.settings_github_sponsors_description),
                iconRes = AppR.drawable.ic_logo_github,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/sponsors/samyyy2311") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_kofi),
                subtitle = stringResource(AppR.string.settings_kofi_description),
                iconRes = AppR.drawable.ic_logo_kofi,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://ko-fi.com/samyyy2311") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.settings_buy_me_a_coffee),
                subtitle = stringResource(AppR.string.settings_buy_me_a_coffee_description),
                iconRes = AppR.drawable.ic_logo_buymeacoffee,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://buymeacoffee.com/samyyy2311") }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(AppR.string.settings_sponsor_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(listBottomPadding + 24.dp))
    }

    if (showListeningRoom) {
        ListeningRoomSheet(
            state = listeningRoom,
            onStart = playbackViewModel::startListeningRoom,
            onFindNearby = playbackViewModel::findNearbyListeningRooms,
            onJoin = playbackViewModel::joinListeningRoom,
            onJoinManual = playbackViewModel::joinListeningRoomManually,
            onLeave = playbackViewModel::leaveListeningRoom,
            onDismiss = {
                showListeningRoom = false
                playbackViewModel.stopFindingNearbyListeningRooms()
            }
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Text(stringResource(AppR.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(AppR.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun serverStatus(
    config: StreamingServerConfig,
    source: MusicSource,
    warningLabel: String,
    offline: Boolean,
    libraryState: LibraryUiState,
    lastRefreshAtMs: Long?
): String {
    if (offline) return stringResource(AppR.string.settings_server_paused)
    if (libraryState is LibraryUiState.Loading) return stringResource(AppR.string.settings_server_checking)

    val context = LocalContext.current
    val loaded = libraryState as? LibraryUiState.Loaded
    val warning = loaded?.sourceWarnings?.firstOrNull { it.startsWith("$warningLabel:") }
    val checkedAt = lastRefreshAtMs?.let { DateFormat.getTimeFormat(context).format(Date(it)) }

    if (warning != null) {
        val message = warning.substringAfter(':').trim()
        return if (checkedAt != null) {
            stringResource(AppR.string.settings_server_unavailable_checked, message, checkedAt)
        } else {
            stringResource(AppR.string.settings_server_unavailable, message)
        }
    }

    val count = loaded?.songs?.count { it.source == source } ?: 0
    return if (checkedAt != null) {
        pluralStringResource(
            AppR.plurals.settings_server_songs_refreshed,
            count,
            config.username,
            count,
            checkedAt
        )
    } else {
        pluralStringResource(
            AppR.plurals.settings_server_songs,
            count,
            config.username,
            count
        )
    }
}

@Composable
private fun listeningRoomStatus(state: ListeningRoomState): String = when (state.role) {
    ListeningRoomRole.HOST -> pluralStringResource(
        AppR.plurals.settings_listening_room_hosting,
        state.participantCount,
        state.participantCount
    )
    ListeningRoomRole.GUEST -> state.roomName?.let {
        stringResource(AppR.string.settings_listening_room_following, it)
    } ?: stringResource(AppR.string.settings_listening_room_following_room)
    ListeningRoomRole.NONE -> stringResource(AppR.string.settings_listening_room_inactive)
}

@Composable
fun ServiceToggleRow(
    service: ExternalService,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isBlackedOut: Boolean = false,
    iconRes: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.secondary
) {
    val onSwitchToggle = hapticToggle(onToggle)
    val description = serviceDescription(service)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale { onToggle(!enabled) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isBlackedOut) iconTint.copy(alpha = 0.5f) else iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                serviceLabel(service),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isBlackedOut) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (isBlackedOut) stringResource(AppR.string.settings_service_paused, description) else description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isBlackedOut) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onSwitchToggle,
            colors = appSwitchColors()
        )
    }
}

@Composable
private fun serviceLabel(service: ExternalService): String = stringResource(
    when (service) {
        ExternalService.DEEZER -> AppR.string.settings_service_deezer
        ExternalService.AUDIODB -> AppR.string.settings_service_audiodb
        ExternalService.LRCLIB -> AppR.string.settings_service_lrclib
        ExternalService.COVER_ART_ARCHIVE -> AppR.string.settings_service_cover_art_archive
        ExternalService.WIKIPEDIA -> AppR.string.settings_service_wikipedia
        ExternalService.GITHUB_UPDATES -> AppR.string.settings_service_github
        ExternalService.RADIO_BROWSER -> AppR.string.settings_service_radio_browser
    }
)

@Composable
private fun serviceDescription(service: ExternalService): String = stringResource(
    when (service) {
        ExternalService.DEEZER -> AppR.string.settings_service_deezer_description
        ExternalService.AUDIODB -> AppR.string.settings_service_audiodb_description
        ExternalService.LRCLIB -> AppR.string.settings_service_lrclib_description
        ExternalService.COVER_ART_ARCHIVE -> AppR.string.settings_service_cover_art_archive_description
        ExternalService.WIKIPEDIA -> AppR.string.settings_service_wikipedia_description
        ExternalService.GITHUB_UPDATES -> AppR.string.settings_service_github_description
        ExternalService.RADIO_BROWSER -> AppR.string.settings_service_radio_browser_description
    }
)

@Composable
private fun UpdateCheckRow(
    result: UpdateCheckResult?,
    downloadProgress: Float?,
    downloadedApk: File?,
    checkEnabled: Boolean,
    onCheck: () -> Unit,
    onDownloadAndInstall: (UpdateCheckResult.UpdateAvailable) -> Unit,
    onInstall: () -> Unit
) {
    val context = LocalContext.current
    val available = result as? UpdateCheckResult.UpdateAvailable
    val isDownloading = downloadProgress != null
    val isDownloaded = downloadedApk != null && downloadedApk.exists()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (checkEnabled && !isDownloading) {
                    Modifier.tapScale {
                        when {
                            isDownloaded -> onInstall()
                            available != null -> {
                                if (available.downloadUrl != null) {
                                    onDownloadAndInstall(available)
                                } else {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, available.url.toUri()))
                                }
                            }
                            else -> onCheck()
                        }
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                when {
                    isDownloaded -> R.drawable.lucide_ic_download
                    isDownloading -> R.drawable.lucide_ic_refresh_cw
                    else -> R.drawable.lucide_ic_refresh_cw
                }
            ),
            contentDescription = null,
            tint = if (isDownloaded || available != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(AppR.string.settings_check_updates), style = MaterialTheme.typography.bodyLarge)
            Text(
                when {
                    !checkEnabled -> stringResource(AppR.string.settings_updates_disabled)
                    isDownloading -> stringResource(AppR.string.settings_updates_downloading, ((downloadProgress ?: 0f) * 100).toInt())
                    isDownloaded -> stringResource(AppR.string.settings_updates_ready)
                    result == null -> stringResource(AppR.string.settings_updates_tap)
                    result is UpdateCheckResult.UpToDate -> stringResource(AppR.string.settings_updates_current)
                    available != null -> {
                        if (available.downloadUrl != null) {
                            stringResource(AppR.string.settings_updates_available_download, available.version)
                        } else {
                            stringResource(AppR.string.settings_updates_available, available.version)
                        }
                    }
                    else -> stringResource(AppR.string.settings_updates_failed)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress ?: 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
        if (available != null && !isDownloading) {
            Icon(
                painter = painterResource(
                    if (isDownloaded || available.downloadUrl != null) R.drawable.lucide_ic_download else R.drawable.lucide_ic_chevron_right
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
