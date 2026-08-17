package `in`.caffeinelabs.cassettecat.data.device

import kotlinx.coroutines.flow.StateFlow

enum class DeviceConnectionType { SOFT_AP, STATION }

data class DiscoveredDevice(val name: String, val connectionType: DeviceConnectionType)

sealed interface DevicePairingState {
    data object SelectingMode : DevicePairingState
    data class Searching(val mode: DeviceConnectionType) : DevicePairingState
    data class DeviceFound(val device: DiscoveredDevice) : DevicePairingState
    data class Connecting(val device: DiscoveredDevice) : DevicePairingState
    data class Connected(val device: DiscoveredDevice) : DevicePairingState
    data class Failed(val mode: DeviceConnectionType, val message: String) : DevicePairingState
}

interface DevicePairingRepository {
    val pairingState: StateFlow<DevicePairingState>
    fun startDiscovery(mode: DeviceConnectionType)
    fun stopDiscovery()
    suspend fun connect(device: DiscoveredDevice)
}
