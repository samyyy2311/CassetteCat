package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun ConnectServerScreen(
    protocol: StreamingProtocol,
    onDone: () -> Unit,
    onCancel: () -> Unit = onDone,
    modifier: Modifier = Modifier,
    viewModel: ConnectServerViewModel = viewModel()
) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val quickConnectState by viewModel.quickConnectState.collectAsStateWithLifecycle()
    val savedConfig by viewModel.savedConfig(protocol).collectAsStateWithLifecycle(initialValue = StreamingServerConfig())

    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var showHttpWarning by rememberSaveable { mutableStateOf(false) }
    var pendingQuickConnect by rememberSaveable { mutableStateOf(false) }

    fun normalizedServerUrl(): String {
        val trimmed = serverUrl.trim()
        return if (trimmed.toUri().scheme == null) "https://$trimmed" else trimmed
    }

    LaunchedEffect(savedConfig) {
        if (serverUrl.isBlank() && savedConfig.serverUrl.isNotBlank()) {
            serverUrl = savedConfig.serverUrl
        }
        if (username.isBlank() && savedConfig.username.isNotBlank()) {
            username = savedConfig.username
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (protocol == StreamingProtocol.SUBSONIC) AppR.drawable.ic_logo_subsonic
                        else AppR.drawable.ic_logo_jellyfin
                    ),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(stringResource(protocolTitleRes(protocol)), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(protocolSubtitleRes(protocol)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        when (val current = state) {
            is ConnectionState.Connected -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_circle_check_big),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(AppR.string.server_connected_as, current.displayName),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(AppR.string.server_connected_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = hapticClick(onDone), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(AppR.string.action_done))
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text(stringResource(AppR.string.server_url)) },
                        placeholder = { Text("https://music.example.com", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_globe),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(AppR.string.server_username)) },
                        placeholder = { Text(stringResource(AppR.string.server_username_placeholder)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_user),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(AppR.string.server_password)) },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_lock),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Icon(
                                painter = painterResource(
                                    if (passwordVisible) R.drawable.lucide_ic_eye_off else R.drawable.lucide_ic_eye
                                ),
                                contentDescription = stringResource(
                                    if (passwordVisible) AppR.string.server_hide_password else AppR.string.server_show_password
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(onClick = hapticClick { passwordVisible = !passwordVisible })
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (current is ConnectionState.Failed) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                current.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_shield_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(AppR.string.server_credentials_encrypted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = hapticClick {
                        if (normalizedServerUrl().toUri().scheme.equals("http", ignoreCase = true)) {
                            showHttpWarning = true
                        } else {
                            viewModel.connect(protocol, normalizedServerUrl(), username.trim(), password)
                        }
                    },
                    enabled = current != ConnectionState.Connecting &&
                        serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (current is ConnectionState.Connecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(AppR.string.action_connect))
                    }
                }

                if (protocol == StreamingProtocol.JELLYFIN) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = hapticClick {
                            if (normalizedServerUrl().toUri().scheme.equals("http", ignoreCase = true)) {
                                pendingQuickConnect = true
                                showHttpWarning = true
                            } else {
                                viewModel.startQuickConnect(normalizedServerUrl())
                            }
                        },
                        enabled = current != ConnectionState.Connecting && serverUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(AppR.string.server_quick_connect_sign_in))
                    }
                }

                Spacer(Modifier.height(4.dp))
                TextButton(onClick = hapticClick(onCancel), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        }
    }

    val untrustedCertificateState = state as? ConnectionState.UntrustedCertificate
    if (untrustedCertificateState != null) {
        AlertDialog(
            onDismissRequest = hapticClick { viewModel.cancelPendingConnection() },
            title = { Text(stringResource(AppR.string.server_untrusted_certificate_title)) },
            text = {
                Text(stringResource(AppR.string.server_untrusted_certificate_message, untrustedCertificateState.fingerprint))
            },
            confirmButton = {
                TextButton(onClick = hapticClick { viewModel.trustCertificateAndRetry() }) {
                    Text(stringResource(AppR.string.server_trust_and_connect))
                }
            },
            dismissButton = {
                TextButton(onClick = hapticClick { viewModel.cancelPendingConnection() }) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        )
    }

    val awaitingApproval = quickConnectState as? QuickConnectState.AwaitingApproval
    if (awaitingApproval != null) {
        AlertDialog(
            onDismissRequest = hapticClick { viewModel.cancelQuickConnect() },
            title = { Text(stringResource(AppR.string.server_quick_connect_title)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(AppR.string.server_quick_connect_instructions))
                    Spacer(Modifier.height(16.dp))
                    Text(awaitingApproval.code, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = hapticClick { viewModel.cancelQuickConnect() }) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        )
    }

    if (showHttpWarning) {
        AlertDialog(
            onDismissRequest = { showHttpWarning = false; pendingQuickConnect = false },
            title = { Text(stringResource(AppR.string.server_http_warning_title)) },
            text = { Text(stringResource(AppR.string.server_http_warning_message)) },
            confirmButton = {
                TextButton(onClick = hapticClick {
                    showHttpWarning = false
                    if (pendingQuickConnect) {
                        pendingQuickConnect = false
                        viewModel.startQuickConnect(normalizedServerUrl())
                    } else {
                        viewModel.connect(protocol, normalizedServerUrl(), username.trim(), password)
                    }
                }) { Text(stringResource(AppR.string.action_connect)) }
            },
            dismissButton = {
                TextButton(onClick = hapticClick { showHttpWarning = false; pendingQuickConnect = false }) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        )
    }
}

private fun protocolTitleRes(protocol: StreamingProtocol): Int = when (protocol) {
    StreamingProtocol.SUBSONIC -> AppR.string.server_connect_subsonic
    StreamingProtocol.JELLYFIN -> AppR.string.server_connect_jellyfin
}

private fun protocolSubtitleRes(protocol: StreamingProtocol): Int = when (protocol) {
    StreamingProtocol.SUBSONIC -> AppR.string.server_subsonic_subtitle
    StreamingProtocol.JELLYFIN -> AppR.string.server_jellyfin_subtitle
}
