package `in`.caffeinelabs.cassettecat.data.scrobble

import android.content.Context
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.songMatchKey
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScrobbleManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val repository = ScrobbleSettingsRepository(context)
    private val serviceSettingsRepository = ServiceSettingsRepository(context)
    private val listenBrainzClient = ListenBrainzClient()

    private suspend fun recordAnswer(token: String, savedListen: Boolean, problem: String?) {
        val current = repository.settings.first().listenBrainz
        // An answer about a token that has since been replaced says nothing about the current one, and playing-now
        // is only kept for a moment, so only a saved listen shows the problem is gone.
        if (problem == null || current.userToken != token || (problem.isEmpty() && !savedListen)) return
        if (problem != current.problem) repository.setListenBrainzProblem(problem)
    }

    fun onTrackStarted(song: Song) {
        scope.launch {
            val serviceSettings = serviceSettingsRepository.settings.first()
            if (serviceSettings.offlineBlackoutMode) return@launch

            val settings = repository.settings.first()
            if (settings.listenBrainz.enabled && settings.listenBrainz.userToken.isNotBlank()) {
                val token = settings.listenBrainz.userToken
                recordAnswer(token, savedListen = false, listenBrainzClient.submitNowPlaying(token, song))
            }
            ScrobbleAccount.entries.forEach { account ->
                val config = settings.account(account)
                if (config.enabled && config.sessionKey.isNotBlank()) {
                    account.client?.updateNowPlaying(config.sessionKey, song)
                }
            }
        }
    }

    // Marks songs as loved, or not, on every connected account. They go one a second, so a sync of
    // many likes stays within the services' rate limits; a newer choice for a song replaces a queued one.
    fun love(songs: List<Song>, loved: Boolean) {
        val keys = songs.map { songMatchKey(it.title, it.artist) }.toSet()
        pendingLoves.update { queued ->
            queued.filterNot { songMatchKey(it.song.title, it.song.artist) in keys } + songs.map { Love(it, loved) }
        }
        synchronized(Companion) {
            if (loveSender?.isActive != true) loveSender = loveScope.launch { sendLoves() }
        }
    }

    private suspend fun sendLoves() {
        while (true) {
            val next = pendingLoves.value.firstOrNull() ?: return
            pendingLoves.update { it - next }
            if (serviceSettingsRepository.settings.first().offlineBlackoutMode) {
                pendingLoves.value = emptyList()
                return
            }
            val settings = repository.settings.first()
            if (settings.listenBrainz.enabled && settings.listenBrainz.userToken.isNotBlank()) {
                listenBrainzClient.sendFeedback(settings.listenBrainz.userToken, next.song, next.loved)
            }
            ScrobbleAccount.entries.forEach { account ->
                val config = settings.account(account)
                if (config.enabled && config.sessionKey.isNotBlank()) {
                    account.client?.love(config.sessionKey, next.song, next.loved)
                }
            }
            delay(LOVE_INTERVAL_MS)
        }
    }

    private data class Love(val song: Song, val loved: Boolean)

    companion object {
        private const val LOVE_INTERVAL_MS = 1_000L
        private val loveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val pendingLoves = MutableStateFlow<List<Love>>(emptyList())
        private var loveSender: Job? = null

        val lovesLeft: StateFlow<Int> = pendingLoves.map { it.size }.stateIn(loveScope, SharingStarted.Eagerly, 0)
    }

    fun onTrackPlayed(song: Song) {
        scope.launch {
            val serviceSettings = serviceSettingsRepository.settings.first()
            if (serviceSettings.offlineBlackoutMode) return@launch

            val settings = repository.settings.first()
            val nowSec = System.currentTimeMillis() / 1000L
            if (settings.listenBrainz.enabled && settings.listenBrainz.userToken.isNotBlank()) {
                val token = settings.listenBrainz.userToken
                recordAnswer(token, savedListen = true, listenBrainzClient.submitListen(token, song, nowSec))
            }
            ScrobbleAccount.entries.forEach { account ->
                val config = settings.account(account)
                if (config.enabled && config.sessionKey.isNotBlank()) {
                    account.client?.scrobble(config.sessionKey, song, nowSec)
                }
            }
        }
    }
}
