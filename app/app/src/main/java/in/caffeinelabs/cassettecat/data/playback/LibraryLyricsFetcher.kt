package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import `in`.caffeinelabs.cassettecat.data.library.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object LibraryLyricsFetcher {
    data class Progress(val found: Int = 0, val total: Int = 0, val left: Int = 0, val waitingForWifi: Boolean = false)

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun update(context: Context, songs: List<Song>, enabled: Boolean, onMobileData: Boolean) {
        val previous = job
        val appContext = context.applicationContext
        job = scope.launch {
            // The old run must be gone before progress is reset or a new run starts.
            previous?.cancelAndJoin()
            if (enabled) fetch(appContext, songs, onMobileData) else _progress.value = Progress()
        }
    }

    private suspend fun fetch(context: Context, songs: List<Song>, onMobileData: Boolean) {
        val client = LrcLibClient(context)
        val (withLyrics, without) = songs.partition { client.storedLyrics(it.artist, it.title, it.album) != null }
        val pending = without.filter { it.artist.isNotBlank() && !client.missingRecently(it.artist, it.title, it.album) }
        var found = withLyrics.size
        _progress.value = Progress(found, songs.size, pending.size)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        pending.forEachIndexed { index, song ->
            var tries = 0
            while (true) {
                waitForAllowedNetwork(connectivity, onMobileData)
                val result = client.lookUpForLibrary(song.artist, song.title, song.album)
                // The request itself can't be cancelled, so don't report it after a cancel.
                currentCoroutineContext().ensureActive()
                if (result == LibraryLookup.FOUND) found++
                if (result != LibraryLookup.FAILED || ++tries >= 3) break
                delay(60_000)
            }
            _progress.value = Progress(found, songs.size, pending.size - index - 1)
            delay(300)
        }
    }

    // Waits for a usable connection, carrying on the moment one appears. Metered networks, such as
    // mobile data or a phone's hotspot, count only when mobile data is allowed.
    private suspend fun waitForAllowedNetwork(connectivity: ConnectivityManager, onMobileData: Boolean) {
        fun allowed(capabilities: NetworkCapabilities?) = capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (onMobileData || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
        if (allowed(connectivity.getNetworkCapabilities(connectivity.activeNetwork))) return
        _progress.value = _progress.value.copy(waitingForWifi = true)
        callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    trySend(capabilities)
                }
            }
            connectivity.registerDefaultNetworkCallback(callback)
            awaitClose { connectivity.unregisterNetworkCallback(callback) }
        }.first(::allowed)
        _progress.value = _progress.value.copy(waitingForWifi = false)
    }
}
