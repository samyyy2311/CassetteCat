package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.device.DesktopQueueTrack
import `in`.caffeinelabs.cassettecat.data.device.DeviceControlApiClient
import `in`.caffeinelabs.cassettecat.data.device.DevicePlaybackRepository
import `in`.caffeinelabs.cassettecat.data.device.DevicePlaybackStatus
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val apiClient = DeviceControlApiClient()

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

    private val _artwork = MutableStateFlow<ImageBitmap?>(null)
    val artwork: StateFlow<ImageBitmap?> = _artwork.asStateFlow()
    private val _upNext = MutableStateFlow<List<DesktopQueueTrack>>(emptyList())
    val upNext: StateFlow<List<DesktopQueueTrack>> = _upNext.asStateFlow()

    init {
        viewModelScope.launch {
            playbackStatus.filterNotNull()
                .map { it.trackTitle to it.artworkKey }
                .distinctUntilChanged()
                .collect { (_, artworkKey) ->
                    refreshQueue()
                    _artwork.value = artworkKey?.takeIf { it.isNotEmpty() }?.let { loadArtwork() }
                }
        }
    }

    private suspend fun refreshQueue() {
        val desktop = connectedDesktop() ?: return
        apiClient.getQueue(desktop.host, desktop.port, desktop.code)?.let { _upNext.value = it }
    }

    private suspend fun loadArtwork(): ImageBitmap? {
        val desktop = connectedDesktop() ?: return null
        val bytes = apiClient.getArtwork(desktop.host, desktop.port, desktop.code) ?: return null
        return withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
    }

    fun playFromQueue(index: Int) {
        val desktop = connectedDesktop() ?: return
        viewModelScope.launch { apiClient.playQueueTrack(desktop.host, desktop.port, index, desktop.code) }
    }

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
