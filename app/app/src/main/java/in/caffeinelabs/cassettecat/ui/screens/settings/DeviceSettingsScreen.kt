package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.device.DeviceConnectionType
import `in`.caffeinelabs.cassettecat.data.device.DevicePairingState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PairingViewModel
import `in`.caffeinelabs.cassettecat.ui.util.hapticToggle
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import com.composables.icons.lucide.R

@Composable
fun DeviceSettingsScreen(
    pairingViewModel: PairingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val pairingState by pairingViewModel.pairingState.collectAsStateWithLifecycle()
    val connectedDevice = (pairingState as? DevicePairingState.Connected)?.device
    val connectionType = connectedDevice?.connectionType ?: DeviceConnectionType.STATION
    var wifiModeIsSoftAp by remember(connectionType) { mutableStateOf(connectionType == DeviceConnectionType.SOFT_AP) }
    var nameInput by remember { mutableStateOf(connectedDevice?.name ?: "") }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val renamedMessage = stringResource(AppR.string.device_settings_renamed, nameInput)
    val renameFailedMessage = stringResource(AppR.string.device_settings_rename_failed)
    val wifiChangedMessage = stringResource(AppR.string.device_settings_wifi_changed)
    val wifiChangeFailedMessage = stringResource(AppR.string.device_settings_wifi_change_failed)
    val rescanStartedMessage = stringResource(AppR.string.device_settings_rescan_started)
    val rescanFailedMessage = stringResource(AppR.string.device_settings_rescan_failed)
    val timeUpdatedMessage = stringResource(AppR.string.device_settings_time_updated)
    val timeUpdateFailedMessage = stringResource(AppR.string.device_settings_time_update_failed)
    val restartingMessage = stringResource(AppR.string.device_settings_restarting)
    val restartFailedMessage = stringResource(AppR.string.device_settings_restart_failed)
    val resetMessage = stringResource(AppR.string.device_settings_reset_done)
    val resetFailedMessage = stringResource(AppR.string.device_settings_reset_failed)

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
            Text(stringResource(AppR.string.pairing_device_settings_title), style = MaterialTheme.typography.headlineSmall)
        }

        SettingsSection(title = stringResource(AppR.string.device_settings_name_section)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = {
                    pairingViewModel.renameDevice(nameInput) { ok ->
                        statusMessage = if (ok) renamedMessage else renameFailedMessage
                    }
                }) { Text(stringResource(AppR.string.action_save)) }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.device_settings_wifi_section)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.pairing_direct_hotspot_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(AppR.string.device_settings_hotspot_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = wifiModeIsSoftAp,
                    onCheckedChange = hapticToggle { enabled ->
                        val mode = if (enabled) "softap" else "station"
                        pairingViewModel.setWifiMode(mode) { ok ->
                            if (ok) wifiModeIsSoftAp = enabled
                            statusMessage = if (ok) wifiChangedMessage else wifiChangeFailedMessage
                        }
                    },
                    colors = appSwitchColors()
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.device_settings_maintenance_section)) {
            Row(
                modifier = Modifier.fillMaxWidth().tapScale { showRestartConfirm = true }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.device_settings_restart_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(AppR.string.device_settings_restart_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SettingsDivider()
            Row(
                modifier = Modifier.fillMaxWidth().tapScale {
                    pairingViewModel.rescanLibrary { ok ->
                        statusMessage = if (ok) rescanStartedMessage else rescanFailedMessage
                    }
                }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.device_settings_rescan_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(AppR.string.device_settings_rescan_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SettingsDivider()
            Row(
                modifier = Modifier.fillMaxWidth().tapScale {
                    pairingViewModel.syncDeviceTime { ok ->
                        statusMessage = if (ok) timeUpdatedMessage else timeUpdateFailedMessage
                    }
                }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.device_settings_sync_time_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(AppR.string.device_settings_sync_time_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.device_settings_danger_section)) {
            Row(
                modifier = Modifier.fillMaxWidth().tapScale { showResetConfirm = true }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(AppR.string.device_settings_factory_reset_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        stringResource(AppR.string.device_settings_factory_reset_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        statusMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(Modifier.height(listBottomPadding))
    }

    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { showRestartConfirm = false },
            title = { Text(stringResource(AppR.string.device_settings_restart_confirm_title)) },
            text = { Text(stringResource(AppR.string.device_settings_restart_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestartConfirm = false
                    pairingViewModel.restartDevice { ok ->
                        statusMessage = if (ok) restartingMessage else restartFailedMessage
                    }
                }) { Text(stringResource(AppR.string.action_restart)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirm = false }) { Text(stringResource(AppR.string.action_cancel)) }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(AppR.string.device_settings_reset_confirm_title)) },
            text = { Text(stringResource(AppR.string.device_settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    pairingViewModel.factoryReset { ok ->
                        statusMessage = if (ok) resetMessage else resetFailedMessage
                    }
                }) { Text(stringResource(AppR.string.action_reset), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(AppR.string.action_cancel)) }
            }
        )
    }
}
