package `in`.caffeinelabs.cassettecat.data.scrobble

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.AtomicFile
import android.util.Log
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.songMatchKey
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private const val OUTBOX_FILE = "scrobble_outbox.json"
private const val LOVED_FILE = "scrobble_loved.json"
private const val LISTENBRAINZ = "listenbrainz"

// One a second keeps a long Sync Likes within the services' rate limits.
private const val SEND_INTERVAL_MS = 1_000L

// A failing service is retried after a minute, then twice as long each time, up to six hours apart.
private const val FIRST_RETRY_MS = 60_000L
private const val LONGEST_RETRY_MS = 6 * 60 * 60_000L

// Last.fm refuses scrobbles older than two weeks, so nothing is worth keeping longer.
private const val KEEP_FOR_MS = 14 * 24 * 60 * 60_000L

// The queue is saved every so many sends rather than after each, so a long Sync Likes doesn't rewrite it
// every second. A crash then resends at most that many, which the services take as the same love again.
private const val SAVE_EVERY_SENDS = 20

@Serializable
internal data class OutboxItem(
    val id: Long,
    val kind: Kind,
    val artist: String,
    val title: String,
    val album: String,
    val listenedAtSec: Long = 0L,
    val loved: Boolean = false,
    // The services that already have it: ListenBrainz and the ids of the Audioscrobbler accounts. It goes to
    // whichever are connected when it's sent, so plays and likes from before an account was connected reach it too.
    val delivered: Set<String> = emptySet(),
    val attempts: Int = 0,
    val addedAtMs: Long = 0L,
    val nextTryAtMs: Long = 0L
) {
    enum class Kind { SCROBBLE, LOVE }

    fun asSong() = Song(
        id = "",
        title = title,
        artist = artist,
        album = album,
        albumId = "",
        durationMs = 0L,
        contentUri = Uri.EMPTY,
        source = MusicSource.Local
    )
}

@Serializable
internal data class LovedSong(val artist: String, val title: String, val album: String)

// Scrobbles and loves wait here until each service has them, so none are lost to blackout mode, a
// missing connection or a service being down. Kept in a file, so they also survive the app closing.
internal object ScrobbleOutbox {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val items = MutableStateFlow<List<OutboxItem>>(emptyList())
    private val loaded = CompletableDeferred<Unit>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var started = false
    private var nextId = 0L
    private var sendsSinceSave = 0
    private var lovedChanged = false
    private lateinit var file: AtomicFile
    private lateinit var lovedFile: AtomicFile
    // The songs each service has loved through the app, so Sync Likes only sends what a service lacks.
    private var lovedOn: Map<String, Map<String, LovedSong>> = emptyMap()

    val lovesLeft: StateFlow<Int> =
        items.map { list -> list.count { it.kind == OutboxItem.Kind.LOVE } }.stateIn(scope, SharingStarted.Eagerly, 0)

    fun start(context: Context) {
        synchronized(this) {
            if (started) return
            started = true
        }
        val app = context.applicationContext
        file = AtomicFile(File(app.filesDir, OUTBOX_FILE))
        lovedFile = AtomicFile(File(app.filesDir, LOVED_FILE))
        scope.launch {
            items.value = runCatching { sharedJson.decodeFromString<List<OutboxItem>>(file.readFully().decodeToString()) }
                .getOrDefault(emptyList())
            // New ids follow both the clock and anything restored, so no two items ever share one.
            nextId = maxOf(System.currentTimeMillis(), (items.value.maxOfOrNull { it.id } ?: 0L) + 1)
            lovedOn = runCatching {
                sharedJson.decodeFromString<Map<String, Map<String, LovedSong>>>(lovedFile.readFully().decodeToString())
            }.getOrDefault(emptyMap())
            loaded.complete(Unit)
            send(app)
        }
        // The sender waits while offline or in blackout, and carries on when either changes, including when
        // a network that stayed connected gets its internet back.
        app.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) wake.trySend(Unit)
                }
            }
        )
        scope.launch {
            ServiceSettingsRepository(app).settings.map { it.offlineBlackoutMode }.distinctUntilChanged().collect {
                wake.trySend(Unit)
            }
        }
        // A newly connected account picks up what's waiting. Signing out forgets what that account loved, as
        // the next sign-in may be someone else.
        scope.launch {
            ScrobbleSettingsRepository(app).settings.distinctUntilChanged().collect { settings ->
                loaded.await()
                val signedIn = signedInServices(settings)
                mutex.withLock {
                    if (!signedIn.containsAll(lovedOn.keys)) {
                        lovedOn = lovedOn.filterKeys { it in signedIn }
                        saveLoved()
                    }
                }
                wake.trySend(Unit)
            }
        }
    }

    // Queued in the outbox's own scope, so it outlives the screen or service that asked. A love replaces one
    // still waiting for the same song, so only the latest choice is sent, and a love skips the services that
    // already have it.
    fun add(context: Context, kind: OutboxItem.Kind, songs: List<Song>, loved: Boolean = false, listenedAtSec: Long = 0L) {
        start(context)
        scope.launch {
            loaded.await()
            if (songs.isEmpty()) return@launch
            val services = connectedServices(ScrobbleSettingsRepository(context).settings.first())
            mutex.withLock {
                val keys = songs.map { songMatchKey(it.title, it.artist) }.toSet()
                val kept = if (kind == OutboxItem.Kind.LOVE) {
                    items.value.filterNot { it.kind == OutboxItem.Kind.LOVE && songMatchKey(it.title, it.artist) in keys }
                } else {
                    items.value
                }
                val now = System.currentTimeMillis()
                items.value = kept + songs.mapNotNull { song ->
                    val key = songMatchKey(song.title, song.artist)
                    val alreadyLoved = if (kind == OutboxItem.Kind.LOVE && loved) {
                        lovedOn.filterValues { key in it }.keys
                    } else {
                        emptySet()
                    }
                    // Every connected service has it already; a service connected later still gets it from Sync Likes.
                    if (services.isNotEmpty() && alreadyLoved.containsAll(services)) return@mapNotNull null
                    OutboxItem(nextId++, kind, song.artist, song.title, song.album, listenedAtSec, loved, alreadyLoved, addedAtMs = now)
                }
                save()
            }
            wake.trySend(Unit)
        }
    }

    // Loves every liked song in the library, and takes back loves the app sent for library songs no longer liked,
    // such as ones unliked while an account was signed out. Songs outside the library are left alone, as a missing
    // server doesn't mean they were unliked.
    fun syncLikes(context: Context, library: List<Song>, likedIds: Set<String>) {
        start(context)
        scope.launch {
            loaded.await()
            val (liked, notLiked) = library.partition { it.id in likedIds }
            val lovedKeys = mutex.withLock { lovedOn.values.flatMapTo(mutableSetOf()) { it.keys } }
            add(context, OutboxItem.Kind.LOVE, liked, loved = true)
            add(context, OutboxItem.Kind.LOVE, notLiked.filter { songMatchKey(it.title, it.artist) in lovedKeys }, loved = false)
        }
    }

    private suspend fun send(context: Context) {
        val settingsRepository = ScrobbleSettingsRepository(context)
        val serviceSettings = ServiceSettingsRepository(context)
        val listenBrainz = ListenBrainzClient()
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        while (true) {
            val now = System.currentTimeMillis()
            mutex.withLock {
                val fresh = items.value.filter { now - it.addedAtMs <= KEEP_FOR_MS }
                if (fresh.size != items.value.size) {
                    items.value = fresh
                    save()
                }
            }
            val settings = settingsRepository.settings.first()
            val services = connectedServices(settings)
            // With no account connected, everything waits for one.
            val next = if (services.isEmpty()) null else items.value.firstOrNull { it.nextTryAtMs <= now }
            val online = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            if (next == null || !online || serviceSettings.settings.first().offlineBlackoutMode) {
                // Sleeps until something changes or the earliest retry is due.
                val due = items.value.minOfOrNull { it.nextTryAtMs }?.takeIf { next == null && services.isNotEmpty() && it > now }
                withTimeoutOrNull(due?.minus(now) ?: Long.MAX_VALUE) { wake.receive() }
                continue
            }
            val accepted = (services - next.delivered).filterTo(mutableSetOf()) { service ->
                deliver(service, next, settings, settingsRepository, listenBrainz)
            }
            val delivered = next.delivered + accepted
            mutex.withLock {
                if (next.kind == OutboxItem.Kind.LOVE && accepted.isNotEmpty()) {
                    val key = songMatchKey(next.title, next.artist)
                    lovedOn = (lovedOn.keys + accepted).associateWith { service ->
                        val songs = lovedOn[service].orEmpty()
                        when {
                            service !in accepted -> songs
                            next.loved -> songs + (key to LovedSong(next.artist, next.title, next.album))
                            else -> songs - key
                        }
                    }
                    lovedChanged = true
                }
                // A newer love for the same song may have replaced this one while it was being sent.
                if (items.value.any { it.id == next.id }) {
                    val rest = items.value.filterNot { it.id == next.id }
                    items.value = if (delivered.containsAll(services)) {
                        rest
                    } else {
                        val wait = (FIRST_RETRY_MS shl next.attempts.coerceAtMost(10)).coerceAtMost(LONGEST_RETRY_MS)
                        rest + next.copy(delivered = delivered, attempts = next.attempts + 1, nextTryAtMs = now + wait)
                    }
                    if (++sendsSinceSave >= SAVE_EVERY_SENDS || items.value.none { it.nextTryAtMs <= now }) {
                        save()
                        if (lovedChanged) saveLoved()
                        lovedChanged = false
                        sendsSinceSave = 0
                    }
                }
            }
            delay(SEND_INTERVAL_MS)
        }
    }

    // True once the service has it, or when it can never take it, as when ListenBrainz refuses the listen.
    private suspend fun deliver(
        target: String,
        item: OutboxItem,
        settings: ScrobbleSettings,
        settingsRepository: ScrobbleSettingsRepository,
        listenBrainz: ListenBrainzClient
    ): Boolean {
        val song = item.asSong()
        if (target == LISTENBRAINZ) {
            val token = settings.listenBrainz.userToken.takeIf { settings.listenBrainz.enabled && it.isNotBlank() } ?: return true
            if (item.kind == OutboxItem.Kind.LOVE) return listenBrainz.sendFeedback(token, song, item.loved)
            // Empty when saved, the reason when refused, or null when it couldn't be reached.
            val answer = listenBrainz.submitListen(token, song, item.listenedAtSec) ?: return false
            if (answer != settings.listenBrainz.problem) settingsRepository.setListenBrainzProblem(answer)
            return true
        }
        val account = ScrobbleAccount.entries.firstOrNull { it.id == target } ?: return true
        val config = settings.account(account)
        val client = account.client ?: return true
        if (!config.enabled || config.sessionKey.isBlank()) return true
        return when (item.kind) {
            OutboxItem.Kind.SCROBBLE -> client.scrobble(config.sessionKey, song, item.listenedAtSec)
            OutboxItem.Kind.LOVE -> client.love(config.sessionKey, song, item.loved)
        }
    }

    // Signed in, whether or not switched on, as switching an account off keeps its loves.
    private fun signedInServices(settings: ScrobbleSettings): Set<String> =
        buildSet {
            if (settings.listenBrainz.userToken.isNotBlank()) add(LISTENBRAINZ)
            ScrobbleAccount.entries.forEach { if (settings.account(it).sessionKey.isNotBlank()) add(it.id) }
        }

    private fun connectedServices(settings: ScrobbleSettings): Set<String> =
        buildSet {
            if (settings.listenBrainz.enabled && settings.listenBrainz.userToken.isNotBlank()) add(LISTENBRAINZ)
            ScrobbleAccount.entries.forEach { account ->
                val config = settings.account(account)
                if (account.client != null && config.enabled && config.sessionKey.isNotBlank()) add(account.id)
            }
        }

    private fun saveLoved() = write(lovedFile, sharedJson.encodeToString(lovedOn))

    private fun save() = write(file, sharedJson.encodeToString(items.value))

    // Written whole to a new file and swapped in, so a crash mid-write keeps the previous copy.
    private fun write(target: AtomicFile, text: String) {
        val stream = runCatching { target.startWrite() }
            .onFailure { Log.w("ScrobbleOutbox", "Couldn't save the waiting scrobbles and loves", it) }
            .getOrNull() ?: return
        runCatching { stream.write(text.encodeToByteArray()) }
            .onSuccess { target.finishWrite(stream) }
            .onFailure {
                target.failWrite(stream)
                Log.w("ScrobbleOutbox", "Couldn't save the waiting scrobbles and loves", it)
            }
    }
}
