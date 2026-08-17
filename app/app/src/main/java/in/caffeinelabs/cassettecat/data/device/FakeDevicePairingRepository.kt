package `in`.caffeinelabs.cassettecat.data.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// No-op stub: firmware has no Wi-Fi/HTTP server yet, so this stays in Searching
// indefinitely rather than simulating a fake find. Swap for a real
// NsdManager/WifiNetworkSpecifier-backed implementation once firmware exposes a network surface.
class FakeDevicePairingRepository : DevicePairingRepository {
    private val _pairingState = MutableStateFlow<DevicePairingState>(DevicePairingState.SelectingMode)
    override val pairingState: StateFlow<DevicePairingState> = _pairingState.asStateFlow()

    override fun startDiscovery(mode: DeviceConnectionType) {
        _pairingState.value = DevicePairingState.Searching(mode)
    }

    override fun stopDiscovery() {
        _pairingState.value = DevicePairingState.SelectingMode
    }

    override suspend fun connect(device: DiscoveredDevice) {
        // Unreachable here (DeviceFound never emitted); kept for interface completeness.
    }
}
