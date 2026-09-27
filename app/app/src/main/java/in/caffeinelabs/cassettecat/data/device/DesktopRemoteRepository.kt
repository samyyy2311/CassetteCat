package `in`.caffeinelabs.cassettecat.data.device

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackUiState
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.desktopRemoteDataStore by preferencesDataStore(name = "desktop_remote")
private val DESKTOP_ADDRESS = stringPreferencesKey("address")
private val DESKTOP_NAME = stringPreferencesKey("name")
// Whether this phone is controlling the desktop, as when a device is picked in Spotify Connect.
private val DESKTOP_ACTIVE = booleanPreferencesKey("active")
private const val QUEUE_SONG_PREFIX = "desktop:queue:"

data class DesktopAddress(val host: String, val port: Int, val code: String)

/** Parses the "ip:port#CODE" address shown in the desktop app's Phone Remote settings. */
internal fun parseDesktopAddress(text: String): DesktopAddress? {
    val parts = text.trim().split("#", limit = 2)
    if (parts.size != 2) return null
    val host = parts[0].substringBeforeLast(':', "")
    val port = parts[0].substringAfterLast(':').toIntOrNull()
    val code = parts[1].trim().uppercase()
    if (host.isBlank() || port == null || port !in 1..65535 || code.isEmpty()) return null
    return DesktopAddress(host, port, code)
}

/** Finds [wanted] in [library] by title and artist, in order; empty when the first song is not there. */
internal fun matchInLibrary(wanted: List<Song>, library: List<Song>): List<Song> {
    fun key(song: Song) = song.title.trim().lowercase() + "\u001f" + song.artist.trim().lowercase()
    val byKey = library.asReversed().associateBy(::key)
    if (wanted.firstOrNull()?.let { byKey[key(it)] } == null) return emptyList()
    return wanted.mapNotNull { byKey[key(it)] }
}

enum class PairingResult { PAIRED, INVALID_ADDRESS, WRONG_CODE, UNREACHABLE }

data class DesktopRemoteState(
    val loaded: Boolean = false,
    val address: DesktopAddress? = null,
    val name: String? = null,
    val active: Boolean = false,
    val offlineBlackout: Boolean = false
) {
    val controlling: Boolean get() = active && address != null && !offlineBlackout
}

/**
 * The paired CassetteCat desktop app. While the phone controls it, [controlledState] presents it as a player,
 * so the app's own mini player and Now Playing screen show and drive the computer.
 */
class DesktopRemoteRepository private constructor(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dataStore = context.desktopRemoteDataStore
    private val apiClient = DeviceControlApiClient(onCodeRejected = ::codeRejected)
    private val playbackRepository = DevicePlaybackRepository(apiClient)

    val state: StateFlow<DesktopRemoteState> = combine(
        dataStore.data,
        ServiceSettingsRepository(context).settings
    ) { prefs, services ->
        DesktopRemoteState(
            loaded = true,
            address = prefs[DESKTOP_ADDRESS]?.let(::parseDesktopAddress),
            name = prefs[DESKTOP_NAME],
            active = prefs[DESKTOP_ACTIVE] ?: false,
            offlineBlackout = services.offlineBlackoutMode
        )
    }.stateIn(scope, SharingStarted.Eagerly, DesktopRemoteState())

    val status: StateFlow<DevicePlaybackStatus?> = playbackRepository.status

    private val upNext = MutableStateFlow<List<DesktopQueueTrack>>(emptyList())
    private val _transferRequests = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    /**
     * Where playback should move: true for the computer, false for this phone. The device sheet, Android's output
     * switcher and the computer itself ask here, and one place in the app carries it out.
     */
    val transferRequests: SharedFlow<Boolean> = _transferRequests.asSharedFlow()
    private val _found = MutableStateFlow<List<DiscoveredDesktop>>(emptyList())
    val found: StateFlow<List<DiscoveredDesktop>> = _found.asStateFlow()

    val controlledState: StateFlow<PlaybackUiState?> = combine(state, status, upNext) { state, status, queue ->
        status?.takeIf { state.controlling && it.trackTitle.isNotEmpty() }?.let { current ->
            PlaybackUiState(
                currentSong = songFor(current),
                isPlaying = current.isPlaying,
                playWhenReady = current.isPlaying,
                durationMs = current.durationMs,
                isShuffleEnabled = current.shuffleEnabled,
                repeatMode = when (current.repeatMode) {
                    1 -> Player.REPEAT_MODE_ALL
                    2 -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                },
                upNext = queue.map { it.toSong() }
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    // Status arrives every couple of seconds; the position advances locally in between.
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    // The player and the device sheet can both be polling; the last one out stops it.
    private var pollers = 0

    // With the app on screen the computer's changes show at once; the notification alone can wait a little.
    private val _inFront = MutableStateFlow(false)
    val isInFront: Boolean get() = _inFront.value

    fun setInFront(inFront: Boolean) {
        _inFront.value = inFront
    }

    init {
        scope.launch {
            combine(state.map { it.address }, _inFront) { address, inFront -> address to inFront }.distinctUntilChanged().collect {
                if (pollers > 0) {
                    playbackRepository.stopPolling()
                    pollCurrentDesktop()
                }
            }
        }
        scope.launch {
            status.filterNotNull().map { it.trackTitle }.distinctUntilChanged().collect { refreshQueue() }
        }
        scope.launch {
            status.filterNotNull().collect { if (it.handoffRequested && state.value.controlling) requestTransfer(toDesktop = false) }
        }
        // A computer paired by typing its address is named once it answers.
        scope.launch {
            status.filterNotNull().map { it.deviceName }.distinctUntilChanged().collect { name ->
                if (!name.isNullOrBlank() && name != state.value.name) dataStore.edit { it[DESKTOP_NAME] = name }
            }
        }
        scope.launch {
            status.collectLatest { current ->
                current ?: return@collectLatest
                val receivedAt = SystemClock.elapsedRealtime()
                do {
                    val elapsed = if (current.isPlaying) SystemClock.elapsedRealtime() - receivedAt else 0L
                    _positionMs.value = (current.positionMs + elapsed).coerceAtMost(current.durationMs)
                    delay(250)
                } while (current.isPlaying)
            }
        }
    }

    // The cover URL carries the pairing code because image loaders cannot send headers; the key changes with the cover.
    private fun artworkUri(key: String?): Uri? {
        val desktop = connectedDesktop() ?: return null
        if (key.isNullOrEmpty()) return null
        return Uri.parse("http://${desktop.host}:${desktop.port}/api/artwork?key=$key&code=${desktop.code}")
    }

    private fun songFor(status: DevicePlaybackStatus): Song {
        return Song(
            id = "desktop:${status.trackTitle}",
            title = status.trackTitle,
            artist = status.trackArtist,
            album = "",
            albumId = "",
            durationMs = status.durationMs,
            contentUri = Uri.EMPTY,
            source = MusicSource.Desktop,
            artUri = artworkUri(status.artworkKey)
        )
    }

    private fun DesktopQueueTrack.toSong() = Song(
        id = "$QUEUE_SONG_PREFIX$index",
        title = title,
        artist = artist,
        album = "",
        albumId = "",
        durationMs = durationMs,
        contentUri = Uri.EMPTY,
        source = MusicSource.Desktop,
        artUri = artworkUri(artworkKey)
    )

    private suspend fun refreshQueue() {
        val desktop = connectedDesktop() ?: return
        apiClient.getQueue(desktop.host, desktop.port, desktop.code)?.let { upNext.value = it }
    }

    fun discover() {
        scope.launch {
            val desktops = discoverDesktops()
            _found.value = desktops
            // A computer that got a new address from the router is followed by name.
            val current = state.value
            val moved = desktops.firstOrNull { it.name == current.name } ?: return@launch
            val address = current.address ?: return@launch
            if (moved.host != address.host || moved.port != address.port) save("${moved.host}:${moved.port}#${address.code}", moved.name)
        }
    }

    /** Pairs with the "ip:port#CODE" address typed by hand. */
    suspend fun pair(text: String): PairingResult = pairAddress(text.trim(), name = null)

    // The computer's copy button gives the whole address, so a pasted one contributes just its code.
    suspend fun pair(desktop: DiscoveredDesktop, code: String): PairingResult =
        pairAddress("${desktop.host}:${desktop.port}#${code.substringAfterLast('#').trim()}", desktop.name)

    // The code is checked first, so a mistyped one is caught here instead of failing quietly afterwards.
    private suspend fun pairAddress(text: String, name: String?): PairingResult {
        val address = parseDesktopAddress(text) ?: return PairingResult.INVALID_ADDRESS
        return when (apiClient.acceptsPairingCode(address.host, address.port, address.code)) {
            true -> PairingResult.PAIRED.also { save(text, name, controlling = true) }
            false -> PairingResult.WRONG_CODE
            null -> PairingResult.UNREACHABLE
        }
    }

    // A computer given a new code refuses the old one. Dropping it stops the retries, which would otherwise lock
    // everyone out, and the kept name lets the pairing screen ask for the new code.
    private fun codeRejected(code: String) {
        scope.launch {
            dataStore.edit {
                if (it[DESKTOP_ADDRESS]?.let(::parseDesktopAddress)?.code == code) {
                    it.remove(DESKTOP_ADDRESS)
                    it[DESKTOP_ACTIVE] = false
                }
            }
        }
    }

    private fun save(address: String, name: String?, controlling: Boolean? = null) {
        scope.launch {
            dataStore.edit {
                it[DESKTOP_ADDRESS] = address
                if (name != null) it[DESKTOP_NAME] = name else it.remove(DESKTOP_NAME)
                if (controlling != null) it[DESKTOP_ACTIVE] = controlling
            }
        }
    }

    fun requestTransfer(toDesktop: Boolean) {
        _transferRequests.tryEmit(toDesktop)
    }

    fun setControlling(controlling: Boolean) {
        scope.launch { dataStore.edit { it[DESKTOP_ACTIVE] = controlling } }
    }

    fun forget() {
        playbackRepository.stopPolling()
        scope.launch { dataStore.edit { it.clear() } }
    }

    private fun connectedDesktop(): DesktopAddress? = state.value.takeUnless { it.offlineBlackout }?.address

    fun startPolling() {
        if (pollers++ == 0) pollCurrentDesktop()
    }

    fun stopPolling() {
        if (pollers > 0 && --pollers == 0) playbackRepository.stopPolling()
    }

    private fun pollCurrentDesktop() {
        val desktop = connectedDesktop() ?: return
        // A LAN round trip is cheap, and the desktop's own controls should show up here quickly.
        val intervalMs = if (isInFront) 1_000L else 3_000L
        playbackRepository.startPolling(desktop.host, desktop.port, null, desktop.code, intervalMs)
    }

    fun sendAction(action: String) {
        val desktop = connectedDesktop() ?: return
        playbackRepository.sendAction(desktop.host, desktop.port, action, null, desktop.code)
    }

    fun setVolume(percent: Int) {
        val desktop = connectedDesktop() ?: return
        playbackRepository.setVolume(desktop.host, desktop.port, percent, null, desktop.code)
    }

    fun seek(positionMs: Long) {
        val desktop = connectedDesktop() ?: return
        _positionMs.value = positionMs
        playbackRepository.seek(desktop.host, desktop.port, positionMs, null, desktop.code)
    }

    /** Reports what this phone plays to the paired computer; returns the commands the computer sent back. */
    suspend fun checkIn(song: Song, isPlaying: Boolean): List<String> {
        val desktop = connectedDesktop() ?: return emptyList()
        return apiClient.checkIn(desktop.host, desktop.port, desktop.code, PhoneCheckIn(song.title, song.artist, isPlaying))
    }

    /** Continues [songs] on the computer from [positionMs]; it plays the ones its own library has. Returns whether it accepted them. */
    suspend fun handOff(songs: List<Song>, positionMs: Long, playing: Boolean): Boolean {
        val desktop = connectedDesktop() ?: return false
        val tracks = songs.map { HandoffTrack(it.title, it.artist) }
        return apiClient.handOff(desktop.host, desktop.port, desktop.code, tracks, positionMs, playing)
    }

    private fun queueIndex(song: Song): Int? = song.id.removePrefix(QUEUE_SONG_PREFIX).takeIf { it != song.id }?.toIntOrNull()

    /** Plays [song] from the desktop's queue; returns false for songs that are not from it. */
    fun playFromQueue(song: Song): Boolean {
        val index = queueIndex(song) ?: return false
        val desktop = connectedDesktop() ?: return true
        scope.launch { apiClient.playQueueTrack(desktop.host, desktop.port, index, desktop.code) }
        return true
    }

    /** Moves the up-next song at [fromIndex] to [toIndex] on the desktop, showing the change at once. */
    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val queue = upNext.value
        val from = queue.getOrNull(fromIndex) ?: return
        val to = queue.getOrNull(toIndex) ?: return
        val desktop = connectedDesktop() ?: return
        upNext.value = queue.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        scope.launch {
            apiClient.moveQueueTrack(desktop.host, desktop.port, from.index, to.index, desktop.code)
            refreshQueue()
        }
    }

    /** Removes [song] from the desktop's queue; returns false for songs that are not from it. */
    fun removeFromQueue(song: Song): Boolean {
        val index = queueIndex(song) ?: return false
        val desktop = connectedDesktop() ?: return true
        upNext.value = upNext.value.filterNot { it.index == index }
        scope.launch {
            apiClient.removeQueueTrack(desktop.host, desktop.port, index, desktop.code)
            refreshQueue()
        }
        return true
    }

    companion object {
        @Volatile private var instance: DesktopRemoteRepository? = null

        fun getInstance(context: Context): DesktopRemoteRepository =
            instance ?: synchronized(this) {
                instance ?: DesktopRemoteRepository(context.applicationContext).also { instance = it }
            }
    }
}
