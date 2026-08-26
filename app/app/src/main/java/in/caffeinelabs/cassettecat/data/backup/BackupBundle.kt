package `in`.caffeinelabs.cassettecat.data.backup

import `in`.caffeinelabs.cassettecat.data.library.FolderFilterConfig
import `in`.caffeinelabs.cassettecat.data.library.PlaylistCoverType
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerLevels
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettings
import `in`.caffeinelabs.cassettecat.data.stats.Milestone
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import kotlinx.serialization.Serializable

@Serializable
data class BackupBundle(
    val version: Int = 2,
    val createdAt: Long,
    val serviceSettings: ServiceSettings,
    val equalizer: EqualizerLevels,
    val favoriteIds: Set<String>,
    val folderFilter: FolderFilterConfig,
    val playlists: List<BackupPlaylist>,
    // keyed by YearMonth.toString(), e.g. "2026-06"
    val listeningStatsMonthly: Map<String, MonthlyStats>,
    val listeningMilestones: List<Milestone>,
    // keyed by StreamingProtocol.name
    val streamingServers: Map<String, StreamingServerConfig>,
    val appPreferences: BackupAppPreferences? = null
)

@Serializable
data class BackupAppPreferences(
    val themeAccent: String = "RECORD_RED",
    val customAccentColor: Long = 0xFFC23B30,
    val amoledDarkTheme: Boolean = false,
    val defaultLibraryTab: String = "SONGS",
    val albumArtCornerRadiusDp: Int = 16,
    val showRemainingTime: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val gaplessPlayback: Boolean = true,
    val replayGainPreAmpDb: Int = 0,
    val monoAudio: Boolean = false,
    val autoplayEnabled: Boolean = false,
    val volumeLimitEnabled: Boolean = false,
    val maxVolumePercent: Int = 80,
    val gridColumnCount: Int = 2,
    val trackRowDensity: String = "DETAILED",
    val defaultSortMetric: String = "TITLE",
    val showAudioQualityBadge: Boolean = true,
    val showNowPlayingBlur: Boolean = true,
    val libraryTabOrder: List<String> = emptyList(),
    val hiddenLibraryTabs: Set<String> = emptySet(),
    val librarySortDirection: String = "ASCENDING",
    val libraryCollectionLayout: String = "GRID",
    val librarySongFilter: String = "ALL",
    val libraryArtistSortOrder: String = "NAME",
    val libraryArtistSortDirection: String = "ASCENDING",
    val libraryAlbumSortOrder: String = "ALBUM",
    val libraryAlbumSortDirection: String = "ASCENDING",
    val libraryGenreSortOrder: String = "NAME",
    val libraryGenreSortDirection: String = "ASCENDING",
    val swipeUpLyricsEnabled: Boolean = true,
    val shakeToSkipEnabled: Boolean = false,
    val shakeSensitivity: Int = 2,
    val flipToPauseEnabled: Boolean = false,
    val proximityWaveSkipEnabled: Boolean = false,
    val miniPlayerSwipeToSkip: Boolean = true,
    val nowPlayingBackdropStyle: String = "OLED_BLACK",
    val appFontFamily: String = "SPACE_GROTESK",
    val lyricsAlignment: String = "CENTER",
    val lyricsActiveStyle: String = "ACCENT_GLOW",
    val lyricsFontFamily: String = "SPACE_GROTESK",
    val wifiOnlyDownloads: Boolean = false,
    val listeningStatsEnabled: Boolean = true,
    val defaultStartScreen: String = "HOME",
    val lastOpenedRoute: String = "home",
    val resumeQueueOnLaunch: Boolean = true,
    val replayGainEnabled: Boolean = true,
    val pauseOnHeadphoneDisconnect: Boolean = true,
    val keepScreenOnLyrics: Boolean = false,
    val lyricsFontSize: String = "MEDIUM",
    val localLrcPriority: Boolean = true,
    val ignoreShortAudioClips: Boolean = false,
    val showHomeRecentlyPlayed: Boolean = true,
    val showHomeHeavyRotation: Boolean = true,
    val showHomeRecentlyAdded: Boolean = true,
    val showHomeForgottenFavorites: Boolean = true,
    val homeSectionOrder: List<String> = emptyList(),
    val showMiniPlayerProgress: Boolean = true,
    val miniPlayerAction: String = "NEXT",
    val artworkAccentEnabled: Boolean = false,
    val autoDriveModeBluetooth: Boolean = false,
    val radioSortOrder: String = "POPULARITY",
    val radioSortDirection: String = "DESCENDING",
    val radioSelectedCountry: String = "",
    val radioSelectedState: String = "",
    val radioSelectedLanguage: String = "",
    val radioSelectedTag: String = "",
    val radioDefaultCountryApplied: Boolean = false
)

@Serializable
data class BackupPlaylist(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val coverType: PlaylistCoverType,
    val coverValue: String?,
    // only set when coverType == IMAGE; base64 of the downscaled JPEG
    val coverImageBase64: String? = null
)
