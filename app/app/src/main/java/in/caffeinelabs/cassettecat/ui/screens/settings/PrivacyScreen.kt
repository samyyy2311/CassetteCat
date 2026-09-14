package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.diagnostics.CrashLogRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.hapticToggle
import kotlinx.coroutines.launch

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by preferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    var showClearCredentialsConfirm by remember { mutableStateOf(false) }
    val crashLogRepository = remember { CrashLogRepository(context) }
    var hasCrashLog by remember { mutableStateOf(crashLogRepository.hasCrashLog()) }
    var showCrashLog by remember { mutableStateOf(false) }
    val shareCrashLogTitle = stringResource(AppR.string.privacy_share_crash_log)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
            Text(stringResource(AppR.string.privacy_title), style = MaterialTheme.typography.headlineSmall)
        }

        SettingsSection(title = stringResource(AppR.string.privacy_listening_record)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.privacy_collect_listening_activity), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(AppR.string.privacy_collect_listening_activity_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferences.listeningStatsEnabled,
                    onCheckedChange = hapticToggle { enabled -> scope.launch { preferencesRepository.setListeningStatsEnabled(enabled) } },
                    colors = appSwitchColors()
                )
            }
        }

        SettingsSection(title = stringResource(AppR.string.privacy_server_credentials), content = {
            Text(
                stringResource(AppR.string.privacy_server_credentials_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
            )
            TextButton(
                onClick = hapticClick { showClearCredentialsConfirm = true },
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            ) {
                Text(stringResource(AppR.string.privacy_remove_credentials), color = MaterialTheme.colorScheme.error)
            }
        })

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(AppR.string.privacy_diagnostics)) {
            ActionRow(
                title = stringResource(AppR.string.privacy_crash_log),
                subtitle = stringResource(
                    if (hasCrashLog) AppR.string.privacy_crash_recorded else AppR.string.privacy_no_crashes
                ),
                iconRes = R.drawable.lucide_ic_bug,
                onClick = hapticClick { showCrashLog = true }
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(AppR.string.privacy_online_policy)) {
            NavigationRow(
                title = stringResource(AppR.string.privacy_web_policy),
                subtitle = stringResource(AppR.string.privacy_web_policy_description),
                iconRes = R.drawable.lucide_ic_globe,
                iconTint = Color(0xFF10B981),
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://cassettecat.caffeinelabs.in/privacy/".toUri()
                        )
                    )
                }
            )
        }
    }

    if (showClearCredentialsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCredentialsConfirm = false },
            title = { Text(stringResource(AppR.string.privacy_remove_credentials_title)) },
            text = { Text(stringResource(AppR.string.privacy_remove_credentials_message)) },
            confirmButton = {
                TextButton(onClick = hapticClick {
                    showClearCredentialsConfirm = false
                    scope.launch {
                        val servers = StreamingServerRepository(context)
                        val credentials = CredentialStore(context)
                        StreamingProtocol.entries.forEach { protocol ->
                            servers.disconnect(protocol)
                            credentials.clear(protocol)
                        }
                    }
                }) { Text(stringResource(AppR.string.action_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = hapticClick { showClearCredentialsConfirm = false }) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        )
    }

    if (showCrashLog) {
        CrashLogSheet(
            logText = remember(showCrashLog) { crashLogRepository.readCrashLog() },
            onDismiss = { showCrashLog = false },
            onShare = { text ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, shareCrashLogTitle))
            },
            onClear = {
                crashLogRepository.clearCrashLog()
                hasCrashLog = false
                showCrashLog = false
            }
        )
    }
}

@Composable
private fun CrashLogSheet(
    logText: String,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    onClear: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(AppR.string.privacy_crash_log), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PressDepthIconButton(R.drawable.lucide_ic_x, stringResource(AppR.string.close), onDismiss)
            }
            Spacer(Modifier.height(16.dp))
            if (logText.isEmpty()) {
                Text(
                    stringResource(AppR.string.privacy_crash_log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(AppR.string.privacy_crash_log_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    logText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = hapticClick { onShare(logText) }) {
                        Text(stringResource(AppR.string.action_share))
                    }
                    TextButton(onClick = hapticClick(onClear)) {
                        Text(stringResource(AppR.string.action_clear), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
