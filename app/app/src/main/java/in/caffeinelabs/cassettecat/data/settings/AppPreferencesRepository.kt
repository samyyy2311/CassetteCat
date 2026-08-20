package `in`.caffeinelabs.cassettecat.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

private val THEME_ACCENT = stringPreferencesKey("theme_accent")
private val THEME_ACCENT_CUSTOM_COLOR = longPreferencesKey("theme_accent_custom_color")
private val AMOLED_DARK_THEME = booleanPreferencesKey("amoled_dark_theme")
private val DEFAULT_LIBRARY_TAB = stringPreferencesKey("default_library_tab")
private val ALBUM_ART_CORNER_RADIUS = intPreferencesKey("album_art_corner_radius")
private val SHOW_REMAINING_TIME = booleanPreferencesKey("show_remaining_time")
private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")

// Audio Engine & Transitions
private val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
private val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
private val REPLAY_GAIN_PRE_AMP_DB = intPreferencesKey("replay_gain_pre_amp_db")
private val MONO_AUDIO = booleanPreferencesKey("mono_audio")
private val SKIP_SILENCE_ENABLED = booleanPreferencesKey("skip_silence_enabled")
private val AUTOPLAY_ENABLED = booleanPreferencesKey("autoplay_enabled")

// Library & Layout
private val GRID_COLUMNS = intPreferencesKey("grid_columns")
private val TRACK_ROW_DENSITY = stringPreferencesKey("track_row_density")
private val DEFAULT_SORT_METRIC = stringPreferencesKey("default_sort_metric")
private val SHOW_AUDIO_QUALITY_BADGE = booleanPreferencesKey("show_audio_quality_badge")
private val SHOW_NOW_PLAYING_BLUR = booleanPreferencesKey("show_now_playing_blur")

private val LIBRARY_SORT_DIRECTION = stringPreferencesKey("library_sort_direction")
private val LIBRARY_COLLECTION_LAYOUT = stringPreferencesKey("library_collection_layout")
private val LIBRARY_SONG_FILTER = stringPreferencesKey("library_song_filter")
private val LIBRARY_ARTIST_SORT_ORDER = stringPreferencesKey("library_artist_sort_order")
private val LIBRARY_ARTIST_SORT_DIRECTION = stringPreferencesKey("library_artist_sort_direction")
private val LIBRARY_ALBUM_SORT_ORDER = stringPreferencesKey("library_album_sort_order")
private val LIBRARY_ALBUM_SORT_DIRECTION = stringPreferencesKey("library_album_sort_direction")
private val LIBRARY_GENRE_SORT_ORDER = stringPreferencesKey("library_genre_sort_order")
private val LIBRARY_GENRE_SORT_DIRECTION = stringPreferencesKey("library_genre_sort_direction")

// Gestures
private val SWIPE_UP_LYRICS_ENABLED = booleanPreferencesKey("swipe_up_lyrics_enabled")

// Lyrics Customization
private val LYRICS_ALIGNMENT = stringPreferencesKey("lyrics_alignment")
private val LYRICS_ACTIVE_STYLE = stringPreferencesKey("lyrics_active_style")

// Cache & Storage
private val MAX_CACHE_SIZE_MB = intPreferencesKey("max_cache_size_mb")
private val AUTO_CACHE_FAVORITES = booleanPreferencesKey("auto_cache_favorites")

private val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
private val LISTENING_STATS_ENABLED = booleanPreferencesKey("listening_stats_enabled")
private val DEFAULT_START_SCREEN = stringPreferencesKey("default_start_screen")
private val LAST_OPENED_ROUTE = stringPreferencesKey("last_opened_route")
private val RESUME_QUEUE_ON_LAUNCH = booleanPreferencesKey("resume_queue_on_launch")
private val REPLAY_GAIN_ENABLED = booleanPreferencesKey("replay_gain_enabled")
private val PAUSE_ON_DISCONNECT = booleanPreferencesKey("pause_on_disconnect")
private val KEEP_SCREEN_ON_LYRICS = booleanPreferencesKey("keep_screen_on_lyrics")
private val LYRICS_FONT_SIZE = stringPreferencesKey("lyrics_font_size")
private val LOCAL_LRC_PRIORITY = booleanPreferencesKey("local_lrc_priority")
private val IGNORE_SHORT_AUDIO_CLIPS = booleanPreferencesKey("ignore_short_audio_clips")
private val SHOW_HOME_RECENTLY_PLAYED = booleanPreferencesKey("show_home_recently_played")
private val SHOW_HOME_HEAVY_ROTATION = booleanPreferencesKey("show_home_heavy_rotation")
private val SHOW_HOME_RECENTLY_ADDED = booleanPreferencesKey("show_home_recently_added")
private val SHOW_HOME_FORGOTTEN_FAVORITES = booleanPreferencesKey("show_home_forgotten_favorites")
private val SHOW_MINI_PLAYER_PROGRESS = booleanPreferencesKey("show_mini_player_progress")

// Radio
private val RADIO_SORT_ORDER = stringPreferencesKey("radio_sort_order")
private val RADIO_SORT_DIRECTION = stringPreferencesKey("radio_sort_direction")
private val RADIO_SELECTED_COUNTRY = stringPreferencesKey("radio_selected_country")
private val RADIO_SELECTED_STATE = stringPreferencesKey("radio_selected_state")
private val RADIO_SELECTED_LANGUAGE = stringPreferencesKey("radio_selected_language")
private val RADIO_SELECTED_TAG = stringPreferencesKey("radio_selected_tag")
private val RADIO_DEFAULT_COUNTRY_APPLIED = booleanPreferencesKey("radio_default_country_applied")

enum class ThemeAccent(val label: String, val colorValue: Long, val containerValue: Long) {
    RECORD_RED("Record Red", 0xFFC23B30, 0xFF3A1512),
    CASSETTE_AMBER("Cassette Amber", 0xFFF59E0B, 0xFF3D2606),
    ELECTRIC_CYAN("Electric Cyan", 0xFF06B6D4, 0xFF082F3B),
    NEON_EMERALD("Neon Emerald", 0xFF10B981, 0xFF062E20),
    TAPE_MAGENTA("Tape Magenta", 0xFFEC4899, 0xFF3B0D24),
    MONOCHROME_SILVER("Monochrome", 0xFFC4C4C0, 0xFF262624),
    CUSTOM("Custom", 0xFFC23B30, 0xFF3A1512)
}

enum class DefaultLibraryTab(val label: String, val pageIndex: Int) {
    SONGS("Songs", 0),
    ARTISTS("Artists", 1),
    ALBUMS("Albums", 2),
    GENRES("Genres", 3),
    PLAYLISTS("Playlists", 4)
}

enum class AlbumArtCornerStyle(val label: String, val radiusDp: Int) {
    CURVED("Curved (16dp)", 16),
    SOFT("Soft (8dp)", 8),
    SQUARE("Vinyl / Square (0dp)", 0)
}

enum class DefaultStartScreen(val label: String, val routeName: String) {
    HOME("Home", "home"),
    LIBRARY("Library", "library"),
    LAST_OPENED("Last active tab", "")
}

enum class LyricsFontSize(val label: String, val scaleMultiplier: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Standard", 1.0f),
    LARGE("Large", 1.25f)
}

enum class TrackRowDensity(val label: String) {
    DETAILED("Detailed"),
    COMPACT("Compact")
}

enum class DefaultSortMetric(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album")
}

enum class LyricsAlignment(val label: String) {
    CENTER("Centre (Karaoke)"),
    LEFT("Left (Editorial)")
}

enum class LyricsActiveStyle(val label: String) {
    ACCENT_GLOW("Accent Glow"),
    CLEAN_WHITE("Pure White")
}

data class AppPreferences(
    val themeAccent: ThemeAccent = ThemeAccent.RECORD_RED,
    val customAccentColor: Long = ThemeAccent.RECORD_RED.colorValue,
    val amoledDarkTheme: Boolean = false,
    val defaultLibraryTab: DefaultLibraryTab = DefaultLibraryTab.SONGS,
    val albumArtCornerRadiusDp: Int = 16,
    val showRemainingTime: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    // Audio Engine & Transitions
    val crossfadeSeconds: Int = 0,
    val gaplessPlayback: Boolean = true,
    val replayGainPreAmpDb: Int = 0,
    val monoAudio: Boolean = false,
    val skipSilenceEnabled: Boolean = false,
    val autoplayEnabled: Boolean = false,
    // Library & Layout
    val gridColumnCount: Int = 2,
    val trackRowDensity: TrackRowDensity = TrackRowDensity.DETAILED,
    val defaultSortMetric: DefaultSortMetric = DefaultSortMetric.TITLE,
    val showAudioQualityBadge: Boolean = true,
    val showNowPlayingBlur: Boolean = true,
    val librarySortDirection: String = "ASCENDING",
    val libraryCollectionLayout: String = "GRID",
    val librarySongFilter: String = "ALL",
    val libraryArtistSortOrder: String = "NAME",
    val libraryArtistSortDirection: String = "ASCENDING",
    val libraryAlbumSortOrder: String = "ALBUM",
    val libraryAlbumSortDirection: String = "ASCENDING",
    val libraryGenreSortOrder: String = "NAME",
    val libraryGenreSortDirection: String = "ASCENDING",
    // Gestures
    val swipeUpLyricsEnabled: Boolean = true,
    // Lyrics Customization
    val lyricsAlignment: LyricsAlignment = LyricsAlignment.CENTER,
    val lyricsActiveStyle: LyricsActiveStyle = LyricsActiveStyle.ACCENT_GLOW,
    // Cache & Storage
    val maxCacheSizeMb: Int = 2048,
    val autoCacheFavorites: Boolean = false,
    // Standard
    val wifiOnlyDownloads: Boolean = false,
    val listeningStatsEnabled: Boolean = true,
    val defaultStartScreen: DefaultStartScreen = DefaultStartScreen.HOME,
    val lastOpenedRoute: String = "home",
    val resumeQueueOnLaunch: Boolean = true,
    val replayGainEnabled: Boolean = true,
    val pauseOnHeadphoneDisconnect: Boolean = true,
    val keepScreenOnLyrics: Boolean = false,
    val lyricsFontSize: LyricsFontSize = LyricsFontSize.MEDIUM,
    val localLrcPriority: Boolean = true,
    val ignoreShortAudioClips: Boolean = false,
    val showHomeRecentlyPlayed: Boolean = true,
    val showHomeHeavyRotation: Boolean = true,
    val showHomeRecentlyAdded: Boolean = true,
    val showHomeForgottenFavorites: Boolean = true,
    val showMiniPlayerProgress: Boolean = true,
    // Radio
    val radioSortOrder: String = "POPULARITY",
    val radioSortDirection: String = "DESCENDING",
    val radioSelectedCountry: String = "",
    val radioSelectedState: String = "",
    val radioSelectedLanguage: String = "",
    val radioSelectedTag: String = "",
    val radioDefaultCountryApplied: Boolean = false
)

class AppPreferencesRepository(private val context: Context) {
    val preferences: Flow<AppPreferences> = context.appPreferencesDataStore.data.map { prefs ->
        AppPreferences(
            themeAccent = try {
                ThemeAccent.valueOf(prefs[THEME_ACCENT] ?: ThemeAccent.RECORD_RED.name)
            } catch (_: Exception) {
                ThemeAccent.RECORD_RED
            },
            customAccentColor = prefs[THEME_ACCENT_CUSTOM_COLOR] ?: ThemeAccent.RECORD_RED.colorValue,
            amoledDarkTheme = prefs[AMOLED_DARK_THEME] ?: false,
            defaultLibraryTab = try {
                DefaultLibraryTab.valueOf(prefs[DEFAULT_LIBRARY_TAB] ?: DefaultLibraryTab.SONGS.name)
            } catch (_: Exception) {
                DefaultLibraryTab.SONGS
            },
            albumArtCornerRadiusDp = prefs[ALBUM_ART_CORNER_RADIUS] ?: 16,
            showRemainingTime = prefs[SHOW_REMAINING_TIME] ?: false,
            hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK_ENABLED] ?: true,
            // Audio Engine & Transitions
            crossfadeSeconds = prefs[CROSSFADE_SECONDS] ?: 0,
            gaplessPlayback = prefs[GAPLESS_PLAYBACK] ?: true,
            replayGainPreAmpDb = prefs[REPLAY_GAIN_PRE_AMP_DB] ?: 0,
            monoAudio = prefs[MONO_AUDIO] ?: false,
            skipSilenceEnabled = prefs[SKIP_SILENCE_ENABLED] ?: false,
            autoplayEnabled = prefs[AUTOPLAY_ENABLED] ?: false,
            // Library & Layout
            gridColumnCount = (prefs[GRID_COLUMNS] ?: 2).coerceIn(2, 4),
            trackRowDensity = try {
                TrackRowDensity.valueOf(prefs[TRACK_ROW_DENSITY] ?: TrackRowDensity.DETAILED.name)
            } catch (_: Exception) {
                TrackRowDensity.DETAILED
            },
            defaultSortMetric = try {
                DefaultSortMetric.valueOf(prefs[DEFAULT_SORT_METRIC] ?: DefaultSortMetric.TITLE.name)
            } catch (_: Exception) {
                DefaultSortMetric.TITLE
            },
            showAudioQualityBadge = prefs[SHOW_AUDIO_QUALITY_BADGE] ?: true,
            showNowPlayingBlur = prefs[SHOW_NOW_PLAYING_BLUR] ?: true,
            librarySortDirection = prefs[LIBRARY_SORT_DIRECTION] ?: "ASCENDING",
            libraryCollectionLayout = prefs[LIBRARY_COLLECTION_LAYOUT] ?: "GRID",
            librarySongFilter = prefs[LIBRARY_SONG_FILTER] ?: "ALL",
            libraryArtistSortOrder = prefs[LIBRARY_ARTIST_SORT_ORDER] ?: "NAME",
            libraryArtistSortDirection = prefs[LIBRARY_ARTIST_SORT_DIRECTION] ?: "ASCENDING",
            libraryAlbumSortOrder = prefs[LIBRARY_ALBUM_SORT_ORDER] ?: "ALBUM",
            libraryAlbumSortDirection = prefs[LIBRARY_ALBUM_SORT_DIRECTION] ?: "ASCENDING",
            libraryGenreSortOrder = prefs[LIBRARY_GENRE_SORT_ORDER] ?: "NAME",
            libraryGenreSortDirection = prefs[LIBRARY_GENRE_SORT_DIRECTION] ?: "ASCENDING",
            // Gestures
            swipeUpLyricsEnabled = prefs[SWIPE_UP_LYRICS_ENABLED] ?: true,
            // Lyrics Customization
            lyricsAlignment = try {
                LyricsAlignment.valueOf(prefs[LYRICS_ALIGNMENT] ?: LyricsAlignment.CENTER.name)
            } catch (_: Exception) {
                LyricsAlignment.CENTER
            },
            lyricsActiveStyle = try {
                LyricsActiveStyle.valueOf(prefs[LYRICS_ACTIVE_STYLE] ?: LyricsActiveStyle.ACCENT_GLOW.name)
            } catch (_: Exception) {
                LyricsActiveStyle.ACCENT_GLOW
            },
            // Cache & Storage
            maxCacheSizeMb = prefs[MAX_CACHE_SIZE_MB] ?: 2048,
            autoCacheFavorites = prefs[AUTO_CACHE_FAVORITES] ?: false,
            // Standard
            wifiOnlyDownloads = prefs[WIFI_ONLY_DOWNLOADS] ?: false,
            listeningStatsEnabled = prefs[LISTENING_STATS_ENABLED] ?: true,
            defaultStartScreen = try {
                DefaultStartScreen.valueOf(prefs[DEFAULT_START_SCREEN] ?: DefaultStartScreen.HOME.name)
            } catch (_: Exception) {
                DefaultStartScreen.HOME
            },
            lastOpenedRoute = prefs[LAST_OPENED_ROUTE] ?: "home",
            resumeQueueOnLaunch = prefs[RESUME_QUEUE_ON_LAUNCH] ?: true,
            replayGainEnabled = prefs[REPLAY_GAIN_ENABLED] ?: true,
            pauseOnHeadphoneDisconnect = prefs[PAUSE_ON_DISCONNECT] ?: true,
            keepScreenOnLyrics = prefs[KEEP_SCREEN_ON_LYRICS] ?: false,
            lyricsFontSize = try {
                LyricsFontSize.valueOf(prefs[LYRICS_FONT_SIZE] ?: LyricsFontSize.MEDIUM.name)
            } catch (_: Exception) {
                LyricsFontSize.MEDIUM
            },
            localLrcPriority = prefs[LOCAL_LRC_PRIORITY] ?: true,
            ignoreShortAudioClips = prefs[IGNORE_SHORT_AUDIO_CLIPS] ?: false,
            showHomeRecentlyPlayed = prefs[SHOW_HOME_RECENTLY_PLAYED] ?: true,
            showHomeHeavyRotation = prefs[SHOW_HOME_HEAVY_ROTATION] ?: true,
            showHomeRecentlyAdded = prefs[SHOW_HOME_RECENTLY_ADDED] ?: true,
            showHomeForgottenFavorites = prefs[SHOW_HOME_FORGOTTEN_FAVORITES] ?: true,
            showMiniPlayerProgress = prefs[SHOW_MINI_PLAYER_PROGRESS] ?: true,
            // Radio
            radioSortOrder = prefs[RADIO_SORT_ORDER] ?: "POPULARITY",
            radioSortDirection = prefs[RADIO_SORT_DIRECTION] ?: "DESCENDING",
            radioSelectedCountry = prefs[RADIO_SELECTED_COUNTRY] ?: "",
            radioSelectedState = prefs[RADIO_SELECTED_STATE] ?: "",
            radioSelectedLanguage = prefs[RADIO_SELECTED_LANGUAGE] ?: "",
            radioSelectedTag = prefs[RADIO_SELECTED_TAG] ?: "",
            radioDefaultCountryApplied = prefs[RADIO_DEFAULT_COUNTRY_APPLIED] ?: false
        )
    }

    suspend fun setThemeAccent(accent: ThemeAccent) {
        context.appPreferencesDataStore.edit { it[THEME_ACCENT] = accent.name }
    }

    suspend fun setCustomAccentColor(colorValue: Long) {
        context.appPreferencesDataStore.edit { it[THEME_ACCENT_CUSTOM_COLOR] = colorValue }
    }

    suspend fun setCustomAccentColorAndAccent(colorValue: Long) {
        context.appPreferencesDataStore.edit {
            it[THEME_ACCENT_CUSTOM_COLOR] = colorValue
            it[THEME_ACCENT] = ThemeAccent.CUSTOM.name
        }
    }

    suspend fun setAmoledDarkTheme(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[AMOLED_DARK_THEME] = enabled }
    }

    suspend fun setDefaultLibraryTab(tab: DefaultLibraryTab) {
        context.appPreferencesDataStore.edit { it[DEFAULT_LIBRARY_TAB] = tab.name }
    }

    suspend fun setAlbumArtCornerRadiusDp(radius: Int) {
        context.appPreferencesDataStore.edit { it[ALBUM_ART_CORNER_RADIUS] = radius }
    }

    suspend fun setShowRemainingTime(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SHOW_REMAINING_TIME] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[HAPTIC_FEEDBACK_ENABLED] = enabled }
    }

    // Audio Engine & Transitions
    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.appPreferencesDataStore.edit { it[CROSSFADE_SECONDS] = seconds }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setReplayGainPreAmpDb(db: Int) {
        context.appPreferencesDataStore.edit { it[REPLAY_GAIN_PRE_AMP_DB] = db }
    }

    suspend fun setMonoAudio(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[MONO_AUDIO] = enabled }
    }

    suspend fun setSkipSilenceEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SKIP_SILENCE_ENABLED] = enabled }
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[AUTOPLAY_ENABLED] = enabled }
    }

    // Library & Layout
    suspend fun setGridColumnCount(columns: Int) {
        context.appPreferencesDataStore.edit { it[GRID_COLUMNS] = columns }
    }

    suspend fun setTrackRowDensity(density: TrackRowDensity) {
        context.appPreferencesDataStore.edit { it[TRACK_ROW_DENSITY] = density.name }
    }

    suspend fun setDefaultSortMetric(metric: DefaultSortMetric) {
        context.appPreferencesDataStore.edit { it[DEFAULT_SORT_METRIC] = metric.name }
    }

    suspend fun setShowAudioQualityBadge(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SHOW_AUDIO_QUALITY_BADGE] = enabled }
    }

    suspend fun setShowNowPlayingBlur(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SHOW_NOW_PLAYING_BLUR] = enabled }
    }

    suspend fun setLibrarySortDirection(direction: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_SORT_DIRECTION] = direction }
    }

    suspend fun setLibraryCollectionLayout(layout: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_COLLECTION_LAYOUT] = layout }
    }

    suspend fun setLibrarySongFilter(filter: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_SONG_FILTER] = filter }
    }

    suspend fun setLibraryArtistSortOrder(order: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_ARTIST_SORT_ORDER] = order }
    }

    suspend fun setLibraryArtistSortDirection(direction: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_ARTIST_SORT_DIRECTION] = direction }
    }

    suspend fun setLibraryAlbumSortOrder(order: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_ALBUM_SORT_ORDER] = order }
    }

    suspend fun setLibraryAlbumSortDirection(direction: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_ALBUM_SORT_DIRECTION] = direction }
    }

    suspend fun setLibraryGenreSortOrder(order: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_GENRE_SORT_ORDER] = order }
    }

    suspend fun setLibraryGenreSortDirection(direction: String) {
        context.appPreferencesDataStore.edit { it[LIBRARY_GENRE_SORT_DIRECTION] = direction }
    }

    // Gestures
    suspend fun setSwipeUpLyricsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SWIPE_UP_LYRICS_ENABLED] = enabled }
    }

    // Lyrics Customization
    suspend fun setLyricsAlignment(alignment: LyricsAlignment) {
        context.appPreferencesDataStore.edit { it[LYRICS_ALIGNMENT] = alignment.name }
    }

    suspend fun setLyricsActiveStyle(style: LyricsActiveStyle) {
        context.appPreferencesDataStore.edit { it[LYRICS_ACTIVE_STYLE] = style.name }
    }

    // Cache & Storage
    suspend fun setMaxCacheSizeMb(sizeMb: Int) {
        context.appPreferencesDataStore.edit { it[MAX_CACHE_SIZE_MB] = sizeMb }
    }

    suspend fun setAutoCacheFavorites(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[AUTO_CACHE_FAVORITES] = enabled }
    }

    // Standard
    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[WIFI_ONLY_DOWNLOADS] = enabled }
    }

    suspend fun setListeningStatsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[LISTENING_STATS_ENABLED] = enabled }
    }

    suspend fun setDefaultStartScreen(screen: DefaultStartScreen) {
        context.appPreferencesDataStore.edit { it[DEFAULT_START_SCREEN] = screen.name }
    }

    suspend fun setLastOpenedRoute(route: String) {
        context.appPreferencesDataStore.edit { it[LAST_OPENED_ROUTE] = route }
    }

    suspend fun setResumeQueueOnLaunch(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[RESUME_QUEUE_ON_LAUNCH] = enabled }
    }

    suspend fun setReplayGainEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[REPLAY_GAIN_ENABLED] = enabled }
    }

    suspend fun setPauseOnHeadphoneDisconnect(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[PAUSE_ON_DISCONNECT] = enabled }
    }

    suspend fun setKeepScreenOnLyrics(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[KEEP_SCREEN_ON_LYRICS] = enabled }
    }

    suspend fun setLyricsFontSize(size: LyricsFontSize) {
        context.appPreferencesDataStore.edit { it[LYRICS_FONT_SIZE] = size.name }
    }

    suspend fun setLocalLrcPriority(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[LOCAL_LRC_PRIORITY] = enabled }
    }

    suspend fun setIgnoreShortAudioClips(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[IGNORE_SHORT_AUDIO_CLIPS] = enabled }
    }

    suspend fun setShowHomeRecentlyPlayed(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SHOW_HOME_RECENTLY_PLAYED] = enabled }
    }

    suspend fun setShowHomeHeavyRotation(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SHOW_HOME_HEAVY_ROTATION] = enabled }
    }

    suspend fun setShowHomeRecentlyAdded(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SHOW_HOME_RECENTLY_ADDED] = enabled }
    }

    suspend fun setShowHomeForgottenFavorites(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SHOW_HOME_FORGOTTEN_FAVORITES] = enabled }
    }

    suspend fun setShowMiniPlayerProgress(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[SHOW_MINI_PLAYER_PROGRESS] = enabled }
    }

    // Radio
    suspend fun setRadioSortOrder(order: String) {
        context.appPreferencesDataStore.edit { it[RADIO_SORT_ORDER] = order }
    }

    suspend fun setRadioSortDirection(direction: String) {
        context.appPreferencesDataStore.edit { it[RADIO_SORT_DIRECTION] = direction }
    }

    suspend fun setRadioSelectedCountry(country: String) {
        context.appPreferencesDataStore.edit { it[RADIO_SELECTED_COUNTRY] = country }
    }

    suspend fun setRadioSelectedState(state: String) {
        context.appPreferencesDataStore.edit { it[RADIO_SELECTED_STATE] = state }
    }

    suspend fun setRadioSelectedLanguage(language: String) {
        context.appPreferencesDataStore.edit { it[RADIO_SELECTED_LANGUAGE] = language }
    }

    suspend fun setRadioSelectedTag(tag: String) {
        context.appPreferencesDataStore.edit { it[RADIO_SELECTED_TAG] = tag }
    }

    suspend fun setRadioDefaultCountryApplied(applied: Boolean) {
        context.appPreferencesDataStore.edit { it[RADIO_DEFAULT_COUNTRY_APPLIED] = applied }
    }
}
