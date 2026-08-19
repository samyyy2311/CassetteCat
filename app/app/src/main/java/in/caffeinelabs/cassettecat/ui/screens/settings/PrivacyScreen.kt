package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
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
    val preferences by preferencesRepository.preferences.collectAsState(initial = AppPreferences())
    var showClearCredentialsConfirm by remember { mutableStateOf(false) }
    val crashLogRepository = remember { CrashLogRepository(context) }
    var hasCrashLog by remember { mutableStateOf(crashLogRepository.hasCrashLog()) }
    var showCrashLog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
            Text("Privacy", style = MaterialTheme.typography.headlineSmall)
        }

        SettingsSection(title = "Listening Record") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Collect listening activity", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Store playback time and play counts on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferences.listeningStatsEnabled,
                    onCheckedChange = hapticToggle { enabled -> scope.launch { preferencesRepository.setListeningStatsEnabled(enabled) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                        checkedBorderColor = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
        }

        SettingsSection(title = "Server credentials", content = {
            Text(
                "Server passwords and access tokens are encrypted with Android Keystore.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
            )
            TextButton(
                onClick = hapticClick { showClearCredentialsConfirm = true },
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            ) {
                Text("Disconnect all servers and remove credentials", color = MaterialTheme.colorScheme.error)
            }
        })

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = "Diagnostics") {
            ActionRow(
                title = "Crash Log",
                subtitle = if (hasCrashLog) "A crash was recorded on this device" else "No crashes recorded",
                iconRes = R.drawable.lucide_ic_bug,
                onClick = hapticClick { showCrashLog = true }
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = "Online Policy") {
            NavigationRow(
                title = "Web Privacy Policy",
                subtitle = "Read our full online privacy policy at cassettecat.caffeinelabs.in",
                iconRes = R.drawable.lucide_ic_globe,
                iconTint = Color(0xFF10B981),
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://cassettecat.caffeinelabs.in/privacy/")
                        )
                    )
                }
            )
        }
    }

    if (showClearCredentialsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCredentialsConfirm = false },
            title = { Text("Remove server credentials?") },
            text = { Text("This disconnects Subsonic and Jellyfin and removes their saved credentials from this device.") },
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
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = hapticClick { showClearCredentialsConfirm = false }) { Text("Cancel") } }
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
                context.startActivity(Intent.createChooser(intent, "Share Crash Log"))
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
                Text("Crash Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PressDepthIconButton(R.drawable.lucide_ic_x, "Close", onDismiss)
            }
            Spacer(Modifier.height(16.dp))
            if (logText.isEmpty()) {
                Text(
                    "No crashes recorded. Nothing is ever sent off this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Stored on this device only. Share it yourself if you want to report a bug.",
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
                    TextButton(onClick = hapticClick { onShare(logText) }) { Text("Share") }
                    TextButton(onClick = hapticClick(onClear)) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
