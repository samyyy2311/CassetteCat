package `in`.caffeinelabs.cassettecat.data.scrobble

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private const val OUTBOX_FILE = "scrobble_outbox.json"
private const val LISTENBRAINZ = "listenbrainz"

// One a second keeps a long Sync Likes within the services' rate limits.
private const val SEND_INTERVAL_MS = 1_000L

// Tries made while online; a song a service keeps refusing is dropped rather than blocking the rest.
private const val MAX_ATTEMPTS = 5

@Serializable
internal data class OutboxItem(
    val id: Long,
    val kind: Kind,
    val artist: String,
    val title: String,
    val album: String,
    val listenedAtSec: Long = 0L,
    val loved: Boolean = false,
    // The services still to accept it: ListenBrainz and the ids of the Audioscrobbler accounts.
    val targets: Set<String>,
    val attempts: Int = 0
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

// Scrobbles and loves wait here until each service has them, so none are lost to blackout mode, a
// missing connection or a service being down. Kept in a file, so they also survive the app closing.
internal object ScrobbleOutbox {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val items = MutableStateFlow<List<OutboxItem>>(emptyList())
    private val loaded = CompletableDeferred<Unit>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var started = false
    private var nextId = System.currentTimeMillis()

    val lovesLeft: StateFlow<Int> =
        items.map { list -> list.count { it.kind == OutboxItem.Kind.LOVE } }.stateIn(scope, SharingStarted.Eagerly, 0)

    fun start(context: Context) {
        synchronized(this) {
            if (started) return
            started = true
        }
        val app = context.applicationContext
        val file = File(app.filesDir, OUTBOX_FILE)
        scope.launch {
            items.value = runCatching { sharedJson.decodeFromString<List<OutboxItem>>(file.readText()) }.getOrDefault(emptyList())
            loaded.complete(Unit)
            send(app, file)
        }
        // The sender waits while offline or in blackout, and carries on when either changes.
        app.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    wake.trySend(Unit)
                }
            }
        )
        scope.launch {
            ServiceSettingsRepository(app).settings.map { it.offlineBlackoutMode }.distinctUntilChanged().collect {
                wake.trySend(Unit)
            }
        }
    }

    // A love replaces one still waiting for the same song, so only the latest choice is sent.
    suspend fun add(context: Context, kind: OutboxItem.Kind, songs: List<Song>, loved: Boolean = false, listenedAtSec: Long = 0L) {
        start(context)
        loaded.await()
        val targets = enabledTargets(context)
        if (targets.isEmpty() || songs.isEmpty()) return
        val file = File(context.applicationContext.filesDir, OUTBOX_FILE)
        mutex.withLock {
            val keys = songs.map { songMatchKey(it.title, it.artist) }.toSet()
            val kept = if (kind == OutboxItem.Kind.LOVE) {
                items.value.filterNot { it.kind == OutboxItem.Kind.LOVE && songMatchKey(it.title, it.artist) in keys }
            } else {
                items.value
            }
            items.value = kept + songs.map { song ->
                OutboxItem(nextId++, kind, song.artist, song.title, song.album, listenedAtSec, loved, targets)
            }
            save(file)
        }
        wake.trySend(Unit)
    }

    private suspend fun send(context: Context, file: File) {
        val settingsRepository = ScrobbleSettingsRepository(context)
        val serviceSettings = ServiceSettingsRepository(context)
        val listenBrainz = ListenBrainzClient()
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        while (true) {
            val next = items.value.firstOrNull()
            val online = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            if (next == null || !online || serviceSettings.settings.first().offlineBlackoutMode) {
                wake.receive()
                continue
            }
            val settings = settingsRepository.settings.first()
            val pending = next.targets.filterTo(mutableSetOf()) { target ->
                !deliver(target, next, settings, settingsRepository, listenBrainz)
            }
            mutex.withLock {
                // A newer love for the same song may have replaced this one while it was being sent.
                if (items.value.any { it.id == next.id }) {
                    val rest = items.value.filterNot { it.id == next.id }
                    val attempts = next.attempts + 1
                    // What's still owed goes to the back, so one failing song doesn't hold up the others.
                    items.value = if (pending.isEmpty() || attempts >= MAX_ATTEMPTS) {
                        rest
                    } else {
                        rest + next.copy(targets = pending, attempts = attempts)
                    }
                    save(file)
                }
            }
            delay(SEND_INTERVAL_MS)
        }
    }

    // True once the service has it, or when it can never take it: the account was disconnected or
    // switched off, or ListenBrainz refused the listen.
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

    private suspend fun enabledTargets(context: Context): Set<String> {
        val settings = ScrobbleSettingsRepository(context).settings.first()
        return buildSet {
            if (settings.listenBrainz.enabled && settings.listenBrainz.userToken.isNotBlank()) add(LISTENBRAINZ)
            ScrobbleAccount.entries.forEach { account ->
                val config = settings.account(account)
                if (account.client != null && config.enabled && config.sessionKey.isNotBlank()) add(account.id)
            }
        }
    }

    private fun save(file: File) {
        runCatching { file.writeText(sharedJson.encodeToString(items.value)) }
            .onFailure { Log.w("ScrobbleOutbox", "Couldn't save the waiting scrobbles and loves", it) }
    }
}
