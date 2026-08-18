package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterMode
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import `in`.caffeinelabs.cassettecat.data.update.UpdateCheckResult
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily

val externalServices = ExternalService.entries.filter { it != ExternalService.GITHUB_UPDATES }

@Composable
fun SettingsScreen(
    onConnectServer: (StreamingProtocol) -> Unit,
    onNavigateToStats: () -> Unit,
    onManageScanFolders: () -> Unit,
    onManageExternalServices: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSleepTimer: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToPairing: () -> Unit = {},
    onNavigateToCredits: () -> Unit = {},
    onNavigateToScrobbling: () -> Unit = {},
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateCheckResult by viewModel.updateCheckResult.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp)
    ) {
        val enabledServices = externalServices.count { uiState.services.isEnabled(it) }

        SettingsHeader()
        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "Audio") {
            NavigationRow(
                title = "Sleep timer",
                subtitle = "Stop playback after a set time",
                iconRes = R.drawable.lucide_ic_timer,
                onClick = onNavigateToSleepTimer
            )
            SettingsDivider()
            NavigationRow(
                title = "Equalizer",
                subtitle = "Tune the current audio output",
                iconRes = R.drawable.lucide_ic_sliders_horizontal,
                onClick = onNavigateToEqualizer
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = "Hardware") {
            NavigationRow(
                title = "CassetteCat Player",
                subtitle = "Pair, sync music, and check hardware telemetry",
                iconRes = R.drawable.lucide_ic_cassette_tape,
                onClick = onNavigateToPairing
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = "Library") {
            NavigationRow(
                title = "Listening Record",
                subtitle = "Monthly time, favourite artists, and repeats",
                iconRes = R.drawable.lucide_ic_chart_no_axes_combined,
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
                onClick = onManageScanFolders
            )
            SettingsDivider()
            NavigationRow(
                title = "Downloads",
                subtitle = "Manage songs saved for offline playback",
                iconRes = R.drawable.lucide_ic_download,
                onClick = onNavigateToDownloads
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = "Servers & services") {
            ServerRow(
                title = "Subsonic",
                subtitle = "Navidrome, gonic, and other Subsonic-API servers",
                config = uiState.subsonic,
                onConnect = { onConnectServer(StreamingProtocol.SUBSONIC) },
                onDisconnect = { viewModel.disconnect(StreamingProtocol.SUBSONIC) }
            )
            SettingsDivider()
            ServerRow(
                title = "Jellyfin",
                subtitle = "Connect to a Jellyfin media server",
                config = uiState.jellyfin,
                onConnect = { onConnectServer(StreamingProtocol.JELLYFIN) },
                onDisconnect = { viewModel.disconnect(StreamingProtocol.JELLYFIN) }
            )
            SettingsDivider()
            NavigationRow(
                title = "External Services",
                subtitle = "$enabledServices of ${externalServices.size} enabled",
                iconRes = R.drawable.lucide_ic_puzzle,
                onClick = onManageExternalServices
            )
            SettingsDivider()
            NavigationRow(
                title = "Scrobbling",
                subtitle = "ListenBrainz and Libre.fm listening sync",
                iconRes = `in`.caffeinelabs.cassettecat.R.drawable.ic_logo_listenbrainz,
                onClick = onNavigateToScrobbling
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = "Data") {
            NavigationRow(
                title = "Privacy",
                subtitle = "Listening data and saved server credentials",
                iconRes = R.drawable.lucide_ic_shield,
                onClick = onNavigateToPrivacy
            )
            SettingsDivider()
            NavigationRow(
                title = "Backup & Restore",
                subtitle = "Keep your library, playlists, and preferences safe",
                iconRes = R.drawable.lucide_ic_archive_restore,
                onClick = onNavigateToBackupRestore
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = "Updates") {
            ServiceToggleRow(
                service = ExternalService.GITHUB_UPDATES,
                enabled = uiState.services.githubUpdatesEnabled,
                onToggle = { enabled -> viewModel.setServiceEnabled(ExternalService.GITHUB_UPDATES, enabled) },
                iconRes = R.drawable.lucide_ic_github
            )
            SettingsDivider(startPadding = 24.dp)
            UpdateCheckRow(
                result = updateCheckResult,
                checkEnabled = uiState.services.githubUpdatesEnabled,
                onCheck = { viewModel.checkForUpdate() }
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = "About") {
            NavigationRow(
                title = "Credits & Attribution",
                subtitle = "Services, libraries, and open source licenses",
                iconRes = R.drawable.lucide_ic_heart,
                onClick = onNavigateToCredits
            )
        }

        Spacer(Modifier.height(listBottomPadding))
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
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Column(content = content)
    }
}

@Composable
fun SettingsDivider(startPadding: Dp = 68.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startPadding),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun ServiceToggleRow(
    service: ExternalService,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    iconRes: Int? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
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
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
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
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(available.url)))
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

@Composable
fun NavigationRow(title: String, subtitle: String, iconRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            painter = painterResource(R.drawable.lucide_ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ServerRow(
    title: String,
    subtitle: String,
    config: StreamingServerConfig,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!config.connected) Modifier.tapScale(onConnect) else Modifier)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_server),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (config.connected) "Connected as ${config.username}" else subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (config.connected) {
            TextButton(onClick = hapticClick(onDisconnect)) { Text("Disconnect") }
        } else {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
