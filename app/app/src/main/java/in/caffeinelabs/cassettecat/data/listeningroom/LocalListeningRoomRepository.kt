package `in`.caffeinelabs.cassettecat.data.listeningroom

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.SystemClock
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
// No look-alike characters (I, O, 0, 1), so codes are easy to read aloud and type.
private const val ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private const val MAX_WIRE_LINE_LENGTH = 65536
private const val NO_JOIN_TIMEOUT_MS = 120_000L
private const val CONNECT_TIMEOUT_MS = 6_000
private const val MAX_RECONNECT_ATTEMPTS = 3
private const val RECONNECT_DELAY_MS = 2_000L
private const val CLOCK_PING_INTERVAL_MS = 2_000L
// Clocks drift a little, so only recent pings count.
private const val CLOCK_SAMPLES = 8
// Within this the guest plays at normal speed; beyond it a gentle speed change closes the gap.
private const val IN_SYNC_MS = 20L
// The fastest catch-up closes this within ten seconds; anything further out seeks.
private const val SEEK_DRIFT_MS = 400L
private const val MAX_CATCH_UP = 0.04f
private const val CATCH_UP_WINDOW_MS = 5_000f
internal const val MAX_LISTENING_ROOM_GUESTS = 8

private class HostGuest(val socket: Socket) {
    @Volatile var writer: BufferedWriter? = null
}

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
    val isPlaying: Boolean,
    val audioPort: Int? = null,
    val roomToken: String? = null,
    // The host's elapsedRealtime when positionMs was read, and its playback speed; absent from older hosts.
    val capturedAtMs: Long? = null,
    val speed: Float? = null
)

data class NearbyListeningRoom(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val roomCode: String
)

enum class ListeningRoomRole { NONE, HOST, GUEST }

data class ListeningRoomState(
    val role: ListeningRoomRole = ListeningRoomRole.NONE,
    val roomName: String? = null,
    val roomCode: String? = null,
    val hostAddress: String? = null,
    val participantCount: Int = 0,
    val nearbyRooms: List<NearbyListeningRoom> = emptyList(),
    val notice: String? = null
)

@Serializable
private data class WireMessage(
    val type: String,
    val snapshot: RoomSnapshot? = null,
    val sentAtMs: Long? = null,
    val hostAtMs: Long? = null
)

/** Estimates how far the host's elapsedRealtime is ahead of this phone's, from timed pings, the way NTP does. */
internal class HostClock {
    private val samples = ArrayDeque<Pair<Long, Long>>()

    /** The offset from the quickest recent ping, since a quick round trip leaves the least room for error. */
    @Synchronized
    fun offsetMs(): Long? = samples.minByOrNull { it.first }?.second

    @Synchronized
    fun addSample(sentAtMs: Long, hostAtMs: Long, receivedAtMs: Long) {
        val roundTripMs = receivedAtMs - sentAtMs
        if (roundTripMs < 0) return
        samples.addLast(roundTripMs to hostAtMs - (sentAtMs + roundTripMs / 2))
        if (samples.size > CLOCK_SAMPLES) samples.removeFirst()
    }
}

/**
 * How much faster or slower than the host a guest [driftMs] ahead of it (behind when negative) plays to get back
 * in step within ten seconds, or null when it should seek instead. A paused guest has nothing to catch up with,
 * so it seeks.
 */
internal fun roomCatchUpSpeed(driftMs: Long, playing: Boolean): Float? = when {
    kotlin.math.abs(driftMs) <= IN_SYNC_MS -> 1f
    !playing || kotlin.math.abs(driftMs) > SEEK_DRIFT_MS -> null
    else -> (1f - driftMs / CATCH_UP_WINDOW_MS).coerceIn(1f - MAX_CATCH_UP, 1f + MAX_CATCH_UP)
}

class LocalListeningRoomRepository(context: Context) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(NsdManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val hostGuests = CopyOnWriteArrayList<HostGuest>()
    private val guestSlots = java.util.concurrent.Semaphore(MAX_LISTENING_ROOM_GUESTS)
    private val discovered = linkedMapOf<String, NearbyListeningRoom>()
    private val rng = SecureRandom()

    private val _state = MutableStateFlow(ListeningRoomState())
    val state: StateFlow<ListeningRoomState> = _state.asStateFlow()
    private val _snapshots = MutableSharedFlow<RoomSnapshot>(extraBufferCapacity = 1)
    val snapshots: SharedFlow<RoomSnapshot> = _snapshots.asSharedFlow()

    private var serverSocket: ServerSocket? = null
    private var hostRegistration: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var discoveryJob: Job? = null
    private var guestSocket: Socket? = null
    private var guestWriter: BufferedWriter? = null
    @Volatile private var hostClock = HostClock()
    private var audioServer: RoomAudioServer? = null
    private var audioPort: Int? = null

    val guestHostAddress: String? get() = guestSocket?.inetAddress?.hostAddress

    /** The host's elapsedRealtime now, once a ping has measured the offset. */
    fun hostElapsedRealtimeMs(): Long? = hostClock.offsetMs()?.let { SystemClock.elapsedRealtime() + it }

    init {
        scope.launch {
            ServiceSettingsRepository(appContext).settings
                .map { it.offlineBlackoutMode }
                .distinctUntilChanged()
                .collect { offline ->
                    if (offline) {
                        stopDiscovery()
                        if (_state.value.role != ListeningRoomRole.NONE) {
                            stopRoom(appContext.getString(R.string.listening_room_blackout_closed))
                        }
                    }
                }
        }
    }

    fun startRoom(queueProvider: () -> List<Song>) {
        if (_state.value.role != ListeningRoomRole.NONE) return
        stopDiscovery()
        scope.launch {
            if (ServiceSettingsRepository(appContext).settings.first().offlineBlackoutMode) {
                _state.value = _state.value.copy(notice = appContext.getString(R.string.listening_room_blackout_disabled))
                return@launch
            }
            runCatching {
                val server = ServerSocket(0)
                serverSocket = server
                val code = buildRoomCode()
                val name = appContext.getString(R.string.listening_room_default_name, code)
                registerHost(name, code, server.localPort)
                val ip = localIpAddress()
                val relay = RoomAudioServer(appContext, code, queueProvider)
                audioServer = relay
                audioPort = relay.start()
                _state.value = ListeningRoomState(
                    role = ListeningRoomRole.HOST,
                    roomName = name,
                    roomCode = code,
                    hostAddress = ip?.let { "$it:${server.localPort}#$code" },
                    participantCount = 0
                )
                scope.launch {
                    delay(NO_JOIN_TIMEOUT_MS)
                    if (_state.value.role == ListeningRoomRole.HOST && _state.value.participantCount == 0) {
                        stopRoom(appContext.getString(R.string.listening_room_nobody_joined))
                    }
                }
                acceptGuests(server, code)
            }.onFailure { error ->
                stopRoom(appContext.getString(R.string.listening_room_start_failed))
            }
        }
    }

    @Suppress("DEPRECATION")
    fun findNearbyRooms() {
        if (_state.value.role != ListeningRoomRole.NONE || discoveryListener != null || discoveryJob != null) return
        discoveryJob = scope.launch {
            if (ServiceSettingsRepository(appContext).settings.first().offlineBlackoutMode) {
                discoveryJob = null
                _state.value = _state.value.copy(notice = appContext.getString(R.string.listening_room_blackout_disabled))
                return@launch
            }
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
                            port = info.port,
                            roomCode = info.attributes["room"]?.toString(Charsets.UTF_8).orEmpty()
                        )
                        if (room.roomCode.isBlank()) return
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
                _state.value = _state.value.copy(notice = appContext.getString(R.string.listening_room_discovery_unavailable))
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discoveryListener = listener
        discoveryJob = null
        runCatching { nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure {
                discoveryListener = null
                _state.value = _state.value.copy(notice = appContext.getString(R.string.listening_room_discovery_unavailable))
            }
        }
    }

    fun stopFindingNearbyRooms() = stopDiscovery()

    fun joinRoom(room: NearbyListeningRoom) {
        if (_state.value.role != ListeningRoomRole.NONE) return
        stopDiscovery()
        connectToHost(room.host, room.port, room.name, room.roomCode)
    }

    fun joinRoomManually(address: String) {
        if (_state.value.role != ListeningRoomRole.NONE) return
        val (endpoint, code) = address.trim().split("#", limit = 2).let {
            if (it.size == 2) it[0] to it[1] else return stopRoom(appContext.getString(R.string.listening_room_enter_address_code))
        }
        val (host, portText) = endpoint.split(":", limit = 2).let {
            if (it.size == 2) it[0] to it[1] else return stopRoom(appContext.getString(R.string.listening_room_enter_address))
        }
        val port = portText.toIntOrNull()
        if (host.isBlank() || port == null || code.isBlank()) {
            stopRoom(appContext.getString(R.string.listening_room_enter_address_code))
            return
        }
        stopDiscovery()
        connectToHost(host, port, appContext.getString(R.string.listening_room_name), code)
    }

    private fun connectToHost(host: String, port: Int, roomName: String, roomCode: String) {
        scope.launch {
            if (ServiceSettingsRepository(appContext).settings.first().offlineBlackoutMode) {
                stopRoom(appContext.getString(R.string.listening_room_blackout_disabled))
                return@launch
            }
            val connected = runCatching { openGuestSocket(host, port, roomName, roomCode) }
                .onFailure { error -> stopRoom(appContext.getString(R.string.listening_room_join_failed)) }
                .isSuccess
            if (connected) maintainGuestConnection(host, port, roomName, roomCode)
        }
    }

    private fun openGuestSocket(host: String, port: Int, roomName: String, roomCode: String) {
        val socket = Socket()
        var writer: BufferedWriter? = null
        var joined = false
        try {
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            writer.write(roomCode)
            writer.newLine()
            writer.flush()
            guestSocket = socket
            guestWriter = writer
            hostClock = HostClock()
            pingHost(socket, writer)
            _state.value = ListeningRoomState(
                role = ListeningRoomRole.GUEST,
                roomName = roomName,
                roomCode = roomCode,
                participantCount = 1,
                notice = appContext.getString(R.string.listening_room_following_notice)
            )
            joined = true
        } finally {
            if (!joined) {
                writer?.let(::closeQuietly)
                runCatching { socket.close() }
            }
        }
    }

    private suspend fun maintainGuestConnection(host: String, port: Int, roomName: String, roomCode: String) {
        while (true) {
            val socket = guestSocket ?: return
            runCatching { readGuestMessages(socket) }
            if (_state.value.role != ListeningRoomRole.GUEST) return

            var reconnected = false
            for (attempt in 1..MAX_RECONNECT_ATTEMPTS) {
                _state.value = _state.value.copy(notice = appContext.getString(R.string.listening_room_reconnecting))
                delay(RECONNECT_DELAY_MS)
                if (runCatching { openGuestSocket(host, port, roomName, roomCode) }.isSuccess) {
                    reconnected = true
                    break
                }
            }
            if (!reconnected) {
                stopRoom(appContext.getString(R.string.listening_room_lost_host))
                return
            }
        }
    }

    private fun pingHost(socket: Socket, writer: BufferedWriter) {
        scope.launch {
            while (guestSocket === socket) {
                val line = json.encodeToString(WireMessage(type = "ping", sentAtMs = SystemClock.elapsedRealtime()))
                val sent = runCatching { synchronized(writer) { writer.write(line); writer.newLine(); writer.flush() } }
                if (sent.isFailure) return@launch
                delay(CLOCK_PING_INTERVAL_MS)
            }
        }
    }

    fun publish(snapshot: RoomSnapshot) {
        if (_state.value.role != ListeningRoomRole.HOST) return
        val line = json.encodeToString(
            WireMessage(type = "snapshot", snapshot = snapshot.copy(audioPort = audioPort, roomToken = _state.value.roomCode))
        ) + "\n"
        scope.launch {
            hostGuests.toList().forEach { guest ->
                val writer = guest.writer ?: return@forEach
                runCatching { synchronized(writer) { writer.write(line); writer.flush() } }
                    .onFailure { removeHostGuest(guest) }
            }
            updateParticipantCount()
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

    private fun acceptGuests(server: ServerSocket, roomCode: String) {
        while (!server.isClosed) {
            runCatching { server.accept() }.getOrNull()?.let { socket ->
                if (!guestSlots.tryAcquire()) {
                    runCatching { socket.close() }
                    return@let
                }
                val guest = HostGuest(socket)
                hostGuests += guest
                scope.launch {
                    runCatching {
                        socket.soTimeout = CONNECT_TIMEOUT_MS
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val suppliedCode = reader.readLineBounded(64)
                        if (suppliedCode != roomCode) throw IOException("Invalid room code")
                        socket.soTimeout = 0
                        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                        guest.writer = writer
                        updateParticipantCount()
                        // Reading until the socket closes also notices a guest that left at once.
                        while (true) {
                            val line = reader.readLineBounded(MAX_WIRE_LINE_LENGTH) ?: break
                            val ping = runCatching { json.decodeFromString<WireMessage>(line) }.getOrNull()
                                ?.takeIf { it.type == "ping" } ?: continue
                            val pong = json.encodeToString(
                                WireMessage(
                                    type = "pong",
                                    sentAtMs = ping.sentAtMs,
                                    hostAtMs = SystemClock.elapsedRealtime()
                                )
                            )
                            synchronized(writer) { writer.write(pong); writer.newLine(); writer.flush() }
                        }
                    }
                    removeHostGuest(guest)
                }
            }
        }
    }

    private fun readGuestMessages(socket: Socket) {
        BufferedReader(InputStreamReader(socket.getInputStream())).use { reader ->
            while (true) {
                val line = reader.readLineBounded(MAX_WIRE_LINE_LENGTH) ?: return
                val message = runCatching { json.decodeFromString<WireMessage>(line) }.getOrNull() ?: continue
                when (message.type) {
                    "snapshot" -> message.snapshot?.let(_snapshots::tryEmit)
                    "pong" -> {
                        val sentAtMs = message.sentAtMs ?: continue
                        val hostAtMs = message.hostAtMs ?: continue
                        hostClock.addSample(sentAtMs, hostAtMs, SystemClock.elapsedRealtime())
                    }
                }
            }
        }
    }

    private fun stopRoom(notice: String?) {
        stopDiscovery()
        hostRegistration?.let { listener -> runCatching { nsdManager.unregisterService(listener) } }
        hostRegistration = null
        audioServer?.stop()
        audioServer = null
        audioPort = null
        hostGuests.toList().forEach(::removeHostGuest)
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
        discoveryJob?.cancel()
        discoveryJob = null
        discoveryListener?.let { listener -> runCatching { nsdManager.stopServiceDiscovery(listener) } }
        discoveryListener = null
        discovered.clear()
    }

    fun release() {
        stopRoom(null)
        scope.cancel()
    }

    private fun buildRoomCode(): String = (1..6).joinToString("") {
        ROOM_CODE_ALPHABET[rng.nextInt(ROOM_CODE_ALPHABET.length)].toString()
    }

    private fun localIpAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()

    private fun closeQuietly(writer: BufferedWriter) = runCatching { writer.close() }

    private fun removeHostGuest(guest: HostGuest) {
        if (!hostGuests.remove(guest)) return
        guest.writer?.let(::closeQuietly)
        runCatching { guest.socket.close() }
        guestSlots.release()
        updateParticipantCount()
    }

    private fun updateParticipantCount() {
        if (_state.value.role == ListeningRoomRole.HOST) {
            _state.value = _state.value.copy(participantCount = hostGuests.count { it.writer != null })
        }
    }
}

internal fun BufferedReader.readLineBounded(maxLength: Int): String? {
    val result = StringBuilder()
    while (true) {
        when (val char = read()) {
            -1 -> return result.takeIf { it.isNotEmpty() }?.toString()
            '\n'.code -> return result.toString().removeSuffix("\r")
            else -> {
                if (result.length >= maxLength) throw IOException("Line exceeds $maxLength characters")
                result.append(char.toChar())
            }
        }
    }
}
