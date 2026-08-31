package `in`.caffeinelabs.cassettecat.data.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.PatternMatcher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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

private const val SOFT_AP_HOST = "192.168.4.1"
private const val SOFT_AP_PORT = 80
private const val SOFT_AP_SSID_PREFIX = "CassetteCat"

class WifiDevicePairingRepository(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val apiClient = CompanionApiClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _pairingState = MutableStateFlow<DevicePairingState>(DevicePairingState.SelectingMode)
    val pairingState: StateFlow<DevicePairingState> = _pairingState.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var searchJob: Job? = null
    private var boundNetwork: Network? = null

    // Lets sync reuse the SoftAP network binding without re-running discovery.
    val currentNetwork: Network? get() = boundNetwork

    private val canAutoAssociateSoftAp: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED

    fun startDiscovery(mode: DeviceConnectionType) {
        stopDiscovery()
        _pairingState.value = DevicePairingState.Searching(mode)

        when (mode) {
            DeviceConnectionType.SOFT_AP -> {
                if (canAutoAssociateSoftAp) startSoftApAutoAssociate(mode) else startSoftApManualPoll(mode)
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

    private fun startSoftApManualPoll(mode: DeviceConnectionType) {
        searchJob = scope.launch {
            var attempts = 0
            while (attempts < 10) {
                delay(1200)
                val status = apiClient.getStatus(SOFT_AP_HOST, SOFT_AP_PORT)
                if (status != null) {
                    _pairingState.value = DevicePairingState.DeviceFound(
                        DiscoveredDevice(name = status.deviceName, host = SOFT_AP_HOST, port = SOFT_AP_PORT, connectionType = DeviceConnectionType.SOFT_AP, status = status)
                    )
                    return@launch
                }
                attempts++
            }
            _pairingState.value = DevicePairingState.Failed(mode, "No CassetteCat device found on Wi-Fi hotspot.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startSoftApAutoAssociate(mode: DeviceConnectionType) {
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsidPattern(PatternMatcher(SOFT_AP_SSID_PREFIX, PatternMatcher.PATTERN_PREFIX))
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                boundNetwork = network
                searchJob = scope.launch {
                    var attempts = 0
                    while (attempts < 10) {
                        val status = apiClient.getStatus(SOFT_AP_HOST, SOFT_AP_PORT, network)
                        if (status != null) {
                            _pairingState.value = DevicePairingState.DeviceFound(
                                DiscoveredDevice(name = status.deviceName, host = SOFT_AP_HOST, port = SOFT_AP_PORT, connectionType = DeviceConnectionType.SOFT_AP, status = status)
                            )
                            return@launch
                        }
                        attempts++
                        delay(1200)
                    }
                    _pairingState.value = DevicePairingState.Failed(mode, "Connected to the hotspot, but the player didn't respond.")
                }
            }

            override fun onUnavailable() {
                _pairingState.value = DevicePairingState.Failed(mode, "Couldn't join the CassetteCat hotspot. Make sure the player is powered on.")
            }
        }
        networkCallback = callback
        connectivityManager.requestNetwork(request, callback, 20_000)
    }

    fun stopDiscovery() {
        searchJob?.cancel()
        searchJob = null
        stopMdnsDiscovery()
        networkCallback?.let { runCatching { connectivityManager.unregisterNetworkCallback(it) } }
        networkCallback = null
        boundNetwork = null
        _pairingState.value = DevicePairingState.SelectingMode
    }

    suspend fun connect(device: DiscoveredDevice) {
        _pairingState.value = DevicePairingState.Connecting(device)
        val network = boundNetwork.takeIf { device.connectionType == DeviceConnectionType.SOFT_AP }
        val status = apiClient.getStatus(device.host, device.port, network)
        _pairingState.value = if (status != null) {
            DevicePairingState.Connected(device.copy(status = status))
        } else {
            DevicePairingState.Failed(device.connectionType, "The player stopped responding.")
        }
    }

    suspend fun provisionWifi(device: DiscoveredDevice, ssid: String, passphrase: String): Boolean {
        val network = boundNetwork.takeIf { device.connectionType == DeviceConnectionType.SOFT_AP }
        return apiClient.provisionWifi(device.host, device.port, ssid, passphrase, network)
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

    fun release() {
        stopDiscovery()
        scope.cancel()
    }
}
