package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterConfig
import `in`.caffeinelabs.cassettecat.data.library.LibraryFolderRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.DefaultStartScreen
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.LyricsFontSize
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettings
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.data.update.GitHubUpdateChecker
import `in`.caffeinelabs.cassettecat.data.update.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val subsonic: StreamingServerConfig = StreamingServerConfig(),
    val jellyfin: StreamingServerConfig = StreamingServerConfig(),
    val services: ServiceSettings = ServiceSettings(),
    val folderFilter: FolderFilterConfig = FolderFilterConfig(),
    val preferences: AppPreferences = AppPreferences()
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val serverRepository = StreamingServerRepository(app)
    private val credentialStore = CredentialStore(app)
    private val serviceSettingsRepository = ServiceSettingsRepository(app)
    private val libraryFolderRepository = LibraryFolderRepository(app)
    private val appPreferencesRepository = AppPreferencesRepository(app)
    private val updateChecker = GitHubUpdateChecker()
    private val currentVersion = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "0.0.0"

    val uiState: StateFlow<SettingsUiState> = combine(
        serverRepository.config(StreamingProtocol.SUBSONIC),
        serverRepository.config(StreamingProtocol.JELLYFIN),
        serviceSettingsRepository.settings,
        libraryFolderRepository.folderFilterConfig,
        appPreferencesRepository.preferences
    ) { subsonic, jellyfin, services, folderFilter, preferences ->
        SettingsUiState(subsonic, jellyfin, services, folderFilter, preferences)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // null = not checked yet this session
    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    fun disconnect(protocol: StreamingProtocol) {
        viewModelScope.launch {
            serverRepository.disconnect(protocol)
            credentialStore.clear(protocol)
        }
    }

    fun setOfflineBlackoutMode(enabled: Boolean) {
        viewModelScope.launch { serviceSettingsRepository.setOfflineBlackoutMode(enabled) }
    }

    fun setServiceEnabled(service: ExternalService, enabled: Boolean) {
        viewModelScope.launch { serviceSettingsRepository.setEnabled(service, enabled) }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateCheckResult.value = null
            _updateCheckResult.value = updateChecker.checkForUpdate(currentVersion)
        }
    }

    fun setDefaultStartScreen(screen: DefaultStartScreen) {
        viewModelScope.launch { appPreferencesRepository.setDefaultStartScreen(screen) }
    }

    fun setResumeQueueOnLaunch(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setResumeQueueOnLaunch(enabled) }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setReplayGainEnabled(enabled) }
    }

    fun setPauseOnHeadphoneDisconnect(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setPauseOnHeadphoneDisconnect(enabled) }
    }

    fun setKeepScreenOnLyrics(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setKeepScreenOnLyrics(enabled) }
    }

    fun setLyricsFontSize(size: LyricsFontSize) {
        viewModelScope.launch { appPreferencesRepository.setLyricsFontSize(size) }
    }

    fun setLocalLrcPriority(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setLocalLrcPriority(enabled) }
    }

    fun setIgnoreShortAudioClips(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setIgnoreShortAudioClips(enabled) }
    }

    fun setShowHomeRecentlyPlayed(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setShowHomeRecentlyPlayed(enabled) }
    }

    fun setShowHomeHeavyRotation(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setShowHomeHeavyRotation(enabled) }
    }

    fun setShowHomeRecentlyAdded(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setShowHomeRecentlyAdded(enabled) }
    }

    fun setShowHomeForgottenFavorites(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setShowHomeForgottenFavorites(enabled) }
    }

    fun setThemeAccent(accent: `in`.caffeinelabs.cassettecat.data.settings.ThemeAccent) {
        viewModelScope.launch { appPreferencesRepository.setThemeAccent(accent) }
    }

    fun setCustomAccentColor(colorValue: Long) {
        viewModelScope.launch {
            appPreferencesRepository.setCustomAccentColor(colorValue)
            appPreferencesRepository.setThemeAccent(`in`.caffeinelabs.cassettecat.data.settings.ThemeAccent.CUSTOM)
        }
    }

    fun setAmoledDarkTheme(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setAmoledDarkTheme(enabled) }
    }

    fun setDefaultLibraryTab(tab: `in`.caffeinelabs.cassettecat.data.settings.DefaultLibraryTab) {
        viewModelScope.launch { appPreferencesRepository.setDefaultLibraryTab(tab) }
    }

    fun setAlbumArtCornerRadiusDp(radius: Int) {
        viewModelScope.launch { appPreferencesRepository.setAlbumArtCornerRadiusDp(radius) }
    }

    fun setShowRemainingTime(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setShowRemainingTime(enabled) }
    }

    fun setSeekStepSeconds(seconds: Int) {
        viewModelScope.launch { appPreferencesRepository.setSeekStepSeconds(seconds) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setHapticFeedbackEnabled(enabled) }
    }

    // Audio Engine & Transitions
    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { appPreferencesRepository.setCrossfadeSeconds(seconds) }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setGaplessPlayback(enabled) }
    }

    fun setReplayGainPreAmpDb(db: Int) {
        viewModelScope.launch { appPreferencesRepository.setReplayGainPreAmpDb(db) }
    }

    fun setMonoAudio(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setMonoAudio(enabled) }
    }

    fun setSkipSilenceEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setSkipSilenceEnabled(enabled) }
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setAutoplayEnabled(enabled) }
    }

    // Library & Layout
    fun setGridColumnCount(columns: Int) {
        viewModelScope.launch { appPreferencesRepository.setGridColumnCount(columns) }
    }

    fun setTrackRowDensity(density: `in`.caffeinelabs.cassettecat.data.settings.TrackRowDensity) {
        viewModelScope.launch { appPreferencesRepository.setTrackRowDensity(density) }
    }

    fun setDefaultSortMetric(metric: `in`.caffeinelabs.cassettecat.data.settings.DefaultSortMetric) {
        viewModelScope.launch { appPreferencesRepository.setDefaultSortMetric(metric) }
    }

    fun setShowAudioQualityBadge(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setShowAudioQualityBadge(enabled) }
    }

    fun setShowNowPlayingBlur(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setShowNowPlayingBlur(enabled) }
    }

    // Gestures
    fun setDoubleTapSeekEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setDoubleTapSeekEnabled(enabled) }
    }

    fun setSwipeUpLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setSwipeUpLyricsEnabled(enabled) }
    }

    // Lyrics Customization
    fun setLyricsAlignment(alignment: `in`.caffeinelabs.cassettecat.data.settings.LyricsAlignment) {
        viewModelScope.launch { appPreferencesRepository.setLyricsAlignment(alignment) }
    }

    fun setLyricsActiveStyle(style: `in`.caffeinelabs.cassettecat.data.settings.LyricsActiveStyle) {
        viewModelScope.launch { appPreferencesRepository.setLyricsActiveStyle(style) }
    }

    // Cache & Storage
    fun setMaxCacheSizeMb(sizeMb: Int) {
        viewModelScope.launch {
            appPreferencesRepository.setMaxCacheSizeMb(sizeMb)
            `in`.caffeinelabs.cassettecat.data.download.DownloadSettingsRepository(getApplication())
                .setMaxCacheBytes(sizeMb * 1024L * 1024L)
        }
    }

    fun setAutoCacheFavorites(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setAutoCacheFavorites(enabled)
            `in`.caffeinelabs.cassettecat.data.download.DownloadSettingsRepository(getApplication())
                .setAutoDownloadFavorites(enabled)
        }
    }
}
