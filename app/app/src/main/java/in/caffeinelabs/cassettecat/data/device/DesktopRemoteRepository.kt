package `in`.caffeinelabs.cassettecat.data.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.Uri
import androidx.core.net.toUri
import android.os.SystemClock
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.songMatchKey
import `in`.caffeinelabs.cassettecat.data.library.local.LocalLibraryRepository
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackUiState
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.stats.Listen
import `in`.caffeinelabs.cassettecat.data.stats.statsSongId
import `in`.caffeinelabs.cassettecat.data.stats.ListeningStatsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.YearMonth
import java.time.ZoneId

private val Context.desktopRemoteDataStore by preferencesDataStore(name = "desktop_remote")
private val DESKTOP_ADDRESS = stringPreferencesKey("address")
private val DESKTOP_NAME = stringPreferencesKey("name")
// Whether this phone is controlling the desktop, as when a device is picked in Spotify Connect.
private val DESKTOP_ACTIVE = booleanPreferencesKey("active")
private val DESKTOP_LAST_BACKUP = longPreferencesKey("last_backup_at")
private val DESKTOP_AGREED_LIKES = stringSetPreferencesKey("agreed_likes")
private val DESKTOP_PENDING_LISTENS = stringPreferencesKey("pending_listens")
private val DESKTOP_LISTENS_SINCE = longPreferencesKey("listens_since")
// The pairing code of the computer that has this phone's whole listening history.
private val DESKTOP_HISTORY_SENT_TO = stringPreferencesKey("listen_history_sent_to")
// The pairing code of the computer that has the monthly totals from before single listens were kept.
private val DESKTOP_TOTALS_SENT_TO = stringPreferencesKey("listen_totals_sent_to")
private const val MAX_PENDING_LISTENS = 2_000
private const val QUEUE_SONG_PREFIX = "desktop:queue:"
private const val BACKUP_INTERVAL_MS = 24 * 60 * 60 * 1000L
private const val LIKES_SYNC_DELAY_MS = 2_000L
private const val COMPUTER_LIBRARY_PAGE = 100
private const val REFIND_INTERVAL_MS = 30_000L
private const val NETWORK_SETTLE_MS = 1_000L
private const val APPROVAL_WAIT_MS = 65_000L
private const val APPROVAL_POLL_MS = 1_000L
private const val APPROVAL_MAX_MISSED_CHECKS = 5

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
    fun key(song: Song) = songMatchKey(song.title, song.artist)
    val byKey = library.asReversed().associateBy(::key)
    if (wanted.firstOrNull()?.let { byKey[key(it)] } == null) return emptyList()
    return wanted.mapNotNull { byKey[key(it)] }
}

internal data class LikesSyncPlan(
    val likeOnPhone: Set<String>,
    val unlikeOnPhone: Set<String>,
    val likeOnDesktop: Set<String>,
    val unlikeOnDesktop: Set<String>,
    val agreed: Set<String>
)

internal fun planLikesSync(phoneLiked: Set<String>, desktopLiked: Set<String>, shared: Set<String>, lastAgreed: Set<String>?): LikesSyncPlan {
    val phone = phoneLiked intersect shared
    val desktop = desktopLiked intersect shared
    val agreed = if (lastAgreed == null) phone + desktop
    else (phone intersect desktop) + ((phone + desktop) - lastAgreed) + (lastAgreed - shared)
    return LikesSyncPlan(agreed - phone, phone - agreed, agreed - desktop, desktop - agreed, agreed)
}

internal fun findAllInLibrary(tracks: List<HandoffTrack>, library: List<Song>): List<Song> {
    val byKey = library.asReversed().associateBy { songMatchKey(it.title, it.artist) }
    return tracks.mapNotNull { byKey[songMatchKey(it.title, it.artist)] }.distinct()
}

enum class PairingResult { PAIRED, INVALID_ADDRESS, WRONG_CODE, UNREACHABLE }

enum class ApprovalResult { PAIRED, NOT_ALLOWED, UNREACHABLE, UNSUPPORTED }

data class DesktopRemoteState(
    val loaded: Boolean = false,
    val address: DesktopAddress? = null,
    val name: String? = null,
    val active: Boolean = false,
    val offlineBlackout: Boolean = false,
    val lastBackupAtMs: Long? = null
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
    private val apiClient = DeviceControlApiClient(onCodeRejected = ::codeRejected, onUnreachable = ::refindSoon)
    private var lastRefindAtMs = 0L
    private val localLibrary = LocalLibraryRepository(context)
    private val favoritesRepository = FavoritesRepository(context)
    private val likesSync = Mutex()
    private val listensSync = Mutex()
    private val appPreferences = AppPreferencesRepository(context)
    private val statsRepository = ListeningStatsRepository(context)
    private var syncedLikesRevision: Int? = null
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
            offlineBlackout = services.offlineBlackoutMode,
            lastBackupAtMs = prefs[DESKTOP_LAST_BACKUP]
        )
    }.stateIn(scope, SharingStarted.Eagerly, DesktopRemoteState())

    val status: StateFlow<DevicePlaybackStatus?> = playbackRepository.status

    private val upNext = MutableStateFlow<List<DesktopQueueTrack>>(emptyList())
    private val _transferRequest = MutableStateFlow<Boolean?>(null)
    /**
     * Where playback is being moved: true for the computer, false for this phone, null once the move is done. The
     * device sheet, Android's output switcher and the computer itself ask here, and one place in the app carries it
     * out; a request made before the app is ready waits, and a newer one replaces it.
     */
    val transferRequest: StateFlow<Boolean?> = _transferRequest.asStateFlow()
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
        if (inFront) refindSoon()
    }

    // A new Wi-Fi network usually gives the computer a new address, so look for it again once the phone has one.
    private val networkChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        scope.launch {
            @OptIn(FlowPreview::class)
            favoritesRepository.favoriteIds.drop(1).debounce(LIKES_SYNC_DELAY_MS).collect { syncLikes() }
        }
        context.getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    networkChanges.tryEmit(Unit)
                }
            }
        )
        scope.launch {
            @OptIn(FlowPreview::class)
            networkChanges.debounce(NETWORK_SETTLE_MS).collect { if (state.value.address != null) refind() }
        }
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
        // The computer's Sync button reaches a phone that is controlling it through its status.
        scope.launch {
            status.filterNotNull().collect { if (it.syncRequested) syncWithDesktop() }
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
        scope.launch { refind() }
    }

    suspend fun refind() {
        if (state.value.offlineBlackout) return
        lastRefindAtMs = SystemClock.elapsedRealtime()
        val desktops = discoverDesktops()
        _found.value = desktops
        val current = state.value
        val moved = desktops.firstOrNull { it.name == current.name } ?: return
        val address = current.address ?: return
        if (moved.host != address.host || moved.port != address.port) save("${moved.host}:${moved.port}#${address.code}", moved.name)
    }

    private fun refindSoon() {
        scope.launch {
            if (state.value.address != null && SystemClock.elapsedRealtime() - lastRefindAtMs >= REFIND_INTERVAL_MS) refind()
        }
    }

    /** Pairs with the "ip:port#CODE" address typed by hand. */
    suspend fun pair(text: String): PairingResult = pairAddress(text.trim(), name = null)

    suspend fun pairWithApproval(desktop: DiscoveredDesktop): ApprovalResult {
        val id = apiClient.requestPairing(desktop.host, desktop.port) ?: return ApprovalResult.UNSUPPORTED
        val deadline = SystemClock.elapsedRealtime() + APPROVAL_WAIT_MS
        var missedChecks = 0
        while (SystemClock.elapsedRealtime() < deadline) {
            delay(APPROVAL_POLL_MS)
            val status = apiClient.pairingStatus(desktop.host, desktop.port, id)
            if (status == null) {
                if (++missedChecks >= APPROVAL_MAX_MISSED_CHECKS) return ApprovalResult.UNREACHABLE
                continue
            }
            missedChecks = 0
            if (status.status == "allowed") {
                return when (pair(desktop, status.code.orEmpty())) {
                    PairingResult.PAIRED -> ApprovalResult.PAIRED
                    PairingResult.UNREACHABLE -> ApprovalResult.UNREACHABLE
                    PairingResult.INVALID_ADDRESS, PairingResult.WRONG_CODE -> ApprovalResult.NOT_ALLOWED
                }
            }
            if (status.status == "denied") return ApprovalResult.NOT_ALLOWED
        }
        return ApprovalResult.NOT_ALLOWED
    }

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
                if (it[DESKTOP_ADDRESS]?.let(::parseDesktopAddress)?.code != parseDesktopAddress(address)?.code) {
                    it.remove(DESKTOP_LAST_BACKUP)
                    it.remove(DESKTOP_AGREED_LIKES)
                    syncedLikesRevision = null
                }
                it[DESKTOP_ADDRESS] = address
                if (name != null) it[DESKTOP_NAME] = name else it.remove(DESKTOP_NAME)
                if (controlling != null) it[DESKTOP_ACTIVE] = controlling
            }
        }
    }

    fun requestTransfer(toDesktop: Boolean) {
        _transferRequest.value = toDesktop
    }

    /** Marks the move to [toDesktop] as done, unless a newer request replaced it meanwhile. */
    fun transferDone(toDesktop: Boolean) {
        _transferRequest.compareAndSet(toDesktop, null)
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

    /** Reports what this phone plays to the paired computer; returns the commands and songs the computer sent back. */
    suspend fun checkIn(song: Song, isPlaying: Boolean, positionMs: Long, volumePercent: Int, artwork: String?): PhoneCheckInReply {
        val desktop = connectedDesktop() ?: return PhoneCheckInReply()
        val state = PhoneCheckIn(song.title, song.artist, isPlaying, positionMs, song.durationMs, volumePercent, artwork)
        val reply = apiClient.checkIn(desktop.host, desktop.port, desktop.code, state)
        if (reply.likesRevision != null && reply.likesRevision != syncedLikesRevision) scope.launch { syncLikes() }
        return reply
    }

    suspend fun syncLikes() = likesSync.withLock {
        try {
            syncLikesNow()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("DesktopRemote", "Couldn't sync likes", e)
        }
    }

    private suspend fun syncLikesNow() {
        val desktop = connectedDesktop() ?: return
        val remote = apiClient.getLikes(desktop.host, desktop.port, desktop.code) ?: return
        val songsByKey = localLibrary.getSongs().groupBy { songMatchKey(it.title, it.artist) }
        val favoriteIds = favoritesRepository.favoriteIds.first()
        val phoneLiked = songsByKey.filterValues { songs -> songs.any { it.id in favoriteIds } }.keys
        val shared = songsByKey.keys intersect remote.library.toSet()
        val plan = planLikesSync(phoneLiked, remote.liked.toSet(), shared, dataStore.data.first()[DESKTOP_AGREED_LIKES])
        if ((plan.likeOnDesktop.isNotEmpty() || plan.unlikeOnDesktop.isNotEmpty()) &&
            !apiClient.changeLikes(desktop.host, desktop.port, desktop.code, plan.likeOnDesktop, plan.unlikeOnDesktop)
        ) return
        val changedOnPhone = (plan.likeOnPhone + plan.unlikeOnPhone).flatMap { songsByKey[it].orEmpty() }
        if (changedOnPhone.isNotEmpty()) {
            favoritesRepository.mirror(
                changedOnPhone.mapTo(HashSet()) { it.id },
                plan.likeOnPhone.flatMap { songsByKey[it].orEmpty() }.mapTo(HashSet()) { it.id }
            )
        }
        dataStore.edit {
            if (it[DESKTOP_ADDRESS]?.let(::parseDesktopAddress)?.code != desktop.code) return@edit
            it[DESKTOP_AGREED_LIKES] = plan.agreed
            syncedLikesRevision = remote.revision
        }
    }

    /** Continues [songs] on the computer from [positionMs]; it plays the ones its own library has. Returns whether it accepted them. */
    suspend fun handOff(songs: List<Song>, positionMs: Long, playing: Boolean): Boolean {
        val desktop = connectedDesktop() ?: return false
        val tracks = songs.map { HandoffTrack(it.title, it.artist) }
        return apiClient.handOff(desktop.host, desktop.port, desktop.code, tracks, positionMs, playing)
    }

    suspend fun backUp(backupJson: String): Boolean {
        val desktop = connectedDesktop() ?: return false
        if (!apiClient.uploadBackup(desktop.host, desktop.port, desktop.code, backupJson)) return false
        runCatching { dataStore.edit { it[DESKTOP_LAST_BACKUP] = System.currentTimeMillis() } }
            .onFailure {
                if (it is CancellationException) throw it
                Log.w("DesktopRemote", "Backed up, but couldn't record when", it)
            }
        return true
    }

    suspend fun backUpIfDue(createBackup: suspend () -> String) {
        val lastBackupAtMs = state.first { it.loaded }.lastBackupAtMs ?: 0L
        if (connectedDesktop() == null || System.currentTimeMillis() - lastBackupAtMs < BACKUP_INTERVAL_MS) return
        backUp(createBackup())
    }

    fun playNext(song: Song, onResult: (Boolean) -> Unit) {
        val desktop = connectedDesktop() ?: return onResult(false)
        scope.launch { onResult(apiClient.playNextOnDesktop(desktop.host, desktop.port, desktop.code, HandoffTrack(song.title, song.artist))) }
    }

    fun queueListen(listen: Listen) {
        if (state.value.address == null) return
        scope.launch {
            dataStore.edit { it[DESKTOP_PENDING_LISTENS] = sharedJson.encodeToString((pendingListens(it[DESKTOP_PENDING_LISTENS]) + listen).takeLast(MAX_PENDING_LISTENS)) }
            syncListens()
        }
    }

    /** Syncs likes and the listening record with the computer; returns whether the computer was reached. */
    suspend fun syncWithDesktop(): Boolean {
        val reached = syncListens()
        syncLikes()
        return reached
    }

    suspend fun syncListens(): Boolean = listensSync.withLock {
        try {
            syncListensNow()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("DesktopRemote", "Couldn't sync listens", e)
            false
        }
    }

    private suspend fun syncListensNow(): Boolean {
        val desktop = connectedDesktop() ?: return false
        var uploaded = true
        // Listens from before pairing were never queued, so a newly paired computer gets the whole history once. The
        // computer skips listens it already has, including its own that this phone was sent.
        if (dataStore.data.first()[DESKTOP_HISTORY_SENT_TO] != desktop.code) {
            if (apiClient.sendListens(desktop.host, desktop.port, desktop.code, statsRepository.listens.first())) {
                dataStore.edit { it[DESKTOP_HISTORY_SENT_TO] = desktop.code }
            } else {
                uploaded = false
            }
        }
        if (dataStore.data.first()[DESKTOP_TOTALS_SENT_TO] != desktop.code) {
            val totals = earlierTotals()
            if (totals.isEmpty() || apiClient.sendListens(desktop.host, desktop.port, desktop.code, totals)) {
                dataStore.edit { it[DESKTOP_TOTALS_SENT_TO] = desktop.code }
            } else {
                uploaded = false
            }
        }
        val pending = pendingListens(dataStore.data.first()[DESKTOP_PENDING_LISTENS])
        if (pending.isNotEmpty()) {
            if (apiClient.sendListens(desktop.host, desktop.port, desktop.code, pending)) {
                val sent = pending.toSet()
                dataStore.edit { it[DESKTOP_PENDING_LISTENS] = sharedJson.encodeToString(pendingListens(it[DESKTOP_PENDING_LISTENS]).filterNot { listen -> listen in sent }) }
            } else {
                uploaded = false
            }
        }
        if (!appPreferences.preferences.first().listeningStatsEnabled) return uploaded
        val since = dataStore.data.first()[DESKTOP_LISTENS_SINCE] ?: 0L
        val listens = apiClient.getListens(desktop.host, desktop.port, desktop.code, since) ?: return false
        if (listens.isEmpty()) return uploaded
        val songsByKey = localLibrary.getSongs().associateBy { songMatchKey(it.title, it.artist) }
        val known = statsRepository.listens.first().mapTo(HashSet()) { it.at to it.statsSongId }
        statsRepository.addListens(
            listens.map { it.copy(songId = songsByKey[songMatchKey(it.title, it.artist)]?.id) }.filterNot { (it.at to it.statsSongId) in known }
        )
        dataStore.edit { it[DESKTOP_LISTENS_SINCE] = listens.maxOf { listen -> listen.at } }
        return uploaded
    }

    // Each song's total for a month from before single listens were kept, dated the first of that month.
    private suspend fun earlierTotals(): List<Listen> {
        val songsById = localLibrary.getSongs().associateBy { it.id }
        return statsRepository.earlierMonthlyStats.first().flatMap { (month, stats) ->
            val at = runCatching {
                YearMonth.parse(month).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull() ?: return@flatMap emptyList()
            val (known, removed) = stats.songPlayCounts.filterValues { it > 0 }.entries.partition { (id, _) ->
                id in songsById || id.startsWith("song:")
            }
            val songs = known.map { (id, plays) ->
                val song = songsById[id]
                // A song not in the library is kept by its title and artist.
                val named = id.removePrefix("song:").split('\u001f')
                Listen(
                    at = at,
                    title = song?.title ?: named.getOrNull(0).orEmpty(),
                    artist = song?.artist ?: named.getOrNull(1).orEmpty(),
                    album = song?.album.orEmpty(),
                    genre = song?.genres?.firstOrNull().orEmpty(),
                    ms = (stats.songListeningMs[id] ?: 0L).coerceAtLeast(1L),
                    songId = id,
                    plays = plays
                )
            }
            // Songs since removed from the phone still count in its Stats without being listed, so they go as one
            // untitled total for the month.
            val removedTotal = removed.takeIf { it.isNotEmpty() }?.let { entries ->
                Listen(
                    at = at,
                    title = "",
                    artist = "",
                    ms = entries.sumOf { stats.songListeningMs[it.key] ?: 0L }.coerceAtLeast(1L),
                    plays = entries.sumOf { it.value }
                )
            }
            songs + listOfNotNull(removedTotal)
        }
    }

    private fun pendingListens(json: String?): List<Listen> =
        json?.let { runCatching { sharedJson.decodeFromString<List<Listen>>(it) }.getOrNull() }.orEmpty()

    fun sendPlaylist(name: String, songs: List<Song>, onResult: (PlaylistCopyResult?) -> Unit) {
        val desktop = connectedDesktop() ?: return onResult(null)
        val playlist = DesktopPlaylist(name, songs.map { HandoffTrack(it.title, it.artist) })
        scope.launch { onResult(apiClient.sendPlaylist(desktop.host, desktop.port, desktop.code, playlist)) }
    }

    /** A page of the computer's library matching [query]. */
    suspend fun computerLibrary(query: String, offset: Int, limit: Int = COMPUTER_LIBRARY_PAGE): ComputerLibraryResult {
        val desktop = connectedDesktop() ?: return ComputerLibraryResult.Unreachable
        return apiClient.getLibrary(desktop.host, desktop.port, desktop.code, query, offset, limit)
    }

    /** Plays [tracks] from the computer's library on the computer, starting at [index]. */
    fun playOnComputer(tracks: List<DesktopLibraryTrack>, index: Int, onResult: (Boolean) -> Unit) {
        val desktop = connectedDesktop() ?: return onResult(false)
        scope.launch { onResult(apiClient.playLibrary(desktop.host, desktop.port, desktop.code, tracks.map { it.id }, index)) }
    }

    /** [tracks] as songs this phone streams from the computer. The URLs carry the code, as the player can't send headers. */
    fun computerSongs(tracks: List<DesktopLibraryTrack>): List<Song> {
        val desktop = connectedDesktop() ?: return emptyList()
        val base = "http://${desktop.host}:${desktop.port}/api"
        return tracks.map { track ->
            Song(
                id = "computer:${track.id}",
                title = track.title,
                artist = track.artist,
                album = track.album,
                albumId = "computer:${track.album}",
                durationMs = track.durationMs,
                contentUri = "$base/stream?id=${track.id}&code=${desktop.code}".toUri(),
                source = MusicSource.Computer,
                artUri = "$base/artwork?id=${track.id}&code=${desktop.code}".toUri()
            )
        }
    }

    suspend fun computerPlaylists(): List<DesktopPlaylist>? {
        val desktop = connectedDesktop() ?: return null
        return apiClient.getPlaylists(desktop.host, desktop.port, desktop.code)
    }

    suspend fun downloadBackup(): String? {
        val desktop = connectedDesktop() ?: return null
        return apiClient.downloadBackup(desktop.host, desktop.port, desktop.code)
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
