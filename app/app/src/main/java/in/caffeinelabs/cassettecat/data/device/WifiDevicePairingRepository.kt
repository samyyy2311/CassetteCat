package `in`.caffeinelabs.cassettecat.data.device

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WifiDevicePairingRepository(private val context: Context) : DevicePairingRepository {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val apiClient = CompanionApiClient()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _pairingState = MutableStateFlow<DevicePairingState>(DevicePairingState.SelectingMode)
    override val pairingState: StateFlow<DevicePairingState> = _pairingState.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var searchJob: Job? = null

    override fun startDiscovery(mode: DeviceConnectionType) {
        stopDiscovery()
        _pairingState.value = DevicePairingState.Searching(mode)

        when (mode) {
            DeviceConnectionType.SOFT_AP -> {
                searchJob = scope.launch {
                    val defaultHost = "192.168.4.1"
                    val defaultPort = 80
                    var attempts = 0
                    while (attempts < 10) {
                        delay(1200)
                        val status = apiClient.getStatus(defaultHost, defaultPort)
                        if (status != null) {
                            val device = DiscoveredDevice(
                                name = status.deviceName,
                                host = defaultHost,
                                port = defaultPort,
                                connectionType = DeviceConnectionType.SOFT_AP,
                                status = status
                            )
                            _pairingState.value = DevicePairingState.DeviceFound(device)
                            return@launch
                        }
                        attempts++
                    }
                    _pairingState.value = DevicePairingState.Failed(mode, "No CassetteCat device found on Wi-Fi hotspot.")
                }
            }

            DeviceConnectionType.STATION -> {
                startMdnsDiscovery()
                searchJob = scope.launch {
                    delay(15000)
                    if (_pairingState.value is DevicePairingState.Searching) {
                        _pairingState.value = DevicePairingState.Failed(mode, "No CassetteCat device found on this Wi-Fi network.")
                    }
                }
            }
        }
    }

    override fun stopDiscovery() {
        searchJob?.cancel()
        searchJob = null
        stopMdnsDiscovery()
        _pairingState.value = DevicePairingState.SelectingMode
    }

    override suspend fun connect(device: DiscoveredDevice) {
        _pairingState.value = DevicePairingState.Connecting(device)
        val status = apiClient.getStatus(device.host, device.port)
        if (status != null) {
            _pairingState.value = DevicePairingState.Connected(device.copy(status = status))
        } else {
            _pairingState.value = DevicePairingState.Connected(device)
        }
    }

    private fun startMdnsDiscovery() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                _pairingState.value = DevicePairingState.Failed(DeviceConnectionType.STATION, "Network discovery failed ($errorCode)")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}

            override fun onDiscoveryStarted(serviceType: String?) {}

            override fun onDiscoveryStopped(serviceType: String?) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { resolveService(it) }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
        }
        discoveryListener = listener
        runCatching {
            nsdManager?.discoverServices("_cassettecat._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        runCatching {
            nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                    resolvedInfo?.let { info ->
                        val host = info.host?.hostAddress ?: return@let
                        val port = info.port
                        val name = info.serviceName ?: "CassetteCat Player"
                        val device = DiscoveredDevice(
                            name = name,
                            host = host,
                            port = port,
                            connectionType = DeviceConnectionType.STATION
                        )
                        _pairingState.value = DevicePairingState.DeviceFound(device)
                    }
                }
            })
        }
    }

    private fun stopMdnsDiscovery() {
        discoveryListener?.let { listener ->
            runCatching { nsdManager?.stopServiceDiscovery(listener) }
        }
        discoveryListener = null
    }
}
