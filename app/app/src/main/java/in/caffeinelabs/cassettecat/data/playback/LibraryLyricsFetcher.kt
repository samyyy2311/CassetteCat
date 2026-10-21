package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import android.net.ConnectivityManager
import `in`.caffeinelabs.cassettecat.data.library.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Fetches lyrics for every song in the library, one at a time, so search can look through them. It runs while the app
 * does, only on Wi-Fi or another unmetered network, and carries on from where it got to on the next start.
 */
object LibraryLyricsFetcher {
    data class Progress(val found: Int = 0, val total: Int = 0, val left: Int = 0, val waitingForWifi: Boolean = false)

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun update(context: Context, songs: List<Song>, enabled: Boolean) {
        job?.cancel()
        if (!enabled) {
            _progress.value = Progress()
            return
        }
        val appContext = context.applicationContext
        job = scope.launch { fetch(appContext, songs) }
    }

    private suspend fun fetch(context: Context, songs: List<Song>) {
        val client = LrcLibClient(context)
        val (withLyrics, without) = songs.partition { client.storedLyrics(it.artist, it.title, it.album) != null }
        val pending = without.filter { it.artist.isNotBlank() && !client.missingRecently(it.artist, it.title, it.album) }
        var found = withLyrics.size
        _progress.value = Progress(found, songs.size, pending.size)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        pending.forEachIndexed { index, song ->
            while (connectivity.isActiveNetworkMetered) {
                _progress.value = _progress.value.copy(waitingForWifi = true)
                delay(60_000)
            }
            var tries = 0
            while (true) {
                val result = client.lookUpForLibrary(song.artist, song.title, song.album)
                if (result == LibraryLookup.FOUND) found++
                // LRCLIB is busy now and then: ask again in a minute, and move on if this song keeps failing.
                if (result != LibraryLookup.FAILED || ++tries >= 3) break
                delay(60_000)
            }
            _progress.value = Progress(found, songs.size, pending.size - index - 1)
            // LRCLIB is run by volunteers, so a library's worth of requests goes one at a time with a pause between.
            delay(300)
        }
    }
}
