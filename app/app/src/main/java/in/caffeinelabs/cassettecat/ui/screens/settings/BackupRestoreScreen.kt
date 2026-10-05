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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.backup.BackupRepository
import `in`.caffeinelabs.cassettecat.data.device.DesktopRemoteRepository
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupRepository = remember { BackupRepository(context) }
    val backupCreatedMessage = stringResource(AppR.string.backup_created)
    val backupFailedMessage = stringResource(AppR.string.backup_create_failed)
    val restoreSuccessMessage = stringResource(AppR.string.backup_restore_success)
    val restoreFailedMessage = stringResource(AppR.string.backup_restore_failed)
    val computerBackupDoneMessage = stringResource(AppR.string.backup_computer_done)
    val computerBackupFailedMessage = stringResource(AppR.string.backup_computer_failed)
    val computerRestoreUnavailableMessage = stringResource(AppR.string.backup_computer_restore_unavailable)
    val desktopRemote = remember { DesktopRemoteRepository.getInstance(context) }
    val desktopState by desktopRemote.state.collectAsStateWithLifecycle()

    var backupToRestore by remember { mutableStateOf<String?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = backupRepository.createBackup()
                val wrote = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null
                }
                resultMessage = if (wrote) backupCreatedMessage else backupFailedMessage
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                if (text == null) resultMessage = restoreFailedMessage else backupToRestore = text
            }
        }
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
            Text(stringResource(AppR.string.backup_title), style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            stringResource(AppR.string.backup_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.backup_section)) {
            NavigationRow(
                title = stringResource(AppR.string.backup_create_title),
                subtitle = stringResource(AppR.string.backup_create_subtitle),
                iconRes = R.drawable.lucide_ic_download,
                onClick = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    createLauncher.launch("CassetteCat_Backup_$date.json")
                }
            )
            NavigationRow(
                title = stringResource(AppR.string.backup_restore_title),
                subtitle = stringResource(AppR.string.backup_restore_subtitle),
                iconRes = R.drawable.lucide_ic_upload,
                onClick = { openLauncher.launch(arrayOf("*/*")) }
            )
        }

        if (desktopState.address != null && !desktopState.offlineBlackout) {
            SettingsSection(title = stringResource(AppR.string.backup_computer_section)) {
                NavigationRow(
                    title = stringResource(AppR.string.backup_computer_create_title),
                    subtitle = desktopState.lastBackupAtMs?.let {
                        stringResource(AppR.string.backup_computer_last, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)))
                    } ?: stringResource(AppR.string.backup_computer_never),
                    iconRes = R.drawable.lucide_ic_monitor,
                    onClick = {
                        scope.launch {
                            val saved = desktopRemote.backUp(backupRepository.createBackup())
                            resultMessage = if (saved) computerBackupDoneMessage else computerBackupFailedMessage
                        }
                    }
                )
                NavigationRow(
                    title = stringResource(AppR.string.backup_computer_restore_title),
                    subtitle = stringResource(AppR.string.backup_computer_restore_subtitle),
                    iconRes = R.drawable.lucide_ic_upload,
                    onClick = {
                        scope.launch {
                            val text = desktopRemote.downloadBackup()
                            if (text == null) resultMessage = computerRestoreUnavailableMessage else backupToRestore = text
                        }
                    }
                )
            }
        }
    }

    if (backupToRestore != null) {
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text(stringResource(AppR.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(AppR.string.backup_restore_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val text = backupToRestore
                    backupToRestore = null
                    if (text != null) {
                        scope.launch {
                            val restored = backupRepository.restoreBackup(text).isSuccess
                            resultMessage = if (restored) restoreSuccessMessage else restoreFailedMessage
                        }
                    }
                }) {
                    Text(stringResource(AppR.string.action_restore), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) { Text(stringResource(AppR.string.action_cancel)) }
            }
        )
    }

    val message = resultMessage
    if (message != null) {
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text(stringResource(AppR.string.backup_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { resultMessage = null }) { Text(stringResource(AppR.string.action_ok)) }
            }
        )
    }
}
