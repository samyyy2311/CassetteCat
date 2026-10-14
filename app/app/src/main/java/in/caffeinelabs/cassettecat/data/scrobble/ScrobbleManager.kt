package `in`.caffeinelabs.cassettecat.data.scrobble

import android.content.Context
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
