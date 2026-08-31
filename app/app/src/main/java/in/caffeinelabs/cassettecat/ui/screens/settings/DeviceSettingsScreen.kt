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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
            Text("Device Settings", style = MaterialTheme.typography.headlineSmall)
        }

        SettingsSection(title = "DEVICE NAME") {
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
                        statusMessage = if (ok) "Renamed to \"$nameInput\"." else "Couldn't rename the player."
                    }
                }) { Text("Save") }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "WI-FI MODE") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Direct Hotspot", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Switch the player back to broadcasting its own Wi-Fi hotspot.",
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
                            statusMessage = if (ok) "Wi-Fi mode changed." else "Couldn't change Wi-Fi mode."
                        }
                    },
                    colors = appSwitchColors()
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "MAINTENANCE") {
            Row(
                modifier = Modifier.fillMaxWidth().tapScale { showRestartConfirm = true }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Restart Player", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Soft reboot. Nothing is erased.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SettingsDivider()
            Row(
                modifier = Modifier.fillMaxWidth().tapScale {
                    pairingViewModel.rescanLibrary { ok ->
                        statusMessage = if (ok) "Rescan started." else "Couldn't start a rescan."
                    }
                }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rescan SD Card", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Re-index songs copied on outside the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SettingsDivider()
            Row(
                modifier = Modifier.fillMaxWidth().tapScale {
                    pairingViewModel.syncDeviceTime { ok ->
                        statusMessage = if (ok) "Player clock updated." else "Couldn't update the player's clock."
                    }
                }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sync Phone Time", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "The player has no clock battery, so this resets on every power cycle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "DANGER ZONE") {
            Row(
                modifier = Modifier.fillMaxWidth().tapScale { showResetConfirm = true }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Factory Reset", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    Text(
                        "Erases all songs and settings on the player.",
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
            title = { Text("Restart the player?") },
            text = { Text("Playback will stop while it reboots.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestartConfirm = false
                    pairingViewModel.restartDevice { ok ->
                        statusMessage = if (ok) "Restarting..." else "Couldn't restart the player."
                    }
                }) { Text("Restart") }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Factory reset the player?") },
            text = { Text("This erases every song and setting on the player. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    pairingViewModel.factoryReset { ok ->
                        statusMessage = if (ok) "Player reset." else "Couldn't reset the player."
                    }
                }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
