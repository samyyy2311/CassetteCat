package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.backup.BackupRepository
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RestoreBackupScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupRepository = remember { BackupRepository(context) }
    val restoreFailedMessage = stringResource(AppR.string.onboarding_restore_failed)
    var failureMessage by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            isRestoring = true
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        text?.let { backupRepository.restoreBackup(it) }
                    }
                    if (result?.isSuccess == true) onContinue() else failureMessage = restoreFailedMessage
                } finally {
                    isRestoring = false
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingHeaderRow(currentStep = 1, totalSteps = 5, onSkip = onContinue)
        Spacer(Modifier.height(10.dp))

        Text(stringResource(AppR.string.onboarding_restore_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(AppR.string.onboarding_restore_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            OnboardingHeroIcon(iconRes = R.drawable.lucide_ic_archive_restore)
        }

        Button(
            onClick = hapticClick { restoreLauncher.launch(arrayOf("*/*")) },
            enabled = !isRestoring,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(AppR.string.onboarding_restore_choose)) }
    }

    failureMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { failureMessage = null },
            title = { Text(stringResource(AppR.string.onboarding_restore_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { failureMessage = null }) {
                    Text(stringResource(AppR.string.action_ok))
                }
            }
        )
    }
}
