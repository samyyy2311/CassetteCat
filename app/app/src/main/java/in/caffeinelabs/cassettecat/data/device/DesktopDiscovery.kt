package `in`.caffeinelabs.cassettecat.data.device

import android.os.SystemClock
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// Must match the desktop app's RemoteControlServer.
private const val DISCOVERY_PORT = 47800
private const val DISCOVERY_PROBE = "CASSETTECAT_DISCOVER"
private val LOCAL_INTERFACE_PREFIXES = listOf("wlan", "swlan", "eth", "ap")

@Serializable
private data class DiscoveryReply(val name: String, val port: Int)

data class DiscoveredDesktop(val name: String, val host: String, val port: Int)

internal fun parseDiscoveryReply(payload: String, host: String): DiscoveredDesktop? =
    runCatching { sharedJson.decodeFromString<DiscoveryReply>(payload) }.getOrNull()
        ?.takeIf { it.name.isNotBlank() && it.port in 1..65535 }
        ?.let { DiscoveredDesktop(it.name, host, it.port) }

// Many routers and phones drop the global broadcast, so the probe also goes to each network's own broadcast
// address and, on home-sized networks, straight to every host. Only Wi-Fi and Ethernet are probed, never
// mobile data or a VPN.
private fun probeTargets(): List<InetAddress> {
    val targets = mutableListOf(InetAddress.getByName("255.255.255.255"))
    NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        .filter { it.isUp && !it.isLoopback && LOCAL_INTERFACE_PREFIXES.any(it.name::startsWith) }
        .flatMap { it.interfaceAddresses }
        .filter { it.address is Inet4Address }
        .forEach { entry ->
            entry.broadcast?.let(targets::add)
            if (entry.networkPrefixLength in 24..30) {
                val base = ByteBuffer.wrap(entry.address.address).int and (-1 shl (32 - entry.networkPrefixLength))
                val hosts = (1 shl (32 - entry.networkPrefixLength)) - 2
                for (offset in 1..hosts) {
                    targets += InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(base + offset).array())
                }
            }
        }
    return targets.distinct()
}

/** Broadcasts on the local network and collects the computers running CassetteCat that answer. */
suspend fun discoverDesktops(timeoutMs: Long = 1_500L): List<DiscoveredDesktop> = withContext(Dispatchers.IO) {
    runCatching {
        DatagramSocket().use { socket ->
            socket.broadcast = true
            val probe = DISCOVERY_PROBE.toByteArray()
            for (target in probeTargets()) {
                runCatching { socket.send(DatagramPacket(probe, probe.size, target, DISCOVERY_PORT)) }
            }
            val found = linkedMapOf<String, DiscoveredDesktop>()
            val buffer = ByteArray(512)
            val deadline = SystemClock.elapsedRealtime() + timeoutMs
            while (true) {
                val remaining = deadline - SystemClock.elapsedRealtime()
                if (remaining <= 0) break
                socket.soTimeout = remaining.toInt()
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    break
                }
                val host = packet.address.hostAddress ?: continue
                parseDiscoveryReply(String(packet.data, 0, packet.length), host)?.let { found["$host:${it.port}"] = it }
            }
            found.values.toList()
        }
    }.getOrDefault(emptyList())
}
