package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.device.CompanionStatus
import `in`.caffeinelabs.cassettecat.data.device.DeviceConnectionType
import `in`.caffeinelabs.cassettecat.data.device.DevicePairingState
import `in`.caffeinelabs.cassettecat.data.device.DiscoveredDevice
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.settings.NavigationRow
import `in`.caffeinelabs.cassettecat.ui.screens.settings.SettingsDivider
import `in`.caffeinelabs.cassettecat.ui.screens.settings.SettingsSection
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun PairingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    isOnboarding: Boolean = false,
    listBottomPadding: Dp = 0.dp,
    onNavigateToSync: () -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToFirmware: () -> Unit = {},
    onNavigateToDeviceSettings: () -> Unit = {},
    viewModel: PairingViewModel = viewModel()
) {
    val state by viewModel.pairingState.collectAsStateWithLifecycle()
    val nearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.selectMode(DeviceConnectionType.SOFT_AP)
    }
    val startSoftApDiscovery = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            nearbyWifiPermissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            viewModel.selectMode(DeviceConnectionType.SOFT_AP)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (isOnboarding) {
            Column(modifier = Modifier.padding(24.dp)) {
                OnboardingHeaderRow(currentStep = 4, totalSteps = 5, onSkip = onFinish)
                Spacer(Modifier.height(10.dp))
                Text("Connect CassetteCat", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pair over Wi-Fi or direct hotspot.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onFinish)
                Text("CassetteCat Player", style = MaterialTheme.typography.headlineSmall)
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "Companion Hardware Sync",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    "Pair and monitor your standalone ESP32 audio player device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        when (val current = state) {
            is DevicePairingState.SelectingMode -> {
                SettingsSection(title = "CONNECTION MODE") {
                    NavigationRow(
                        title = "Direct Hotspot",
                        subtitle = "Connect directly to the player's Wi-Fi network (192.168.4.1)",
                        iconRes = R.drawable.lucide_ic_radio_tower,
                        onClick = startSoftApDiscovery
                    )
                    SettingsDivider()
                    NavigationRow(
                        title = "Local Network (mDNS)",
                        subtitle = "Discover player connected to the same home Wi-Fi router",
                        iconRes = R.drawable.lucide_ic_house_wifi,
                        onClick = { viewModel.selectMode(DeviceConnectionType.STATION) }
                    )
                }
            }

            is DevicePairingState.Searching -> {
                SettingsSection(title = "DISCOVERY") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (current.mode == DeviceConnectionType.SOFT_AP) "Searching for CassetteCat hotspot..." else "Looking for devices on local Wi-Fi...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Make sure player is powered on",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = hapticClick { viewModel.cancelSearch() },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            is DevicePairingState.DeviceFound -> {
                SettingsSection(title = "DEVICE FOUND") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_cassette_tape),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(current.device.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${current.device.host}:${current.device.port}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = hapticClick { viewModel.connect(current.device) }) {
                            Text("Connect")
                        }
                    }
                }
            }

            is DevicePairingState.Connecting -> {
                SettingsSection(title = "CONNECTING") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("Establishing session with ${current.device.name}...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            is DevicePairingState.Connected -> {
                ConnectedCompanionView(
                    device = current.device,
                    onDisconnect = { viewModel.disconnect() },
                    onNavigateToSync = onNavigateToSync,
                    onNavigateToNowPlaying = onNavigateToNowPlaying,
                    onNavigateToStorage = onNavigateToStorage,
                    onNavigateToFirmware = onNavigateToFirmware,
                    onNavigateToDeviceSettings = onNavigateToDeviceSettings
                )
            }

            is DevicePairingState.Failed -> {
                SettingsSection(title = "CONNECTION STATUS") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_triangle_alert),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Could not connect", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                current.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = hapticClick { viewModel.selectMode(current.mode) },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text("Try again", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        Spacer(Modifier.height(listBottomPadding + 24.dp))
    }
}

@Composable
private fun ConnectedCompanionView(
    device: DiscoveredDevice,
    onDisconnect: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToFirmware: () -> Unit,
    onNavigateToDeviceSettings: () -> Unit
) {
    val status = device.status ?: CompanionStatus(deviceName = device.name)
    val used = status.storageUsedBytes ?: (4L * 1024 * 1024 * 1024)
    val total = status.storageTotalBytes ?: (32L * 1024 * 1024 * 1024)

    SettingsSection(title = "CONNECTED COMPANION") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_circle_check_big),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Connected at ${device.host}:${device.port}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = hapticClick(onDisconnect)) {
                Text("Disconnect", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    SettingsSection(title = "CONTROL PANEL") {
        NavigationRow(
            title = "Now Playing",
            subtitle = "Remote play, pause, skip, and volume",
            iconRes = R.drawable.lucide_ic_music,
            onClick = onNavigateToNowPlaying
        )
        SettingsDivider()
        NavigationRow(
            title = "Sync Songs",
            subtitle = "Push local songs to the player's SD card",
            iconRes = R.drawable.lucide_ic_upload,
            onClick = onNavigateToSync
        )
        SettingsDivider()
        NavigationRow(
            title = "Storage",
            subtitle = "Browse and manage files on the SD card",
            iconRes = R.drawable.lucide_ic_folder,
            onClick = onNavigateToStorage
        )
        SettingsDivider()
        NavigationRow(
            title = "Firmware",
            subtitle = "v${status.firmwareVersion} installed",
            iconRes = R.drawable.lucide_ic_cpu,
            onClick = onNavigateToFirmware
        )
        SettingsDivider()
        NavigationRow(
            title = "Device Settings",
            subtitle = "Rename, Wi-Fi mode, factory reset",
            iconRes = R.drawable.lucide_ic_settings,
            onClick = onNavigateToDeviceSettings
        )
    }

    Spacer(Modifier.height(24.dp))

    SettingsSection(title = "HARDWARE TELEMETRY") {
        SettingsDetailRow("Firmware Version", "v${status.firmwareVersion}")
        SettingsDivider()
        SettingsDetailRow("Battery Level", "${status.batteryPercentage ?: 100}%${if (status.isCharging) " (Charging)" else ""}")
        SettingsDivider()
        SettingsDetailRow("SD Card Storage", "${formatBytes(used)} / ${formatBytes(total)}")
        SettingsDivider()
        SettingsDetailRow("Protocol", if (device.connectionType == DeviceConnectionType.SOFT_AP) "Direct SoftAP Hotspot" else "Local Network (mDNS)")
    }
}

@Composable
private fun SettingsDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.0f MB".format(bytes / (1024.0 * 1024))
    else -> "%.0f KB".format(bytes / 1024.0)
}
