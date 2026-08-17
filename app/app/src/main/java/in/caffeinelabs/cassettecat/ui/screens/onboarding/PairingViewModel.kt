package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.device.DeviceConnectionType
import `in`.caffeinelabs.cassettecat.data.device.DevicePairingRepository
import `in`.caffeinelabs.cassettecat.data.device.DevicePairingState
import `in`.caffeinelabs.cassettecat.data.device.DiscoveredDevice
import `in`.caffeinelabs.cassettecat.data.device.FakeDevicePairingRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PairingViewModel : ViewModel() {
    // Hardcoded, not a constructor param, so viewModel()'s reflection factory keeps a true
    // zero-arg constructor. Swap for a real impl once firmware exposes a network surface.
    private val repository: DevicePairingRepository = FakeDevicePairingRepository()

    val pairingState: StateFlow<DevicePairingState> = repository.pairingState

    fun selectMode(mode: DeviceConnectionType) = repository.startDiscovery(mode)

    fun cancelSearch() = repository.stopDiscovery()

    fun connect(device: DiscoveredDevice) {
        viewModelScope.launch { repository.connect(device) }
    }
}
