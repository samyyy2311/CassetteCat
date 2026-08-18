package `in`.caffeinelabs.cassettecat.data.listeningroom

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val SERVICE_TYPE = "_cassettecat-room._tcp."
private const val MAX_WIRE_LINE_LENGTH = 65536

@Serializable
data class RoomTrack(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long
)

@Serializable
data class RoomSnapshot(
    val tracks: List<RoomTrack>,
    val positionMs: Long,
    val isPlaying: Boolean
)

data class NearbyListeningRoom(
    val id: String,
    val name: String,
    val host: String,
    val port: Int
)

enum class ListeningRoomRole { NONE, HOST, GUEST }

data class ListeningRoomState(
    val role: ListeningRoomRole = ListeningRoomRole.NONE,
    val roomName: String? = null,
    val roomCode: String? = null,
    val participantCount: Int = 0,
    val nearbyRooms: List<NearbyListeningRoom> = emptyList(),
    val notice: String? = null
)

@Serializable
private data class WireMessage(
    val type: String,
    val snapshot: RoomSnapshot? = null
)

/**
 * Direct, LAN-only room transport. Discovery uses Android NSD/mDNS and every socket is
 * opened directly between the host and guests. It deliberately has no account, relay,
 * persistence, analytics, or internet endpoint.
 */
class LocalListeningRoomRepository(context: Context) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(NsdManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val writers = CopyOnWriteArrayList<BufferedWriter>()
    private val discovered = linkedMapOf<String, NearbyListeningRoom>()

    private val _state = MutableStateFlow(ListeningRoomState())
    val state: StateFlow<ListeningRoomState> = _state.asStateFlow()
    private val _snapshots = MutableSharedFlow<RoomSnapshot>(extraBufferCapacity = 1)
    val snapshots: SharedFlow<RoomSnapshot> = _snapshots.asSharedFlow()

    private var serverSocket: ServerSocket? = null
    private var hostRegistration: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var guestSocket: Socket? = null
    private var guestWriter: BufferedWriter? = null

    fun startRoom() {
        if (_state.value.role != ListeningRoomRole.NONE) return
        scope.launch {
            runCatching {
                val server = ServerSocket(0)
                serverSocket = server
                val code = buildRoomCode()
                val name = "Listening Room $code"
                registerHost(name, code, server.localPort)
                _state.value = ListeningRoomState(
                    role = ListeningRoomRole.HOST,
                    roomName = name,
                    roomCode = code,
                    participantCount = 0
                )
                acceptGuests(server)
            }.onFailure { error ->
                stopRoom("Couldn’t start a nearby room: ${error.message ?: "network unavailable"}")
            }
        }
    }

    @Suppress("DEPRECATION")
    fun findNearbyRooms() {
        if (_state.value.role != ListeningRoomRole.NONE || discoveryListener != null) return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val hostAddress = info.host?.hostAddress ?: return
                        val room = NearbyListeningRoom(
                            id = "$hostAddress:${info.port}:${info.serviceName}",
                            name = info.serviceName,
                            host = hostAddress,
                            port = info.port
                        )
                        discovered[room.id] = room
                        _state.value = _state.value.copy(nearbyRooms = discovered.values.toList(), notice = null)
                    }
                })
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val current = discovered.filterValues { it.name == serviceInfo.serviceName }.keys
                current.forEach(discovered::remove)
                _state.value = _state.value.copy(nearbyRooms = discovered.values.toList())
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                discoveryListener = null
                _state.value = _state.value.copy(notice = "Nearby room discovery is unavailable on this network.")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discoveryListener = listener
        runCatching { nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure {
                discoveryListener = null
                _state.value = _state.value.copy(notice = "Nearby room discovery is unavailable on this network.")
            }
    }

    fun joinRoom(room: NearbyListeningRoom) {
        if (_state.value.role != ListeningRoomRole.NONE) return
        stopDiscovery()
        scope.launch {
            runCatching {
                // The room may remain idle for a while before the host changes playback, so
                // only connect has a timeout; reads intentionally stay open.
                val socket = Socket(room.host, room.port)
                guestSocket = socket
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                guestWriter = writer
                _state.value = ListeningRoomState(
                    role = ListeningRoomRole.GUEST,
                    roomName = room.name,
                    participantCount = 1,
                    notice = "Following the host on this Wi-Fi network."
                )
                readGuestMessages(socket)
            }.onFailure { error ->
                stopRoom("Couldn’t join this room: ${error.message ?: "connection failed"}")
            }
        }
    }

    /** Hosts publish the current track, queue and position directly to every joined guest. */
    fun publish(snapshot: RoomSnapshot) {
        if (_state.value.role != ListeningRoomRole.HOST) return
        val line = json.encodeToString(WireMessage(type = "snapshot", snapshot = snapshot)) + "\n"
        scope.launch {
            writers.toList().forEach { writer ->
                runCatching { writer.write(line); writer.flush() }
                    .onFailure { writers.remove(writer); closeQuietly(writer) }
            }
            _state.value = _state.value.copy(participantCount = writers.size)
        }
    }

    fun leaveRoom() = stopRoom(null)

    private fun registerHost(name: String, code: String, port: Int) {
        val service = NsdServiceInfo().apply {
            serviceName = name
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("room", code)
            setAttribute("version", "1")
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
        }
        hostRegistration = listener
        nsdManager.registerService(service, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun acceptGuests(server: ServerSocket) {
        while (!server.isClosed) {
            runCatching { server.accept() }.getOrNull()?.let { socket ->
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                writers += writer
                _state.value = _state.value.copy(participantCount = writers.size)
                scope.launch {
                    // Guests are followers. Keep the socket open solely for host snapshots;
                    // no guest command or listening data is collected.
                    runCatching { BufferedReader(InputStreamReader(socket.getInputStream())).readLines() }
                    writers.remove(writer)
                    closeQuietly(writer)
                    runCatching { socket.close() }
                    _state.value = _state.value.copy(participantCount = writers.size)
                }
            }
        }
    }

    private fun readGuestMessages(socket: Socket) {
        BufferedReader(InputStreamReader(socket.getInputStream())).useLines { lines ->
            lines.forEach { line ->
                if (line.length > MAX_WIRE_LINE_LENGTH) return@forEach
                val message = runCatching { json.decodeFromString<WireMessage>(line) }.getOrNull() ?: return@forEach
                if (message.type == "snapshot") message.snapshot?.let(_snapshots::tryEmit)
            }
        }
    }

    private fun stopRoom(notice: String?) {
        stopDiscovery()
        hostRegistration?.let { listener -> runCatching { nsdManager.unregisterService(listener) } }
        hostRegistration = null
        writers.forEach(::closeQuietly)
        writers.clear()
        guestWriter?.let(::closeQuietly)
        guestWriter = null
        guestSocket?.let { runCatching { it.close() } }
        guestSocket = null
        serverSocket?.let { runCatching { it.close() } }
        serverSocket = null
        discovered.clear()
        _state.value = ListeningRoomState(notice = notice)
    }

    private fun stopDiscovery() {
        discoveryListener?.let { listener -> runCatching { nsdManager.stopServiceDiscovery(listener) } }
        discoveryListener = null
        discovered.clear()
    }

    fun release() {
        stopRoom(null)
        scope.cancel()
    }

    private fun buildRoomCode(): String = (1..6).joinToString("") {
        "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[SecureRandom().nextInt(30)].toString()
    }

    private fun closeQuietly(writer: BufferedWriter) = runCatching { writer.close() }
}
