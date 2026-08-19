package `in`.caffeinelabs.cassettecat.data.device

import kotlinx.serialization.Serializable

enum class DeviceConnectionType { SOFT_AP, STATION }

@Serializable
data class CompanionStatus(
    val deviceName: String = "CassetteCat Player",
    val firmwareVersion: String = "1.0.0",
    val batteryPercentage: Int? = null,
    val isCharging: Boolean = false,
    val storageUsedBytes: Long? = null,
    val storageTotalBytes: Long? = null
)

data class DiscoveredDevice(
    val name: String,
    val host: String = "192.168.4.1",
    val port: Int = 80,
    val connectionType: DeviceConnectionType,
    val status: CompanionStatus? = null
)

sealed interface DevicePairingState {
    data object SelectingMode : DevicePairingState
    data class Searching(val mode: DeviceConnectionType) : DevicePairingState
    data class DeviceFound(val device: DiscoveredDevice) : DevicePairingState
    data class Connecting(val device: DiscoveredDevice) : DevicePairingState
    data class Connected(val device: DiscoveredDevice) : DevicePairingState
    data class Failed(val mode: DeviceConnectionType, val message: String) : DevicePairingState
}

