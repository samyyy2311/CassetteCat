package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.device.DeviceConnectionType
import `in`.caffeinelabs.cassettecat.data.device.DevicePairingState
import `in`.caffeinelabs.cassettecat.data.device.DiscoveredDevice
import `in`.caffeinelabs.cassettecat.data.device.WifiDevicePairingRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PairingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = WifiDevicePairingRepository(app)

    val pairingState: StateFlow<DevicePairingState> = repository.pairingState

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

    override fun onCleared() {
        repository.release()
    }
}
