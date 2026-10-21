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

/** Fetches lyrics for the whole library, for search. Unmetered networks only; resumes on the next start. */
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
                // Transient failure: retry in a minute, skip the song after three.
                if (result != LibraryLookup.FAILED || ++tries >= 3) break
                delay(60_000)
            }
            _progress.value = Progress(found, songs.size, pending.size - index - 1)
            // Throttle: one request at a time, with a short gap.
            delay(300)
        }
    }
}
