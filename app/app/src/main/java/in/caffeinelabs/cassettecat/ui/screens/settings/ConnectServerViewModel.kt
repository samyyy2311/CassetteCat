package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.streaming.CertificatePinRepository
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.data.streaming.findUntrustedCertificateCause
import `in`.caffeinelabs.cassettecat.data.streaming.jellyfin.JellyfinApiClient
import `in`.caffeinelabs.cassettecat.data.streaming.subsonic.SubsonicApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException
import java.net.SocketTimeoutException

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val displayName: String) : ConnectionState
    data class Failed(val message: String) : ConnectionState
    data class UntrustedCertificate(val fingerprint: String) : ConnectionState
}

// protocol is passed per-call rather than injected into the constructor, so this
// stays a zero-arg-Application AndroidViewModel, no custom ViewModelProvider.Factory.
class ConnectServerViewModel(app: Application) : AndroidViewModel(app) {
    private val serverRepository = StreamingServerRepository(app)
    private val credentialStore = CredentialStore(app)
    private val certificatePinRepository = CertificatePinRepository(app)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var pendingAttempt: (suspend () -> ConnectionState)? = null

    fun connect(protocol: StreamingProtocol, serverUrl: String, username: String, password: String) {
        val attempt: suspend () -> ConnectionState = {
            when (protocol) {
                StreamingProtocol.SUBSONIC -> connectSubsonic(serverUrl, username, password)
                StreamingProtocol.JELLYFIN -> connectJellyfin(serverUrl, username, password)
            }
        }
        pendingAttempt = attempt
        runAttempt(attempt)
    }

    // Called after the user reviews the certificate fingerprint and chooses to trust it.
    fun trustCertificateAndRetry() {
        val fingerprint = (_connectionState.value as? ConnectionState.UntrustedCertificate)?.fingerprint ?: return
        val attempt = pendingAttempt ?: return
        viewModelScope.launch {
            certificatePinRepository.pin(fingerprint)
            runAttempt(attempt)
        }
    }

    fun cancelPendingConnection() {
        pendingAttempt = null
        _connectionState.value = ConnectionState.Idle
    }

    private fun runAttempt(attempt: suspend () -> ConnectionState) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            _connectionState.value = runCatching { attempt() }.getOrElse { it.toConnectionState() }
        }
    }

    // SubsonicApiException/JellyfinApiException messages are already user-readable and
    // surfaced as-is; only raw java.net exceptions get mapped, their defaults are technical.
    private fun Throwable.toConnectionState(): ConnectionState {
        val untrusted = findUntrustedCertificateCause()
        if (untrusted != null) return ConnectionState.UntrustedCertificate(untrusted.fingerprint)
        return ConnectionState.Failed(toFriendlyMessage())
    }

    private fun Throwable.toFriendlyMessage(): String = when (this) {
        is UnknownHostException -> "Couldn't find that server. Check the URL."
        is ConnectException -> "Couldn't reach the server. Check the URL and that it's running."
        is SocketTimeoutException -> "The server took too long to respond. Check the URL and your connection."
        else -> message ?: "Couldn't connect"
    }

    private suspend fun connectSubsonic(serverUrl: String, username: String, password: String): ConnectionState {
        SubsonicApiClient(serverUrl, username, password).ping()
        credentialStore.saveSubsonicPassword(password)
        serverRepository.setConfig(
            StreamingProtocol.SUBSONIC,
            StreamingServerConfig(serverUrl = serverUrl, username = username, connected = true)
        )
        return ConnectionState.Connected(username)
    }

    private suspend fun connectJellyfin(serverUrl: String, username: String, password: String): ConnectionState {
        val client = JellyfinApiClient(serverUrl, serverRepository.deviceId())
        val result = client.authenticate(username, password)
        credentialStore.saveJellyfinAccessToken(result.AccessToken)
        serverRepository.setConfig(
            StreamingProtocol.JELLYFIN,
            StreamingServerConfig(serverUrl = serverUrl, username = username, userId = result.User.Id, connected = true)
        )
        return ConnectionState.Connected(username)
    }
}
