package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterMode
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import `in`.caffeinelabs.cassettecat.data.update.UpdateCheckResult
import `in`.caffeinelabs.cassettecat.data.listeningroom.statusSubtitle
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.ListeningRoomSheet
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.hapticToggle
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily

val externalServices = ExternalService.entries.filter { it != ExternalService.GITHUB_UPDATES }

@Composable
fun SettingsScreen(
    playbackViewModel: PlaybackViewModel,
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
    val uiState by viewModel.uiState.collectAsState()
    val updateCheckResult by viewModel.updateCheckResult.collectAsState()
    val listeningRoom by playbackViewModel.listeningRoom.collectAsState()
    var showListeningRoom by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        val enabledServices = externalServices.count { uiState.services.isEnabled(it) }

        SettingsHeader()
        Spacer(Modifier.height(10.dp))

        // 1. Audio & Playback
        SettingsSection(title = "Audio & Playback") {
            NavigationRow(
                title = "Customisation",
                subtitle = "Startup tab, home feed, audio, and display",
                iconRes = R.drawable.lucide_ic_palette,
                iconTint = Color(0xFF38BDF8),
                onClick = onNavigateToCustomization
            )
            SettingsDivider()
            NavigationRow(
                title = "Sleep timer",
                subtitle = "Stop playback after a set time",
                iconRes = R.drawable.lucide_ic_moon,
                iconTint = Color(0xFFA5B4FC),
                onClick = onNavigateToSleepTimer
            )
            SettingsDivider()
            NavigationRow(
                title = "Equalizer",
                subtitle = "Tune the current audio output",
                iconRes = R.drawable.lucide_ic_sliders_horizontal,
                iconTint = Color(0xFFF59E0B),
                onClick = onNavigateToEqualizer
            )
        }

        Spacer(Modifier.height(20.dp))

        // 2. Library & Hardware
        SettingsSection(title = "Library & Hardware") {
            NavigationRow(
                title = "Listening Record",
                subtitle = "Monthly stats, artists, and repeats",
                iconRes = R.drawable.lucide_ic_disc_3,
                iconTint = Color(0xFFC23B30),
                onClick = onNavigateToStats
            )
            SettingsDivider()
            NavigationRow(
                title = "Scan Folders",
                subtitle = when (uiState.folderFilter.mode) {
                    FolderFilterMode.NONE -> "All music"
                    FolderFilterMode.WHITELIST -> "${uiState.folderFilter.folders.size} folder(s) included"
                    FolderFilterMode.BLACKLIST -> "${uiState.folderFilter.folders.size} folder(s) excluded"
                },
                iconRes = R.drawable.lucide_ic_folder,
                iconTint = Color(0xFFC4C4C0),
                onClick = onManageScanFolders
            )
            SettingsDivider()
            NavigationRow(
                title = "Downloads",
                subtitle = "Manage songs saved for offline playback",
                iconRes = R.drawable.lucide_ic_download,
                iconTint = Color(0xFF38BDF8),
                onClick = onNavigateToDownloads
            )
            SettingsDivider()
            NavigationRow(
                title = "CassetteCat Player",
                subtitle = "Pair, sync music, and hardware telemetry",
                iconRes = R.drawable.lucide_ic_cassette_tape,
                iconTint = Color(0xFFF4F4F5),
                onClick = onNavigateToPairing
            )
        }

        Spacer(Modifier.height(20.dp))

        // 3. Streaming & Services
        SettingsSection(title = "Streaming & Services") {
            ToggleRow(
                title = "Offline Blackout Mode",
                subtitle = if (uiState.services.offlineBlackoutMode) "All network services and streaming disabled" else "Disable all network calls, streaming, and lookups",
                checked = uiState.services.offlineBlackoutMode,
                onCheckedChange = { viewModel.setOfflineBlackoutMode(it) },
                iconRes = R.drawable.lucide_ic_radio,
                iconTint = if (uiState.services.offlineBlackoutMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
            )
            SettingsDivider()
            ServerRow(
                title = "Subsonic",
                subtitle = "Navidrome, gonic, and other Subsonic servers",
                config = uiState.subsonic,
                iconRes = AppR.drawable.ic_logo_subsonic,
                iconTint = Color.Unspecified,
                onConnect = { onConnectServer(StreamingProtocol.SUBSONIC) },
                onDisconnect = { viewModel.disconnect(StreamingProtocol.SUBSONIC) }
            )
            SettingsDivider()
            ServerRow(
                title = "Jellyfin",
                subtitle = "Connect to a Jellyfin media server",
                config = uiState.jellyfin,
                iconRes = AppR.drawable.ic_logo_jellyfin,
                iconTint = Color.Unspecified,
                onConnect = { onConnectServer(StreamingProtocol.JELLYFIN) },
                onDisconnect = { viewModel.disconnect(StreamingProtocol.JELLYFIN) }
            )
            SettingsDivider()
            NavigationRow(
                title = "External Services",
                subtitle = "$enabledServices of ${externalServices.size} enabled (Lyrics, Metadata, Radio)",
                iconRes = R.drawable.lucide_ic_globe,
                iconTint = Color(0xFF38BDF8),
                onClick = onManageExternalServices
            )
            SettingsDivider()
            NavigationRow(
                title = "Scrobbling",
                subtitle = "ListenBrainz and Libre.fm listening sync",
                iconRes = AppR.drawable.ic_logo_listenbrainz,
                iconTint = Color.Unspecified,
                onClick = onNavigateToScrobbling
            )
            SettingsDivider()
            NavigationRow(
                title = "Listening Room",
                subtitle = listeningRoom.statusSubtitle(),
                iconRes = R.drawable.lucide_ic_users,
                iconTint = Color(0xFFC23B30),
                onClick = { showListeningRoom = true }
            )
        }

        Spacer(Modifier.height(20.dp))

        // 4. Data & App Updates
        SettingsSection(title = "Data & Updates") {
            NavigationRow(
                title = "Privacy & Security",
                subtitle = "Listening data and saved server credentials",
                iconRes = R.drawable.lucide_ic_shield,
                iconTint = Color(0xFF10B981),
                onClick = onNavigateToPrivacy
            )
            SettingsDivider()
            NavigationRow(
                title = "Backup & Restore",
                subtitle = "Keep library, playlists, and settings safe",
                iconRes = R.drawable.lucide_ic_archive_restore,
                iconTint = Color(0xFF60A5FA),
                onClick = onNavigateToBackupRestore
            )
            SettingsDivider()
            ServiceToggleRow(
                service = ExternalService.GITHUB_UPDATES,
                enabled = uiState.services.githubUpdatesEnabled,
                onToggle = { enabled -> viewModel.setServiceEnabled(ExternalService.GITHUB_UPDATES, enabled) },
                iconRes = AppR.drawable.ic_logo_github,
                iconTint = Color.Unspecified
            )
            if (uiState.services.githubUpdatesEnabled) {
                SettingsDivider(startPadding = 24.dp)
                UpdateCheckRow(
                    result = updateCheckResult,
                    checkEnabled = true,
                    onCheck = { viewModel.checkForUpdate() }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 5. Support & About (Clean, consistent styling)
        SettingsSection(title = "Support & About") {
            NavigationRow(
                title = "About & Legal",
                subtitle = "Audio DSP, data sources, privacy & permissions",
                iconRes = R.drawable.lucide_ic_file_text,
                iconTint = Color(0xFF38BDF8),
                onClick = onNavigateToAboutLegal
            )
            SettingsDivider()
            NavigationRow(
                title = "Credits & Attribution",
                subtitle = "Contributors, open source libraries, and fonts",
                iconRes = R.drawable.lucide_ic_heart,
                iconTint = Color(0xFFC23B30),
                onClick = onNavigateToCredits
            )
            SettingsDivider()
            NavigationRow(
                title = "Ko-fi",
                subtitle = "Support development on Ko-fi",
                iconRes = AppR.drawable.ic_logo_kofi,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://ko-fi.com/samyyy2311") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Buy Me a Coffee",
                subtitle = "Support Samarth on Buy Me a Coffee",
                iconRes = AppR.drawable.ic_logo_buymeacoffee,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://buymeacoffee.com/samyyy2311") }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "20% of sponsorship proceeds fund free lyrics infrastructure on LRCLIB.",
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
            onDismiss = { showListeningRoom = false }
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Manage playback, library, hardware, services, and data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ServiceToggleRow(
    service: ExternalService,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    iconRes: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.secondary
) {
    val onSwitchToggle = hapticToggle(onToggle)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale { onToggle(!enabled) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(service.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                service.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onSwitchToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                checkedBorderColor = MaterialTheme.colorScheme.tertiary
            )
        )
    }
}

@Composable
private fun UpdateCheckRow(result: UpdateCheckResult?, checkEnabled: Boolean, onCheck: () -> Unit) {
    val context = LocalContext.current
    val available = result as? UpdateCheckResult.UpdateAvailable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (checkEnabled) {
                    Modifier.tapScale {
                        if (available != null) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, available.url.toUri()))
                        } else {
                            onCheck()
                        }
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_refresh_cw),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Check for Updates", style = MaterialTheme.typography.bodyLarge)
            Text(
                when {
                    !checkEnabled -> "Disabled above"
                    result == null -> "Tap to check"
                    result is UpdateCheckResult.UpToDate -> "You're up to date"
                    result is UpdateCheckResult.UpdateAvailable -> "Version ${result.version} available, tap to view"
                    else -> "Couldn't check, tap to retry"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (available != null) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
