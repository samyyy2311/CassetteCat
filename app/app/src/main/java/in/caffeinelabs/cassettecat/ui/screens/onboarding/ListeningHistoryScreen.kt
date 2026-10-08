package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.scrobble.ScrobbleAccount
import `in`.caffeinelabs.cassettecat.data.scrobble.ScrobbleSettings
import `in`.caffeinelabs.cassettecat.data.scrobble.ScrobbleSettingsRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ListenBrainzConnectDialog
import `in`.caffeinelabs.cassettecat.ui.screens.settings.NavigationRow
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ScrobbleAccountDialog
import `in`.caffeinelabs.cassettecat.ui.screens.settings.SettingsDivider
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ToggleRow
import `in`.caffeinelabs.cassettecat.ui.screens.settings.text
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@Composable
fun ListeningHistoryScreen(onContinue: () -> Unit, onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by preferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val scrobbleRepository = remember { ScrobbleSettingsRepository(context) }
    val scrobbleSettings by scrobbleRepository.settings.collectAsStateWithLifecycle(initialValue = ScrobbleSettings())
    var connectingListenBrainz by remember { mutableStateOf(false) }
    var signingInTo by remember { mutableStateOf<ScrobbleAccount?>(null) }
    val pendingSaves = remember { mutableListOf<Job>() }
    fun save(block: suspend () -> Unit) {
        pendingSaves += scope.launch { block() }
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingHeaderRow(currentStep = 3, totalSteps = 5)
        Spacer(Modifier.height(10.dp))

        Text(stringResource(AppR.string.onboarding_history_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(AppR.string.onboarding_history_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ToggleRow(
                title = stringResource(AppR.string.settings_listening_record),
                subtitle = stringResource(AppR.string.privacy_collect_listening_activity_description),
                checked = preferences.listeningStatsEnabled,
                onCheckedChange = { enabled -> save { preferencesRepository.setListeningStatsEnabled(enabled) } },
                iconRes = R.drawable.lucide_ic_disc_3
            )
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)

            Spacer(Modifier.height(24.dp))
            Text(stringResource(AppR.string.onboarding_scrobbling_label), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(AppR.string.onboarding_scrobbling_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            NavigationRow(
                title = stringResource(AppR.string.scrobbling_listenbrainz_name),
                subtitle = scrobbleSettings.listenBrainz.userName.takeIf { it.isNotBlank() }
                    ?.let { stringResource(AppR.string.scrobbling_connected_as, it) }
                    ?: stringResource(AppR.string.scrobbling_listenbrainz_connect_description),
                iconRes = AppR.drawable.ic_logo_listenbrainz,
                iconTint = Color.Unspecified,
                onClick = { connectingListenBrainz = true }
            )
            ScrobbleAccount.entries.filter { it.client != null }.forEach { account ->
                val config = scrobbleSettings.account(account)
                SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
                NavigationRow(
                    title = stringResource(account.text.name),
                    subtitle = if (config.sessionKey.isNotBlank()) {
                        stringResource(AppR.string.scrobbling_connected_as, config.username)
                    } else {
                        stringResource(account.text.connectDescription)
                    },
                    iconRes = account.text.icon,
                    iconTint = Color.Unspecified,
                    onClick = { signingInTo = account }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = hapticClick { scope.launch { pendingSaves.joinAll(); onContinue() } }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(AppR.string.action_continue))
        }
    }

    if (connectingListenBrainz) {
        ListenBrainzConnectDialog(
            onDismiss = { connectingListenBrainz = false },
            onConnect = { token, userName ->
                save { scrobbleRepository.saveListenBrainz(token, userName, enabled = true) }
                connectingListenBrainz = false
            },
            onGetTokenClick = { onOpenUrl("https://listenbrainz.org/profile/") }
        )
    }
    signingInTo?.let { account ->
        ScrobbleAccountDialog(
            account = account,
            onDismiss = { signingInTo = null },
            onConnect = { username, sessionKey ->
                save { scrobbleRepository.saveAccount(account, username, sessionKey) }
                signingInTo = null
            }
        )
    }
}
