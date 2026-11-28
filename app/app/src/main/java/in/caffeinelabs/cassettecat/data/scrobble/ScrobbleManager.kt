package `in`.caffeinelabs.cassettecat.data.scrobble

import android.content.Context
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScrobbleManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val repository = ScrobbleSettingsRepository(context)
    private val serviceSettingsRepository = ServiceSettingsRepository(context)
    private val listenBrainzClient = ListenBrainzClient()

    init {
        ScrobbleOutbox.start(context)
    }

    // Playing-now is only kept for a moment, so it can report a problem but only a saved listen clears one.
    // An answer about a token that has since been replaced says nothing about the current one.
    private suspend fun recordNowPlayingProblem(token: String, problem: String?) {
        val current = repository.settings.first().listenBrainz
        if (problem.isNullOrEmpty() || current.userToken != token) return
        if (problem != current.problem) repository.setListenBrainzProblem(problem)
    }

    fun onTrackStarted(song: Song) {
        scope.launch {
            val serviceSettings = serviceSettingsRepository.settings.first()
            if (serviceSettings.offlineBlackoutMode) return@launch

            val settings = repository.settings.first()
            if (settings.listenBrainz.enabled && settings.listenBrainz.userToken.isNotBlank()) {
                val token = settings.listenBrainz.userToken
                recordNowPlayingProblem(token, listenBrainzClient.submitNowPlaying(token, song))
            }
            ScrobbleAccount.entries.forEach { account ->
                val config = settings.account(account)
                if (config.enabled && config.sessionKey.isNotBlank()) {
                    account.client?.updateNowPlaying(config.sessionKey, song)
                }
            }
        }
    }

    // Marks songs as loved, or not, on every connected account. They wait in the outbox until sent, so
    // loves made offline or in blackout mode go out once the connection is back.
    fun love(songs: List<Song>, loved: Boolean) {
        ScrobbleOutbox.add(context, OutboxItem.Kind.LOVE, songs, loved = loved)
    }

    fun syncLikes(library: List<Song>, likedIds: Set<String>) {
        ScrobbleOutbox.syncLikes(context, library, likedIds)
    }

    fun onTrackPlayed(song: Song) {
        val nowSec = System.currentTimeMillis() / 1000L
        ScrobbleOutbox.add(context, OutboxItem.Kind.SCROBBLE, listOf(song), listenedAtSec = nowSec)
    }

    companion object {
        val lovesLeft: StateFlow<Int> get() = ScrobbleOutbox.lovesLeft
    }
}
