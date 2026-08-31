package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.device.DeviceConnectionType
import `in`.caffeinelabs.cassettecat.data.device.DeviceControlApiClient
import `in`.caffeinelabs.cassettecat.data.device.DeviceFileEntry
import `in`.caffeinelabs.cassettecat.data.device.DevicePairingState
import `in`.caffeinelabs.cassettecat.data.device.DevicePlaybackRepository
import `in`.caffeinelabs.cassettecat.data.device.DevicePlaybackStatus
import `in`.caffeinelabs.cassettecat.data.device.DeviceSyncRepository
import `in`.caffeinelabs.cassettecat.data.device.DiscoveredDevice
import `in`.caffeinelabs.cassettecat.data.device.GitHubFirmwareReleaseClient
import `in`.caffeinelabs.cassettecat.data.device.GitHubReleaseResult
import `in`.caffeinelabs.cassettecat.data.device.RemoteSongEntry
import `in`.caffeinelabs.cassettecat.data.device.SyncItemState
import `in`.caffeinelabs.cassettecat.data.device.WifiDevicePairingRepository
import `in`.caffeinelabs.cassettecat.data.library.Song
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class PairingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = WifiDevicePairingRepository(app)
    private val syncRepository = DeviceSyncRepository()
    private val playbackRepository = DevicePlaybackRepository()
    private val controlApiClient = DeviceControlApiClient()
    private val githubReleaseClient = GitHubFirmwareReleaseClient()

    val pairingState: StateFlow<DevicePairingState> = repository.pairingState
    val syncStates: StateFlow<Map<String, SyncItemState>> = syncRepository.syncStates
    val remoteManifest: StateFlow<List<RemoteSongEntry>?> = syncRepository.remoteManifest
    val playbackStatus: StateFlow<DevicePlaybackStatus?> = playbackRepository.status

    fun selectMode(mode: DeviceConnectionType) = repository.startDiscovery(mode)

    fun cancelSearch() = repository.stopDiscovery()

    fun disconnect() = repository.stopDiscovery()

    fun connect(device: DiscoveredDevice) {
        viewModelScope.launch { repository.connect(device) }
    }

    fun provisionWifi(device: DiscoveredDevice, ssid: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.provisionWifi(device, ssid, pass)
            onResult(ok)
        }
    }

    fun refreshSyncManifest() {
        val connected = pairingState.value as? DevicePairingState.Connected ?: return
        viewModelScope.launch {
            syncRepository.refreshManifest(connected.device.host, connected.device.port, repository.currentNetwork)
        }
    }

    fun pendingSyncSongs(localSongs: List<Song>): List<Song> = syncRepository.pendingSongs(localSongs)

    fun syncSongs(songs: List<Song>) {
        val connected = pairingState.value as? DevicePairingState.Connected ?: return
        syncRepository.syncSongs(songs, connected.device.host, connected.device.port, repository.currentNetwork)
    }

    private fun connectedDevice(): DiscoveredDevice? = (pairingState.value as? DevicePairingState.Connected)?.device

    fun startPlaybackPolling() {
        val device = connectedDevice() ?: return
        playbackRepository.startPolling(device.host, device.port, repository.currentNetwork)
    }

    fun stopPlaybackPolling() = playbackRepository.stopPolling()

    fun sendPlaybackAction(action: String) {
        val device = connectedDevice() ?: return
        playbackRepository.sendAction(device.host, device.port, action, repository.currentNetwork)
    }

    fun setDeviceVolume(percent: Int) {
        val device = connectedDevice() ?: return
        playbackRepository.setVolume(device.host, device.port, percent, repository.currentNetwork)
    }

    fun seekDevicePlayback(positionMs: Long) {
        val device = connectedDevice() ?: return
        playbackRepository.seek(device.host, device.port, positionMs, repository.currentNetwork)
    }

    fun renameDevice(name: String, onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.renameDevice(device.host, device.port, name, repository.currentNetwork))
        }
    }

    fun setWifiMode(mode: String, onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.setWifiMode(device.host, device.port, mode, repository.currentNetwork))
        }
    }

    fun factoryReset(onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.factoryReset(device.host, device.port, repository.currentNetwork))
        }
    }

    fun restartDevice(onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.restartDevice(device.host, device.port, repository.currentNetwork))
        }
    }

    fun rescanLibrary(onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.rescanLibrary(device.host, device.port, repository.currentNetwork))
        }
    }

    fun syncDeviceTime(onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.syncDeviceTime(device.host, device.port, System.currentTimeMillis(), repository.currentNetwork))
        }
    }

    fun listDeviceFiles(path: String, onResult: (List<DeviceFileEntry>?) -> Unit) {
        val device = connectedDevice() ?: return onResult(null)
        viewModelScope.launch {
            onResult(controlApiClient.listFiles(device.host, device.port, path, repository.currentNetwork))
        }
    }

    fun deleteDeviceFile(path: String, onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.deleteFile(device.host, device.port, path, repository.currentNetwork))
        }
    }

    fun uploadFirmware(file: File, onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.uploadFirmware(device.host, device.port, file, repository.currentNetwork))
        }
    }

    fun checkForFirmwareUpdate(onResult: (GitHubReleaseResult) -> Unit) {
        viewModelScope.launch { onResult(githubReleaseClient.getLatestRelease()) }
    }

    fun updateFirmwareFromUrl(url: String, onResult: (Boolean) -> Unit) {
        val device = connectedDevice() ?: return onResult(false)
        viewModelScope.launch {
            onResult(controlApiClient.updateFirmwareFromUrl(device.host, device.port, url, repository.currentNetwork))
        }
    }

    override fun onCleared() {
        repository.release()
        playbackRepository.release()
        syncRepository.release()
    }
}
