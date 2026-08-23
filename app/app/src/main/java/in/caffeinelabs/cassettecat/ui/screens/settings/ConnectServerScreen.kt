package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun ConnectServerScreen(
    protocol: StreamingProtocol,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectServerViewModel = viewModel()
) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val quickConnectState by viewModel.quickConnectState.collectAsStateWithLifecycle()
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var showHttpWarning by rememberSaveable { mutableStateOf(false) }
    var pendingQuickConnect by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(protocolTitle(protocol), style = MaterialTheme.typography.headlineSmall)
        Text(protocolSubtitle(protocol), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        when (val current = state) {
            is ConnectionState.Connected -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_circle_check_big),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Connected as ${current.displayName}", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = hapticClick(onDone), modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }

            else -> {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://music.example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(
                                if (passwordVisible) R.drawable.lucide_ic_eye_off else R.drawable.lucide_ic_eye
                            ),
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            modifier = Modifier.clickable(
                                onClick = hapticClick { passwordVisible = !passwordVisible }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (current is ConnectionState.Failed) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        current.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = hapticClick {
                        if (serverUrl.toUri().scheme.equals("http", ignoreCase = true)) {
                            showHttpWarning = true
                        } else {
                            viewModel.connect(protocol, serverUrl.trim(), username.trim(), password)
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
                        Text("Connect")
                    }
                }
                if (protocol == StreamingProtocol.JELLYFIN) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = hapticClick {
                            if (serverUrl.toUri().scheme.equals("http", ignoreCase = true)) {
                                pendingQuickConnect = true
                                showHttpWarning = true
                            } else {
                                viewModel.startQuickConnect(serverUrl.trim())
                            }
                        },
                        enabled = current != ConnectionState.Connecting && serverUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign in with Quick Connect")
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = hapticClick(onDone), modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }

    val untrustedCertificateState = state as? ConnectionState.UntrustedCertificate
    if (untrustedCertificateState != null) {
        AlertDialog(
            onDismissRequest = hapticClick { viewModel.cancelPendingConnection() },
            title = { Text("Untrusted certificate") },
            text = {
                Text(
                    "This server's certificate isn't signed by a recognized authority, common for self-hosted servers. " +
                        "Only continue if you trust this server and recognize its fingerprint:\n\n${untrustedCertificateState.fingerprint}"
                )
            },
            confirmButton = {
                TextButton(onClick = hapticClick { viewModel.trustCertificateAndRetry() }) { Text("Trust & Connect") }
            },
            dismissButton = {
                TextButton(onClick = hapticClick { viewModel.cancelPendingConnection() }) { Text("Cancel") }
            }
        )
    }

    val awaitingApproval = quickConnectState as? QuickConnectState.AwaitingApproval
    if (awaitingApproval != null) {
        AlertDialog(
            onDismissRequest = hapticClick { viewModel.cancelQuickConnect() },
            title = { Text("Quick Connect") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Enter this code in Jellyfin under your profile's Quick Connect page:")
                    Spacer(Modifier.height(16.dp))
                    Text(awaitingApproval.code, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = hapticClick { viewModel.cancelQuickConnect() }) { Text("Cancel") }
            }
        )
    }

    if (showHttpWarning) {
        AlertDialog(
            onDismissRequest = { showHttpWarning = false; pendingQuickConnect = false },
            title = { Text("Use HTTPS if possible") },
            text = { Text("HTTP can expose your login while it travels across the network. Continue only for a server you trust on a private network.") },
            confirmButton = {
                TextButton(onClick = hapticClick {
                    showHttpWarning = false
                    if (pendingQuickConnect) {
                        pendingQuickConnect = false
                        viewModel.startQuickConnect(serverUrl.trim())
                    } else {
                        viewModel.connect(protocol, serverUrl.trim(), username.trim(), password)
                    }
                }) { Text("Connect") }
            },
            dismissButton = {
                TextButton(onClick = hapticClick { showHttpWarning = false; pendingQuickConnect = false }) { Text("Cancel") }
            }
        )
    }
}

private fun protocolTitle(protocol: StreamingProtocol) = when (protocol) {
    StreamingProtocol.SUBSONIC -> "Connect to Subsonic"
    StreamingProtocol.JELLYFIN -> "Connect to Jellyfin"
}

private fun protocolSubtitle(protocol: StreamingProtocol) = when (protocol) {
    StreamingProtocol.SUBSONIC -> "Works with Navidrome, gonic, and other Subsonic-API servers."
    StreamingProtocol.JELLYFIN -> "Connect to a Jellyfin media server."
}
