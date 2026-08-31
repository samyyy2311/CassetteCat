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

class DevicePlaybackRepository {
    private val apiClient = DeviceControlApiClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null

    private val _status = MutableStateFlow<DevicePlaybackStatus?>(null)
    val status: StateFlow<DevicePlaybackStatus?> = _status.asStateFlow()

    fun startPolling(host: String, port: Int, network: Network?) {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (true) {
                _status.value = apiClient.getPlaybackStatus(host, port, network)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun sendAction(host: String, port: Int, action: String, network: Network?) {
        scope.launch { apiClient.sendPlaybackAction(host, port, action, network) }
    }

    fun setVolume(host: String, port: Int, percent: Int, network: Network?) {
        scope.launch { apiClient.setVolume(host, port, percent, network) }
    }

    fun seek(host: String, port: Int, positionMs: Long, network: Network?) {
        scope.launch { apiClient.seek(host, port, positionMs, network) }
    }

    fun release() {
        stopPolling()
        scope.cancel()
    }
}
