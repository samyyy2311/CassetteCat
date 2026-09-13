package `in`.caffeinelabs.cassettecat.ui.screens.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.device.DevicePairingState
import `in`.caffeinelabs.cassettecat.data.device.FirmwareUpdateInfo
import `in`.caffeinelabs.cassettecat.data.device.GitHubReleaseResult
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PairingViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import com.composables.icons.lucide.R
import java.io.File

private sealed interface UpdateCheckState {
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val info: FirmwareUpdateInfo) : UpdateCheckState
    data object NoReleaseYet : UpdateCheckState
    data object CheckFailed : UpdateCheckState
}

@Composable
fun DeviceFirmwareScreen(
    pairingViewModel: PairingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val pairingState by pairingViewModel.pairingState.collectAsStateWithLifecycle()
    val currentFirmwareVersion = (pairingState as? DevicePairingState.Connected)?.device?.status?.firmwareVersion ?: "unknown"

    var checkState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Checking) }
    var pendingRemoteUpdate by remember { mutableStateOf<FirmwareUpdateInfo?>(null) }
    var pendingLocalFile by remember { mutableStateOf<File?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val remoteUpdateStartedMessage = stringResource(AppR.string.device_firmware_remote_started)
    val updateFailedMessage = stringResource(AppR.string.device_firmware_update_failed)
    val localUpdateDoneMessage = stringResource(AppR.string.device_firmware_local_done)

    fun runCheck() {
        checkState = UpdateCheckState.Checking
        pairingViewModel.checkForFirmwareUpdate { result ->
            checkState = when (result) {
                is GitHubReleaseResult.Found ->
                    if (result.info.version == currentFirmwareVersion) UpdateCheckState.UpToDate
                    else UpdateCheckState.Available(result.info)
                GitHubReleaseResult.NoReleaseYet -> UpdateCheckState.NoReleaseYet
                GitHubReleaseResult.CheckFailed -> UpdateCheckState.CheckFailed
            }
        }
    }

    LaunchedEffect(Unit) { runCheck() }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val input = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
        val cacheFile = File(context.cacheDir, "ota_upload.bin")
        input.use { stream -> cacheFile.outputStream().use { output -> stream.copyTo(output) } }
        pendingLocalFile = cacheFile
    }

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
            Text(stringResource(AppR.string.device_firmware_title), style = MaterialTheme.typography.headlineSmall)
        }

        SettingsSection(title = stringResource(AppR.string.device_firmware_current_version_section)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    "v$currentFirmwareVersion",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = IbmPlexMonoFontFamily)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.device_firmware_updates_section)) {
            when (val state = checkState) {
                UpdateCheckState.Checking -> Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(AppR.string.device_firmware_checking), style = MaterialTheme.typography.bodyLarge)
                }

                UpdateCheckState.UpToDate -> Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_circle_check_big),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(AppR.string.device_firmware_up_to_date), style = MaterialTheme.typography.bodyLarge)
                }

                is UpdateCheckState.Available -> Row(
                    modifier = Modifier.fillMaxWidth().tapScale { if (!isBusy) pendingRemoteUpdate = state.info }.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(AppR.string.device_firmware_available), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(AppR.string.device_firmware_version_on_github, state.info.version),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(AppR.string.device_firmware_update_now),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                UpdateCheckState.NoReleaseYet -> Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(AppR.string.device_firmware_no_releases), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(AppR.string.device_firmware_no_releases_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                UpdateCheckState.CheckFailed -> Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(AppR.string.device_firmware_check_failed), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(AppR.string.device_firmware_check_connection),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { runCheck() }) { Text(stringResource(AppR.string.action_retry)) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.device_firmware_manual_section)) {
            Row(
                modifier = Modifier.fillMaxWidth().tapScale { if (!isBusy) filePicker.launch(arrayOf("application/octet-stream")) }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.device_firmware_choose_file), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(AppR.string.device_firmware_choose_file_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isBusy) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(AppR.string.device_firmware_updating), style = MaterialTheme.typography.bodyLarge)
            }
        }

        resultMessage?.let {
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

    pendingRemoteUpdate?.let { info ->
        AlertDialog(
            onDismissRequest = { pendingRemoteUpdate = null },
            title = { Text(stringResource(AppR.string.device_firmware_remote_confirm_title, info.version)) },
            text = { Text(stringResource(AppR.string.device_firmware_remote_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRemoteUpdate = null
                    isBusy = true
                    pairingViewModel.updateFirmwareFromUrl(info.downloadUrl) { ok ->
                        isBusy = false
                        resultMessage = if (ok) remoteUpdateStartedMessage else updateFailedMessage
                    }
                }) { Text(stringResource(AppR.string.action_update), color = MaterialTheme.colorScheme.tertiary) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoteUpdate = null }) { Text(stringResource(AppR.string.action_cancel)) }
            }
        )
    }

    pendingLocalFile?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingLocalFile = null },
            title = { Text(stringResource(AppR.string.device_firmware_local_confirm_title)) },
            text = { Text(stringResource(AppR.string.device_firmware_local_confirm_message, file.name)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingLocalFile = null
                    isBusy = true
                    pairingViewModel.uploadFirmware(file) { ok ->
                        isBusy = false
                        resultMessage = if (ok) localUpdateDoneMessage else updateFailedMessage
                    }
                }) { Text(stringResource(AppR.string.action_update), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingLocalFile = null }) { Text(stringResource(AppR.string.action_cancel)) }
            }
        )
    }
}
