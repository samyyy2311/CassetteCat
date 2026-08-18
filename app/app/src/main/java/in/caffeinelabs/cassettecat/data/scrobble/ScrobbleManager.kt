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
    private val libreFmClient = LibreFmClient()

    fun onTrackStarted(song: Song) {
        scope.launch {
            val serviceSettings = serviceSettingsRepository.settings.first()
            if (serviceSettings.offlineBlackoutMode) return@launch

            val settings = repository.settings.first()
            if (settings.listenBrainz.enabled && settings.listenBrainz.userToken.isNotBlank()) {
                listenBrainzClient.submitNowPlaying(settings.listenBrainz.userToken, song)
            }
            if (settings.libreFm.enabled && settings.libreFm.sessionKey.isNotBlank()) {
                libreFmClient.updateNowPlaying(settings.libreFm.sessionKey, song)
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
                listenBrainzClient.submitListen(settings.listenBrainz.userToken, song, nowSec)
            }
            if (settings.libreFm.enabled && settings.libreFm.sessionKey.isNotBlank()) {
                libreFmClient.scrobble(settings.libreFm.sessionKey, song, nowSec)
            }
        }
    }
}
