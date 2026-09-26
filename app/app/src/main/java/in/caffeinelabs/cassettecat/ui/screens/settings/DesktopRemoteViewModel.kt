package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.device.DevicePlaybackRepository
import `in`.caffeinelabs.cassettecat.data.device.DevicePlaybackStatus
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.desktopRemoteDataStore by preferencesDataStore(name = "desktop_remote")
private val DESKTOP_ADDRESS = stringPreferencesKey("address")

data class DesktopAddress(val host: String, val port: Int, val code: String)

/** Parses the "ip:port#CODE" address shown in the desktop app's Phone Remote settings. */
internal fun parseDesktopAddress(text: String): DesktopAddress? {
    val parts = text.trim().split("#", limit = 2)
    if (parts.size != 2) return null
    val host = parts[0].substringBeforeLast(':', "")
    val port = parts[0].substringAfterLast(':').toIntOrNull()
    val code = parts[1].trim().uppercase()
    if (host.isBlank() || port == null || port !in 1..65535 || code.isEmpty()) return null
    return DesktopAddress(host, port, code)
}

data class DesktopRemoteState(
    val loaded: Boolean = false,
    val address: DesktopAddress? = null,
    val offlineBlackout: Boolean = false
)

class DesktopRemoteViewModel(app: Application) : AndroidViewModel(app), PlaybackRemote {
    private val dataStore = app.desktopRemoteDataStore
    private val playbackRepository = DevicePlaybackRepository()

    val state: StateFlow<DesktopRemoteState> = combine(
        dataStore.data,
        ServiceSettingsRepository(app).settings
    ) { prefs, services ->
        DesktopRemoteState(
            loaded = true,
            address = prefs[DESKTOP_ADDRESS]?.let(::parseDesktopAddress),
            offlineBlackout = services.offlineBlackoutMode
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DesktopRemoteState())

    override val playbackStatus: StateFlow<DevicePlaybackStatus?> = playbackRepository.status

    /** Saves [text] when it parses as a desktop address; returns whether it did. */
    fun pair(text: String): Boolean {
        if (parseDesktopAddress(text) == null) return false
        viewModelScope.launch { dataStore.edit { it[DESKTOP_ADDRESS] = text.trim() } }
        return true
    }

    fun forget() {
        playbackRepository.stopPolling()
        viewModelScope.launch { dataStore.edit { it.remove(DESKTOP_ADDRESS) } }
    }

    private fun connectedDesktop(): DesktopAddress? =
        state.value.takeUnless { it.offlineBlackout }?.address

    override fun startPlaybackPolling() {
        val desktop = connectedDesktop() ?: return
        playbackRepository.startPolling(desktop.host, desktop.port, null, desktop.code)
    }

    override fun stopPlaybackPolling() = playbackRepository.stopPolling()

    override fun sendPlaybackAction(action: String) {
        val desktop = connectedDesktop() ?: return
        playbackRepository.sendAction(desktop.host, desktop.port, action, null, desktop.code)
    }

    override fun setDeviceVolume(percent: Int) {
        val desktop = connectedDesktop() ?: return
        playbackRepository.setVolume(desktop.host, desktop.port, percent, null, desktop.code)
    }

    override fun seekDevicePlayback(positionMs: Long) {
        val desktop = connectedDesktop() ?: return
        playbackRepository.seek(desktop.host, desktop.port, positionMs, null, desktop.code)
    }

    override fun onCleared() {
        playbackRepository.release()
    }
}
