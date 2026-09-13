package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import androidx.core.net.toUri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.scrobble.LibreFmClient
import `in`.caffeinelabs.cassettecat.data.scrobble.ListenBrainzClient
import `in`.caffeinelabs.cassettecat.data.scrobble.ScrobbleSettings
import `in`.caffeinelabs.cassettecat.data.scrobble.ScrobbleSettingsRepository
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.hapticToggle
import kotlinx.coroutines.launch

@Composable
fun ScrobbleSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ScrobbleSettingsRepository(context) }
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = ScrobbleSettings())

    var showListenBrainzDialog by remember { mutableStateOf(false) }
    var showLibreFmDialog by remember { mutableStateOf(false) }

    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
            Text(stringResource(AppR.string.scrobbling_title), style = MaterialTheme.typography.headlineSmall)
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                stringResource(AppR.string.scrobbling_services_label),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                stringResource(AppR.string.scrobbling_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.scrobbling_listenbrainz_section)) {
            val isConnected = settings.listenBrainz.userToken.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(AppR.drawable.ic_logo_listenbrainz),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.scrobbling_listenbrainz_name), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (isConnected) {
                            if (settings.listenBrainz.userName.isNotBlank()) {
                                stringResource(AppR.string.scrobbling_connected_as, settings.listenBrainz.userName)
                            } else {
                                stringResource(AppR.string.scrobbling_connected)
                            }
                        } else {
                            stringResource(AppR.string.scrobbling_not_connected)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isConnected) {
                    Switch(
                        checked = settings.listenBrainz.enabled,
                        onCheckedChange = hapticToggle { enabled -> scope.launch { repository.setListenBrainzEnabled(enabled) } },
                        colors = appSwitchColors()
                    )
                }
            }

            SettingsDivider()

            if (isConnected) {
                TextButton(
                    onClick = hapticClick { scope.launch { repository.disconnectListenBrainz() } },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                ) {
                    Text(stringResource(AppR.string.scrobbling_disconnect_listenbrainz), color = MaterialTheme.colorScheme.error)
                }
            } else {
                NavigationRow(
                    title = stringResource(AppR.string.scrobbling_connect_account),
                    subtitle = stringResource(AppR.string.scrobbling_listenbrainz_connect_description),
                    iconRes = R.drawable.lucide_ic_link,
                    onClick = { showListenBrainzDialog = true }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.scrobbling_librefm_section)) {
            val isConnected = settings.libreFm.sessionKey.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(AppR.drawable.ic_logo_librefm),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppR.string.scrobbling_librefm_name), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (isConnected) {
                            stringResource(AppR.string.scrobbling_connected_as, settings.libreFm.username)
                        } else {
                            stringResource(AppR.string.scrobbling_not_connected)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isConnected) {
                    Switch(
                        checked = settings.libreFm.enabled,
                        onCheckedChange = hapticToggle { enabled -> scope.launch { repository.setLibreFmEnabled(enabled) } },
                        colors = appSwitchColors()
                    )
                }
            }

            SettingsDivider()

            if (isConnected) {
                TextButton(
                    onClick = hapticClick { scope.launch { repository.disconnectLibreFm() } },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                ) {
                    Text(stringResource(AppR.string.scrobbling_disconnect_librefm), color = MaterialTheme.colorScheme.error)
                }
            } else {
                NavigationRow(
                    title = stringResource(AppR.string.scrobbling_connect_account),
                    subtitle = stringResource(AppR.string.scrobbling_librefm_connect_description),
                    iconRes = R.drawable.lucide_ic_link,
                    onClick = { showLibreFmDialog = true }
                )
            }
        }

        Spacer(Modifier.height(listBottomPadding + 24.dp))
    }

    if (showListenBrainzDialog) {
        ListenBrainzConnectDialog(
            onDismiss = { showListenBrainzDialog = false },
            onConnect = { token, userName ->
                scope.launch {
                    repository.saveListenBrainz(token, userName, enabled = true)
                    showListenBrainzDialog = false
                }
            },
            onGetTokenClick = { openUrl("https://listenbrainz.org/profile/") }
        )
    }

    if (showLibreFmDialog) {
        LibreFmConnectDialog(
            onDismiss = { showLibreFmDialog = false },
            onConnect = { username, sessionKey ->
                scope.launch {
                    repository.saveLibreFm(username, sessionKey, enabled = true)
                    showLibreFmDialog = false
                }
            }
        )
    }
}

@Composable
private fun ListenBrainzConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (token: String, userName: String) -> Unit,
    onGetTokenClick: () -> Unit
) {
    var token by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val client = remember { ListenBrainzClient() }
    val tokenRequiredMessage = stringResource(AppR.string.scrobbling_token_required)
    val invalidTokenMessage = stringResource(AppR.string.scrobbling_token_invalid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(AppR.string.scrobbling_listenbrainz_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(AppR.string.scrobbling_listenbrainz_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it; errorMessage = null },
                    label = { Text(stringResource(AppR.string.scrobbling_user_token)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onGetTokenClick) {
                    Text(stringResource(AppR.string.scrobbling_get_listenbrainz_token), color = MaterialTheme.colorScheme.tertiary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = hapticClick {
                    if (token.isBlank()) {
                        errorMessage = tokenRequiredMessage
                        return@hapticClick
                    }
                    isValidating = true
                    errorMessage = null
                    scope.launch {
                        val userName = client.validateToken(token.trim())
                        isValidating = false
                        if (userName != null) {
                            onConnect(token.trim(), userName)
                        } else {
                            errorMessage = invalidTokenMessage
                        }
                    }
                },
                enabled = !isValidating && token.isNotBlank()
            ) {
                if (isValidating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(AppR.string.action_connect))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(AppR.string.action_cancel)) }
        }
    )
}

@Composable
private fun LibreFmConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (username: String, sessionKey: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val client = remember { LibreFmClient() }
    val credentialsRequiredMessage = stringResource(AppR.string.scrobbling_credentials_required)
    val authFailedMessage = stringResource(AppR.string.scrobbling_auth_failed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(AppR.string.scrobbling_librefm_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(AppR.string.scrobbling_librefm_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text(stringResource(AppR.string.scrobbling_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text(stringResource(AppR.string.scrobbling_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = hapticClick {
                    if (username.isBlank() || password.isBlank()) {
                        errorMessage = credentialsRequiredMessage
                        return@hapticClick
                    }
                    isAuthenticating = true
                    errorMessage = null
                    scope.launch {
                        val sessionKey = client.authenticate(username.trim(), password)
                        isAuthenticating = false
                        if (sessionKey != null) {
                            onConnect(username.trim(), sessionKey)
                        } else {
                            errorMessage = authFailedMessage
                        }
                    }
                },
                enabled = !isAuthenticating && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(AppR.string.scrobbling_log_in))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(AppR.string.action_cancel)) }
        }
    )
}
