package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
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
                Text(stringResource(AppR.string.pairing_onboarding_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(AppR.string.pairing_onboarding_subtitle),
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
                PressDepthIconButton(
                    R.drawable.lucide_ic_chevron_left,
                    stringResource(AppR.string.action_back),
                    onFinish
                )
                Text(stringResource(AppR.string.pairing_player_title), style = MaterialTheme.typography.headlineSmall)
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    stringResource(AppR.string.pairing_companion_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    stringResource(AppR.string.pairing_companion_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        when (val current = state) {
            is DevicePairingState.SelectingMode -> {
                SettingsSection(title = stringResource(AppR.string.pairing_connection_mode)) {
                    NavigationRow(
                        title = stringResource(AppR.string.pairing_direct_hotspot_title),
                        subtitle = stringResource(AppR.string.pairing_direct_hotspot_subtitle),
                        iconRes = R.drawable.lucide_ic_radio_tower,
                        onClick = startSoftApDiscovery
                    )
                    SettingsDivider()
                    NavigationRow(
                        title = stringResource(AppR.string.pairing_local_network_title),
                        subtitle = stringResource(AppR.string.pairing_local_network_subtitle),
                        iconRes = R.drawable.lucide_ic_house_wifi,
                        onClick = { viewModel.selectMode(DeviceConnectionType.STATION) }
                    )
                }
            }

            is DevicePairingState.Searching -> {
                SettingsSection(title = stringResource(AppR.string.pairing_discovery)) {
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
                                stringResource(
                                    if (current.mode == DeviceConnectionType.SOFT_AP) {
                                        AppR.string.pairing_searching_hotspot
                                    } else {
                                        AppR.string.pairing_searching_local
                                    }
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(AppR.string.pairing_power_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = hapticClick { viewModel.cancelSearch() },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(stringResource(AppR.string.action_cancel), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            is DevicePairingState.DeviceFound -> {
                SettingsSection(title = stringResource(AppR.string.pairing_device_found)) {
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
                            Text(stringResource(AppR.string.action_connect))
                        }
                    }
                }
            }

            is DevicePairingState.Connecting -> {
                SettingsSection(title = stringResource(AppR.string.pairing_connecting)) {
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
                        Text(
                            stringResource(AppR.string.pairing_establishing_session, current.device.name),
                            style = MaterialTheme.typography.bodyLarge
                        )
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
                SettingsSection(title = stringResource(AppR.string.pairing_connection_status)) {
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
                            Text(stringResource(AppR.string.pairing_connection_failed), style = MaterialTheme.typography.bodyLarge)
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
                        Text(stringResource(AppR.string.action_try_again), color = MaterialTheme.colorScheme.tertiary)
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

    SettingsSection(title = stringResource(AppR.string.pairing_connected_companion)) {
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
                    stringResource(AppR.string.pairing_connected_at, device.host, device.port),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = hapticClick(onDisconnect)) {
                Text(stringResource(AppR.string.action_disconnect), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    SettingsSection(title = stringResource(AppR.string.pairing_control_panel)) {
        NavigationRow(
            title = stringResource(AppR.string.pairing_now_playing_title),
            subtitle = stringResource(AppR.string.pairing_now_playing_subtitle),
            iconRes = R.drawable.lucide_ic_music,
            onClick = onNavigateToNowPlaying
        )
        SettingsDivider()
        NavigationRow(
            title = stringResource(AppR.string.pairing_sync_songs_title),
            subtitle = stringResource(AppR.string.pairing_sync_songs_subtitle),
            iconRes = R.drawable.lucide_ic_upload,
            onClick = onNavigateToSync
        )
        SettingsDivider()
        NavigationRow(
            title = stringResource(AppR.string.pairing_storage_title),
            subtitle = stringResource(AppR.string.pairing_storage_subtitle),
            iconRes = R.drawable.lucide_ic_folder,
            onClick = onNavigateToStorage
        )
        SettingsDivider()
        NavigationRow(
            title = stringResource(AppR.string.pairing_firmware_title),
            subtitle = stringResource(AppR.string.pairing_firmware_installed, status.firmwareVersion),
            iconRes = R.drawable.lucide_ic_cpu,
            onClick = onNavigateToFirmware
        )
        SettingsDivider()
        NavigationRow(
            title = stringResource(AppR.string.pairing_device_settings_title),
            subtitle = stringResource(AppR.string.pairing_device_settings_subtitle),
            iconRes = R.drawable.lucide_ic_settings,
            onClick = onNavigateToDeviceSettings
        )
    }

    Spacer(Modifier.height(24.dp))

    SettingsSection(title = stringResource(AppR.string.pairing_hardware_telemetry)) {
        SettingsDetailRow(
            stringResource(AppR.string.pairing_firmware_version),
            "v${status.firmwareVersion}"
        )
        SettingsDivider()
        val battery = status.batteryPercentage ?: 100
        SettingsDetailRow(
            stringResource(AppR.string.pairing_battery_level),
            if (status.isCharging) {
                stringResource(AppR.string.pairing_battery_charging_value, battery)
            } else {
                stringResource(AppR.string.pairing_battery_value, battery)
            }
        )
        SettingsDivider()
        SettingsDetailRow(
            stringResource(AppR.string.pairing_sd_storage),
            "${formatBytes(used)} / ${formatBytes(total)}"
        )
        SettingsDivider()
        SettingsDetailRow(
            stringResource(AppR.string.pairing_protocol),
            stringResource(
                if (device.connectionType == DeviceConnectionType.SOFT_AP) {
                    AppR.string.pairing_protocol_hotspot
                } else {
                    AppR.string.pairing_protocol_local
                }
            )
        )
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
