package `in`.caffeinelabs.cassettecat.data.device

import android.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 2000L

// token is the desktop app's pairing code; the hardware player has none.
class DevicePlaybackRepository(private val apiClient: DeviceControlApiClient = DeviceControlApiClient()) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null

    private val _status = MutableStateFlow<DevicePlaybackStatus?>(null)
    val status: StateFlow<DevicePlaybackStatus?> = _status.asStateFlow()

    fun startPolling(host: String, port: Int, network: Network?, token: String? = null, intervalMs: Long = POLL_INTERVAL_MS) {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (true) {
                _status.value = apiClient.getPlaybackStatus(host, port, network, token)
                delay(intervalMs)
            }
        }
    }

    private fun sendThenRefresh(host: String, port: Int, network: Network?, token: String?, send: suspend () -> Boolean) {
        scope.launch {
            if (send() && pollJob?.isActive == true) {
                apiClient.getPlaybackStatus(host, port, network, token)?.let { _status.value = it }
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun sendAction(host: String, port: Int, action: String, network: Network?, token: String? = null) {
        sendThenRefresh(host, port, network, token) { apiClient.sendPlaybackAction(host, port, action, network, token) }
    }

    fun setVolume(host: String, port: Int, percent: Int, network: Network?, token: String? = null) {
        sendThenRefresh(host, port, network, token) { apiClient.setVolume(host, port, percent, network, token) }
    }

    fun seek(host: String, port: Int, positionMs: Long, network: Network?, token: String? = null) {
        sendThenRefresh(host, port, network, token) { apiClient.seek(host, port, positionMs, network, token) }
    }

    fun release() {
        stopPolling()
        scope.cancel()
    }
}
