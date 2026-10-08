@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.ui.playback

import android.app.Application
import android.graphics.Bitmap
import android.media.AudioManager
import android.util.Base64
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.LibraryRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.isFromAnotherDevice
import `in`.caffeinelabs.cassettecat.data.library.local.LocalLibraryRepository
import `in`.caffeinelabs.cassettecat.data.download.DownloadSettingsRepository
import `in`.caffeinelabs.cassettecat.data.device.DesktopRemoteRepository
import `in`.caffeinelabs.cassettecat.data.device.DesktopRemoteState
import `in`.caffeinelabs.cassettecat.data.device.HandoffTrack
import `in`.caffeinelabs.cassettecat.data.device.findAllInLibrary
import `in`.caffeinelabs.cassettecat.data.device.matchInLibrary
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomRole
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.listeningroom.LocalListeningRoomRepository
import `in`.caffeinelabs.cassettecat.data.listeningroom.NearbyListeningRoom
import `in`.caffeinelabs.cassettecat.data.listeningroom.RoomSnapshot
import `in`.caffeinelabs.cassettecat.data.listeningroom.RoomTrack
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerController
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerSettingsRepository
import `in`.caffeinelabs.cassettecat.data.playback.EmbeddedLyricsLoader
import `in`.caffeinelabs.cassettecat.data.playback.LocalLrcLoader
import `in`.caffeinelabs.cassettecat.data.playback.LrcLibClient
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackRepository
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackStateRepository
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackUiState
import `in`.caffeinelabs.cassettecat.data.radio.toRadioStation
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.stats.Listen
import `in`.caffeinelabs.cassettecat.data.stats.ListeningStatsRepository
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.data.streaming.jellyfin.JellyfinLibraryRepository
import `in`.caffeinelabs.cassettecat.data.streaming.subsonic.SubsonicLibraryRepository
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
import `in`.caffeinelabs.cassettecat.ui.screens.library.splitArtists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random

private const val POSITION_TICK_ACTIVE_MS = 250L
private const val POSITION_TICK_IDLE_MS = 1000L
private const val SAVE_EVERY_N_TICKS = 40 // ~10s at POSITION_TICK_ACTIVE_MS
private const val PLAY_COUNT_SHARE = 0.9
private const val UNKNOWN_LENGTH_PLAY_MS = 30_000L
private const val SCROBBLE_MAX_WAIT_MS = 4 * 60 * 1000L
private const val SCROBBLE_MIN_LENGTH_MS = 30_000L
private const val REPLAY_START_MS = 5_000L

internal fun countsAsPlay(listenedMs: Long, durationMs: Long): Boolean =
    listenedMs >= if (durationMs > 0) (durationMs * PLAY_COUNT_SHARE).toLong() else UNKNOWN_LENGTH_PLAY_MS

internal fun countsAsScrobble(listenedMs: Long, durationMs: Long): Boolean =
    durationMs > SCROBBLE_MIN_LENGTH_MS && listenedMs >= minOf(durationMs / 2, SCROBBLE_MAX_WAIT_MS)
private const val AUTOPLAY_BATCH_SIZE = 20
private const val AUTOPLAY_SAME_ARTIST_WEIGHT = 4.0
private const val AUTOPLAY_COLLABORATION_WEIGHT = 2.5
private const val AUTOPLAY_SHARED_GENRE_WEIGHT = 2.0
private const val AUTOPLAY_RELATED_GENRE_WEIGHT = 1.0
private const val AUTOPLAY_ERA_PROXIMITY_WEIGHT = 1.0
private const val AUTOPLAY_FAVORITE_WEIGHT = 1.5

private class CurrentListen(val song: Song) {
    var listenedMs = 0L
    var counted = false
    var scrobbled = false
    var positionMs = 0L
}
private data class LyricsRequest(val song: Song?, val embeddedLyrics: String?, val lrcLibEnabled: Boolean)

internal fun instantMixAffinity(
    seedArtist: String,
    seedGenres: List<String>,
    candidateArtist: String,
    candidateGenres: List<String>,
    seedYear: Int? = null,
    candidateYear: Int? = null
): Int {
    val normalizedSeedGenres = seedGenres.map { it.lowercase() }.toSet()
    val sharedGenres = candidateGenres.map { it.lowercase() }.toSet().count { it in normalizedSeedGenres }
    val seedCollaborators = seedArtist.splitArtists().map { it.lowercase() }.toSet()
    val candCollaborators = candidateArtist.splitArtists().map { it.lowercase() }.toSet()
    val artistBonus = if (candidateArtist.equals(seedArtist, ignoreCase = true)) {
        2
    } else if (seedCollaborators.intersect(candCollaborators).isNotEmpty()) {
        1
    } else {
        0
    }
    val yearBonus = if (seedYear != null && candidateYear != null && kotlin.math.abs(seedYear - candidateYear) <= 5) 1 else 0
    return sharedGenres * 3 + artistBonus + yearBonus
}

internal fun buildInstantMix(seed: Song, library: List<Song>, limit: Int = 25): List<Song> {
    if (limit <= 0) return emptyList()
    val candidates = library.distinctBy { it.id }.filter {
        it.id != seed.id && it.source != MusicSource.Radio && it.source != MusicSource.ListeningRoomHost
    }
    val ranked = candidates.sortedWith(
        compareByDescending<Song> { candidate ->
            instantMixAffinity(seed.artist, seed.genres, candidate.artist, candidate.genres, seed.releaseYear, candidate.releaseYear)
        }.thenBy { (seed.id + it.id).hashCode() }
    )
    return (listOf(seed) + ranked).take(limit)
}

// Enough to carry on listening without sending a whole library.
private const val HANDOFF_QUEUE_LIMIT = 100
private const val DESKTOP_CHECK_IN_MS = 1_500L
// Kept under the six seconds after which the computer stops showing the phone.
private const val DESKTOP_BACKGROUND_CHECK_IN_MS = 4_000L
private const val DESKTOP_PAUSED_CHECK_IN_MS = 10 * 60 * 1000L
private const val DESKTOP_IDLE_CHECK_IN_MS = 30_000L

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = PlaybackRepository(app)
    private val listeningRoomRepository = LocalListeningRoomRepository(app)
    private val desktop = DesktopRemoteRepository.getInstance(app)

    // The phone's own player. Stats, scrobbling, saving and autoplay always read this.
    private val localState: StateFlow<PlaybackUiState> = repository.state

    // What the app shows and controls: the paired computer while the phone controls it, otherwise this phone.
    val playbackState: StateFlow<PlaybackUiState> = combine(localState, desktop.controlledState) { local, remote ->
        remote?.copy(history = local.history) ?: local
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.state.value)

    /** The computer being controlled, or null while the phone plays itself. */
    val controlledDesktop: StateFlow<DesktopRemoteState?> = combine(desktop.state, desktop.controlledState) { state, remote ->
        state.takeIf { remote != null }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val listeningRoom: StateFlow<ListeningRoomState> = listeningRoomRepository.state

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = combine(_positionMs, desktop.positionMs, desktop.controlledState) { local, remote, controlled ->
        if (controlled != null) remote else local
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val streamingServerRepository = StreamingServerRepository(app)
    private val credentialStore = CredentialStore(app)
    private val librariesBySource: Map<MusicSource, LibraryRepository> = mapOf(
        MusicSource.Local to LocalLibraryRepository(app),
        MusicSource.Subsonic to SubsonicLibraryRepository(streamingServerRepository, credentialStore),
        MusicSource.Jellyfin to JellyfinLibraryRepository(streamingServerRepository, credentialStore)
    )
    private val favoritesRepository = FavoritesRepository(app)
    // Radio favorites store station objects rather than song IDs.
    private val radioFavoritesRepository = `in`.caffeinelabs.cassettecat.data.radio.RadioFavoritesRepository(app)

    private val _isCurrentSongFavorite = MutableStateFlow(false)
    val isCurrentSongFavorite: StateFlow<Boolean> = _isCurrentSongFavorite.asStateFlow()

    private val lrcLibClient = LrcLibClient(app.cacheDir)
    private val localLrcLoader = LocalLrcLoader(app)
    private val embeddedLyricsLoader = EmbeddedLyricsLoader(app)
    private val serviceSettingsRepository = ServiceSettingsRepository(app)
    private val appPreferencesRepository = AppPreferencesRepository(app)
    private val appPreferences = appPreferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, `in`.caffeinelabs.cassettecat.data.settings.AppPreferences())
    private val autoDownloadFavorites = DownloadSettingsRepository(app).autoDownloadFavorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val _syncedLyrics = MutableStateFlow<List<LyricLine>?>(null)
    val syncedLyrics: StateFlow<List<LyricLine>?> = _syncedLyrics.asStateFlow()
    private val _fallbackLyrics = MutableStateFlow<String?>(null)
    val fallbackLyrics: StateFlow<String?> = _fallbackLyrics.asStateFlow()
    private val _lyricsProvider = MutableStateFlow<String?>(null)
    val lyricsProvider: StateFlow<String?> = _lyricsProvider.asStateFlow()
    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val stateRepository = PlaybackStateRepository(app)
    private val statsRepository = ListeningStatsRepository(app)
    val monthlyStats: StateFlow<Map<String, MonthlyStats>> = statsRepository.monthlyStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    private val equalizerSettingsRepository = EqualizerSettingsRepository(app)
    private val scrobbleManager = `in`.caffeinelabs.cassettecat.data.scrobble.ScrobbleManager(app, viewModelScope)
    private var hasAttemptedRestore = false
    private var currentListen: CurrentListen? = null

    // Media3 doesn't push continuous position updates, so poll while playing.
    private var tickerJob: Job? = null

    init {
        repository.onQueueExhausted = { viewModelScope.launch { maybeAutoplay() } }
        repository.onQueueLowWatermark = { viewModelScope.launch { maybeAutoplay() } }
        viewModelScope.launch {
            repository.connect()
        }
        viewModelScope.launch {
            localState.collect { state ->
                if (state.isPlaying) startTicker() else { stopTicker(); savePlaybackState() }
                if (listeningRoom.value.role == ListeningRoomRole.HOST) publishRoomSnapshot()
            }
        }
        viewModelScope.launch {
            desktop.state.map { it.controlling }.distinctUntilChanged().collect { controlling ->
                if (controlling && localState.value.isPlaying) repository.pause()
            }
        }
        // While this phone plays and a computer is paired, the computer sees it and can send it commands.
        viewModelScope.launch {
            combine(desktop.state, localState) { state, local ->
                local.currentSong?.takeIf { state.address != null && !state.controlling && !state.offlineBlackout && !it.isFromAnotherDevice }
                    ?.let { it to local.isPlaying }
            }.distinctUntilChanged().collectLatest { playing ->
                val (song, isPlaying) = playing ?: return@collectLatest
                // After a long pause the check-ins slow down to save battery; the computer keeps showing the phone.
                val idleAt = if (isPlaying) Long.MAX_VALUE else SystemClock.elapsedRealtime() + DESKTOP_PAUSED_CHECK_IN_MS
                var sendArtwork = true
                while (true) {
                    val artwork = if (sendArtwork) desktopArtwork(song) else null
                    val reply = desktop.checkIn(song, isPlaying, repository.currentPositionMs(), mediaVolumePercent(), artwork)
                    sendArtwork = reply.needsArtwork
                    reply.commands.forEach(::runDesktopCommand)
                    if (reply.playNext.isNotEmpty()) launch { playNextFromDesktop(reply.playNext) }
                    delay(
                        when {
                            SystemClock.elapsedRealtime() >= idleAt -> DESKTOP_IDLE_CHECK_IN_MS
                            desktop.isInFront -> DESKTOP_CHECK_IN_MS
                            else -> DESKTOP_BACKGROUND_CHECK_IN_MS
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            listeningRoomRepository.snapshots.collect { snapshot ->
                if (listeningRoom.value.role == ListeningRoomRole.GUEST) applyRoomSnapshot(snapshot)
            }
        }
        // Refresh position immediately on song transition when ticker is idle.
        viewModelScope.launch {
            localState.map { it.currentSong?.id }.distinctUntilChanged().collect {
                _positionMs.value = repository.currentPositionMs()
            }
        }
        viewModelScope.launch {
            combine(
                playbackState.map { it.currentSong }.distinctUntilChanged(),
                favoritesRepository.favoriteIds,
                radioFavoritesRepository.favoriteStations
            ) { song, favoriteIds, stations ->
                when (song?.source) {
                    null -> false
                    MusicSource.Radio -> stations.any { it.uuid == song.id.removePrefix("radio:") }
                    else -> song.id in favoriteIds
                }
            }.collect { _isCurrentSongFavorite.value = it }
        }
        viewModelScope.launch {
            localState.map { it.currentSong }.distinctUntilChanged().collect { song ->
                if (song != null) {
                    if (song.source != MusicSource.Radio) scrobbleManager.onTrackStarted(song)
                    savePlaybackState()
                }
            }
        }
        viewModelScope.launch {
            serviceSettingsRepository.settings.map { it.offlineBlackoutMode }
                .combine(localState.map { it.currentSong }) { offline, song -> offline to song }
                .collect { (offline, song) ->
                    if (offline && song != null && song.source != MusicSource.Local) {
                        pause()
                    }
                }
        }
        // only when there's no embedded lyrics for the current song
        viewModelScope.launch {
            playbackState.map { it.currentSong to it.currentLyrics }
                .combine(serviceSettingsRepository.settings) { (song, embedded), settings ->
                    LyricsRequest(song, embedded, settings.isEnabled(ExternalService.LRCLIB))
                }
                .distinctUntilChanged()
                .collectLatest { request ->
                    val song = request.song
                    val embedded = request.embeddedLyrics
                    _syncedLyrics.value = null
                    _fallbackLyrics.value = null
                    _lyricsProvider.value = null
                    _isLoadingLyrics.value = false
                    val prioritizeLocalLrc = appPreferences.value.localLrcPriority
                    if (prioritizeLocalLrc && song != null && song.source == MusicSource.Local) {
                        val localLrc = localLrcLoader.loadFor(song)
                        if (localLrc != null) {
                            _syncedLyrics.value = localLrc
                            _lyricsProvider.value = "Local file"
                        } else if (!embedded.isNullOrBlank()) {
                            _lyricsProvider.value = "Embedded metadata"
                        } else {
                            val localEmbedded = embeddedLyricsLoader.loadFor(song)
                            if (!localEmbedded.isNullOrBlank()) {
                                _fallbackLyrics.value = localEmbedded
                                _lyricsProvider.value = "Embedded metadata"
                            } else if (request.lrcLibEnabled) {
                                _isLoadingLyrics.value = true
                                try {
                                    lrcLibClient.fetchLyrics(song.artist, song.title, song.album)?.let { result ->
                                        _syncedLyrics.value = result.syncedLyrics
                                        _fallbackLyrics.value = result.plainLyrics
                                        _lyricsProvider.value = "LRCLIB"
                                    }
                                } finally {
                                    _isLoadingLyrics.value = false
                                }
                            }
                        }
                    } else if (!embedded.isNullOrBlank()) {
                        _lyricsProvider.value = "Embedded metadata"
                    } else if (song != null) {
                        val localEmbedded = if (song.source == MusicSource.Local) embeddedLyricsLoader.loadFor(song) else null
                        if (!localEmbedded.isNullOrBlank()) {
                            _fallbackLyrics.value = localEmbedded
                            _lyricsProvider.value = "Embedded metadata"
                        } else {
                            val localLrc = if (song.source == MusicSource.Local) localLrcLoader.loadFor(song) else null
                            if (localLrc != null) {
                                _syncedLyrics.value = localLrc
                                _lyricsProvider.value = "Local file"
                            } else if (request.lrcLibEnabled) {
                                _isLoadingLyrics.value = true
                                try {
                                    lrcLibClient.fetchLyrics(song.artist, song.title, song.album)?.let { result ->
                                        _syncedLyrics.value = result.syncedLyrics
                                        _fallbackLyrics.value = result.plainLyrics
                                        _lyricsProvider.value = "LRCLIB"
                                    }
                                } finally {
                                    _isLoadingLyrics.value = false
                                }
                            }
                        }
                    }
                }
        }
        viewModelScope.launch {
            localState.map { it.audioSessionId }.distinctUntilChanged().collect { sessionId ->
                if (sessionId == C.AUDIO_SESSION_ID_UNSET) return@collect
                val levels = equalizerSettingsRepository.levels.first()
                EqualizerController.attach(
                    audioSessionId = sessionId,
                    bassBoostActive = levels.bassBoostStrength > 0,
                    virtualizerActive = levels.virtualizerStrength > 0,
                    loudnessActive = levels.loudnessNormalization || levels.preampGainMb > 0
                )
                EqualizerController.setMasterEnabled(levels.enabled)
                levels.bandLevelsMb.forEachIndexed { band, levelMb -> EqualizerController.setBandLevel(band, levelMb) }
                EqualizerController.setBassBoostStrength(levels.bassBoostStrength)
                EqualizerController.setVirtualizerStrength(levels.virtualizerStrength)
                EqualizerController.setPreampGainMb(levels.preampGainMb)
                EqualizerController.setLoudnessNormalization(levels.loudnessNormalization, levels.preampGainMb)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (isFollowingRoomHost()) return
        desktop.setControlling(false)
        viewModelScope.launch { repository.playQueue(songs, startIndex, shuffle) }
    }

    fun shuffleAll(songs: List<Song>) {
        if (isFollowingRoomHost() || songs.isEmpty()) return
        desktop.setControlling(false)
        viewModelScope.launch { repository.shuffleAll(songs) }
    }

    fun playInstantMix(seed: Song, library: List<Song>) {
        playQueue(buildInstantMix(seed, library), 0)
    }

    // once per process, and only into an idle session
    fun restoreIfNeeded(allSongs: List<Song>) {
        if (!appPreferences.value.resumeQueueOnLaunch || allSongs.isEmpty() || hasAttemptedRestore) return
        hasAttemptedRestore = true
        viewModelScope.launch {
            val saved = stateRepository.load() ?: return@launch
            val songsById = allSongs.associateBy { it.id }
            if (saved.historySongIds.isNotEmpty()) {
                val resolvedHistory = saved.historySongIds.mapNotNull { songsById[it] }
                if (resolvedHistory.isNotEmpty()) {
                    repository.restoreHistory(resolvedHistory)
                }
            }
            if (localState.value.currentSong != null) return@launch
            val resolvedSongs = saved.queueSongIds.mapNotNull { songsById[it] }
            if (resolvedSongs.isEmpty()) return@launch
            // Skip songs deleted from storage while keeping relative queue ordering.
            val adjustedIndex = saved.queueSongIds.take(saved.currentIndex).count { it in songsById }
            repository.restoreQueue(resolvedSongs, adjustedIndex.coerceIn(0, resolvedSongs.size - 1), saved.positionMs)
        }
    }
    private var sleepTimerJob: Job? = null
    private var sleepFading = false
    private val _sleepTimerEndMs = MutableStateFlow<Long?>(null)
    val sleepTimerEndMs: StateFlow<Long?> = _sleepTimerEndMs.asStateFlow()

    private val _sleepTimerFadeOut = MutableStateFlow(true)
    val sleepTimerFadeOut: StateFlow<Boolean> = _sleepTimerFadeOut.asStateFlow()

    private val _sleepTimerFinishTrack = MutableStateFlow(false)
    val sleepTimerFinishTrack: StateFlow<Boolean> = _sleepTimerFinishTrack.asStateFlow()

    private val _sleepTimerFadeSeconds = MutableStateFlow(30)
    val sleepTimerFadeSeconds: StateFlow<Int> = _sleepTimerFadeSeconds.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        repository.setPlaybackSpeed(speed)
    }

    fun setPlaybackPitch(pitch: Float) {
        _playbackPitch.value = pitch
        repository.setPlaybackPitch(pitch)
    }

    fun setSleepTimerFadeOut(enabled: Boolean) {
        _sleepTimerFadeOut.value = enabled
    }

    fun setSleepTimerFinishTrack(enabled: Boolean) {
        _sleepTimerFinishTrack.value = enabled
    }

    fun setSleepTimerFadeSeconds(seconds: Int) {
        _sleepTimerFadeSeconds.value = seconds
    }

    fun startSleepTimer(
        durationMs: Long,
        finishTrack: Boolean = _sleepTimerFinishTrack.value,
        fadeOut: Boolean = _sleepTimerFadeOut.value,
        fadeSeconds: Int = _sleepTimerFadeSeconds.value
    ) {
        sleepTimerJob?.cancel()
        val effectiveDurationMs = if (durationMs == -1L) {
            (localState.value.durationMs - _positionMs.value).coerceAtLeast(1_000L)
        } else {
            durationMs
        }
        _sleepTimerEndMs.value = SystemClock.elapsedRealtime() + effectiveDurationMs
        sleepTimerJob = viewModelScope.launch {
            var fadeStarted = false
            var initialVol = 0f
            try {
                val fadeMs = if (fadeOut) (fadeSeconds * 1000L).coerceAtMost(effectiveDurationMs / 2) else 0L
                val preFadeMs = (effectiveDurationMs - fadeMs).coerceAtLeast(0L)
                if (preFadeMs > 0) {
                    delay(preFadeMs)
                }
                if (fadeOut && fadeMs > 0) {
                    sleepFading = true
                    repository.setVolumeOverrideActive(true)
                    initialVol = repository.getVolume()
                    fadeStarted = true
                    val steps = 20
                    val stepDelay = fadeMs / steps
                    for (i in 1..steps) {
                        delay(stepDelay)
                        val factor = 1f - (i.toFloat() / steps.toFloat())
                        repository.setVolume(initialVol * factor)
                    }
                    if (localState.value.isPlaying) togglePlayPause()
                } else {
                    if (localState.value.isPlaying) togglePlayPause()
                }
            } finally {
                if (fadeStarted) {
                    repository.setVolume(initialVol)
                    repository.setVolumeOverrideActive(false)
                    sleepFading = false
                }
                if (coroutineContext[Job] === sleepTimerJob) {
                    sleepTimerJob = null
                    _sleepTimerEndMs.value = null
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerEndMs.value = null
    }

    /** Continues this phone's song and queue on the computer, then controls the computer from here; returns whether it took them. */
    suspend fun transferToDesktop(): Boolean {
        val local = localState.value
        val current = local.currentSong
        // With a song to carry over, control moves only once the computer has taken it.
        val handedOver = current == null || current.source == MusicSource.Radio || current.isFromAnotherDevice ||
            desktop.handOff(listOf(current) + local.upNext.take(HANDOFF_QUEUE_LIMIT), repository.currentPositionMs(), local.isPlaying)
        if (handedOver) desktop.setControlling(true)
        return handedOver
    }

    /** Continues the computer's song and queue on this phone with the matching songs in [library]. */
    fun transferToPhone(library: List<Song>) {
        val remote = desktop.controlledState.value
        val matched = remote?.currentSong?.let { matchInLibrary(listOf(it) + remote.upNext, library) }.orEmpty()
        desktop.setControlling(false)
        if (matched.isEmpty() || isFollowingRoomHost()) return
        desktop.sendAction("pause")
        val positionMs = desktop.positionMs.value
        val playing = remote?.isPlaying == true
        viewModelScope.launch {
            repository.playQueue(matched, startIndex = 0, startPositionMs = positionMs, playWhenReady = playing)
        }
    }

    private suspend fun playNextFromDesktop(tracks: List<HandoffTrack>) {
        val library = librariesBySource.values.flatMap { library -> runCatching { library.getSongs() }.getOrDefault(emptyList()) }
        val songs = findAllInLibrary(tracks, library)
        if (songs.isNotEmpty()) addToUpNext(songs)
    }

    private fun runDesktopCommand(command: String) {
        when (command) {
            "play" -> if (!localState.value.isPlaying) repository.togglePlayPause()
            "pause" -> repository.pause()
            "next" -> repository.skipNext()
            "previous" -> repository.skipPrevious()
            "handoff" -> desktop.requestTransfer(toDesktop = true)
            else -> when {
                command.startsWith("seek:") -> command.removePrefix("seek:").toLongOrNull()?.let(repository::seekTo)
                command.startsWith("volume:") -> command.removePrefix("volume:").toIntOrNull()?.let(::setMediaVolumePercent)
            }
        }
    }

    private val audioManager get() = getApplication<Application>().getSystemService(AudioManager::class.java)

    private fun mediaVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
    }

    private fun setMediaVolumePercent(percent: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (percent.coerceIn(0, 100) * max + 50) / 100, 0)
    }

    private suspend fun desktopArtwork(song: Song): String? {
        val cover = loadSongArtwork(getApplication(), song, thumbnail = true) ?: return null
        return withContext(Dispatchers.Default) {
            val bytes = java.io.ByteArrayOutputStream().also { cover.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    // Sends [action] to the computer when it is the device being controlled; returns whether it did.
    private fun sentToDesktop(action: String): Boolean {
        if (controlledDesktop.value == null) return false
        desktop.sendAction(action)
        return true
    }

    fun pause() { if (!sentToDesktop("pause") && !isFollowingRoomHost()) repository.pause() }
    fun togglePlayPause() {
        val action = if (playbackState.value.isPlaying) "pause" else "play"
        if (!sentToDesktop(action) && !isFollowingRoomHost()) repository.togglePlayPause()
    }
    fun skipNext() { if (!sentToDesktop("next") && !isFollowingRoomHost()) repository.skipNext() }
    fun skipPrevious() { if (!sentToDesktop("previous") && !isFollowingRoomHost()) repository.skipPrevious() }
    fun toggleShuffle() { if (!sentToDesktop("toggle_shuffle") && !isFollowingRoomHost()) repository.toggleShuffle() }
    fun cycleRepeatMode() { if (!sentToDesktop("cycle_repeat") && !isFollowingRoomHost()) repository.cycleRepeatMode() }
    fun playFromQueue(song: Song) { if (!desktop.playFromQueue(song) && !isFollowingRoomHost()) repository.playFromQueue(song) }
    fun moveInUpNext(fromIndex: Int, toIndex: Int) {
        when {
            controlledDesktop.value != null -> desktop.moveInQueue(fromIndex, toIndex)
            !isFollowingRoomHost() -> repository.moveInUpNext(fromIndex, toIndex)
        }
    }
    fun addToUpNext(songs: List<Song>) { if (!isFollowingRoomHost()) repository.addToUpNext(songs) }
    fun addToEndOfQueue(songs: List<Song>) { if (!isFollowingRoomHost()) repository.addToEndOfQueue(songs) }
    fun removeFromUpNext(songId: String) {
        val song = playbackState.value.upNext.firstOrNull { it.id == songId }
        if (song != null && desktop.removeFromQueue(song)) return
        if (!isFollowingRoomHost()) repository.removeFromUpNext(songId)
    }
    fun clearHistory() { if (!isFollowingRoomHost()) repository.clearHistory() }
    fun seekTo(positionMs: Long) {
        if (controlledDesktop.value != null) return desktop.seek(positionMs)
        if (isFollowingRoomHost()) return
        repository.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun startListeningRoom() = listeningRoomRepository.startRoom {
        localState.value.currentSong?.let { listOf(it) + localState.value.upNext } ?: emptyList()
    }
    fun findNearbyListeningRooms() = listeningRoomRepository.findNearbyRooms()
    fun stopFindingNearbyListeningRooms() = listeningRoomRepository.stopFindingNearbyRooms()
    fun joinListeningRoom(room: NearbyListeningRoom) = listeningRoomRepository.joinRoom(room)
    fun joinListeningRoomManually(address: String) = listeningRoomRepository.joinRoomManually(address)
    fun leaveListeningRoom() {
        cachedGuestLibrary = null
        listeningRoomRepository.leaveRoom()
    }

    fun toggleFavoriteForCurrentSong() {
        val song = playbackState.value.currentSong?.takeIf { it.source != MusicSource.Desktop } ?: return
        val newValue = !_isCurrentSongFavorite.value
        _isCurrentSongFavorite.value = newValue
        viewModelScope.launch {
            runCatching {
                if (song.source == MusicSource.Radio) {
                    if (newValue) {
                        radioFavoritesRepository.add(song.toRadioStation())
                    } else {
                        radioFavoritesRepository.remove(song.id.removePrefix("radio:"))
                    }
                } else {
                    librariesBySource[song.source]?.setFavorite(song.id, newValue)
                    favoritesRepository.setFavorite(song.id, newValue)
                    if (newValue && autoDownloadFavorites.value && song.source != MusicSource.Local) {
                        `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository.getInstance(getApplication()).download(song)
                    }
                }
            }.onFailure { _isCurrentSongFavorite.value = !newValue }
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            var tick = 0
            var lastTickRealtime = SystemClock.elapsedRealtime()
            while (true) {
                val hasSubscribers = _positionMs.subscriptionCount.value > 0
                val tickDelay = if (hasSubscribers) POSITION_TICK_ACTIVE_MS else POSITION_TICK_IDLE_MS

                _positionMs.value = repository.currentPositionMs()
                applyCrossfade()

                val nowRealtime = SystemClock.elapsedRealtime()
                val deltaMs = (nowRealtime - lastTickRealtime).coerceAtLeast(0L)
                lastTickRealtime = nowRealtime

                val listenable = localState.value.currentSong?.takeIf { it.source != MusicSource.Radio }
                val positionMs = _positionMs.value
                val replayed = currentListen?.let { it.counted && positionMs < REPLAY_START_MS && positionMs < it.positionMs } == true
                if (currentListen?.song?.id != listenable?.id || replayed) {
                    finishListen()
                    currentListen = listenable?.let(::CurrentListen)
                }
                currentListen?.let {
                    it.listenedMs += deltaMs
                    it.positionMs = positionMs
                }
                tick++
                if (tick % 5 == 0 && listeningRoom.value.role == ListeningRoomRole.HOST) {
                    publishRoomSnapshot()
                }
                val saveInterval = if (hasSubscribers) SAVE_EVERY_N_TICKS else 10
                if (tick % saveInterval == 0) {
                    savePlaybackState()
                }
                maybeRecordPlay()
                delay(tickDelay)
            }
        }
    }

    // Minimum listening duration required to count as an intentional play.
    private fun maybeRecordPlay() {
        if (!appPreferences.value.listeningStatsEnabled) return
        val listen = currentListen ?: return
        val durationMs = localState.value.durationMs
        if (countsAsPlay(listen.listenedMs, durationMs)) listen.counted = true
        if (!listen.scrobbled && countsAsScrobble(listen.listenedMs, durationMs)) {
            listen.scrobbled = true
            scrobbleManager.onTrackPlayed(listen.song)
        }
    }

    private fun finishListen() {
        val current = currentListen?.takeIf { it.counted } ?: return
        val song = current.song
        val listen = Listen(
            System.currentTimeMillis(), song.title, song.artist, song.album, song.genres.firstOrNull().orEmpty(), current.listenedMs, song.id
        )
        statsRepository.record(listen)
        desktop.queueListen(listen)
    }

    private fun applyCrossfade() {
        val fadeMs = appPreferences.value.crossfadeSeconds * 1000L
        val dur = localState.value.durationMs
        if (fadeMs <= 0 || dur <= 0 || sleepFading) return
        val pos = _positionMs.value
        val fadeIn = (pos.toFloat() / fadeMs).coerceIn(0f, 1f)
        val fadeOut = ((dur - pos).toFloat() / fadeMs).coerceIn(0f, 1f)
        repository.setCrossfadeFraction(minOf(fadeIn, fadeOut))
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun savePlaybackState() {
        // Radio keeps its own queue, so the saved music queue survives listening to a station.
        if (localState.value.currentSong?.source == MusicSource.Radio) return
        val snapshot = repository.snapshotForSave() ?: return
        viewModelScope.launch { stateRepository.save(snapshot) }
    }

    private fun publishRoomSnapshot() {
        val current = localState.value.currentSong ?: return
        val queue = listOf(current) + localState.value.upNext
        listeningRoomRepository.publish(
            RoomSnapshot(
                tracks = queue.map { it.toRoomTrack() },
                positionMs = repository.currentPositionMs(),
                isPlaying = localState.value.isPlaying
            )
        )
    }

    private var autoplayInFlight = false

    private suspend fun maybeAutoplay() {
        if (autoplayInFlight) return
        if (!appPreferences.value.autoplayEnabled) return
        if (listeningRoom.value.role != ListeningRoomRole.NONE) return
        if (localState.value.upNext.size > 1) return
        autoplayInFlight = true
        try {
            performAutoplay()
        } finally {
            autoplayInFlight = false
        }
    }

    private suspend fun performAutoplay() {
        val seed = localState.value.currentSong
        val queueIds = localState.value.upNext.map { it.id }.toSet()
        val skippedIds = repository.skipTracker.skippedSongIds()
        val exclude = (localState.value.history.map { it.id } + queueIds + skippedIds + listOfNotNull(seed?.id)).toSet()
        val available = librariesBySource.values.flatMap { library ->
            runCatching { library.getSongs() }.getOrDefault(emptyList())
        }.filterNot { it.id in exclude }
        if (available.isEmpty()) return

        val playCounts = HashMap<String, Int>()
        statsRepository.monthlyStats.first().values.forEach { month ->
            month.songPlayCounts.forEach { (songId, count) -> playCounts[songId] = (playCounts[songId] ?: 0) + count }
        }
        val seedArtists = seed?.artist?.splitArtists()?.map { it.lowercase() }?.toSet().orEmpty()
        val seedGenres = seed?.genres?.map { it.lowercase() }?.toSet().orEmpty()
        val seedYear = seed?.releaseYear

        val weighted = available.map { song ->
            var weight = 1.0
            val candidateArtists = song.artist.splitArtists().map { it.lowercase() }.toSet()
            if (seed != null && song.artist.isNotBlank() && song.artist.equals(seed.artist, ignoreCase = true)) {
                weight += AUTOPLAY_SAME_ARTIST_WEIGHT
            } else if (candidateArtists.any { it in seedArtists }) {
                weight += AUTOPLAY_COLLABORATION_WEIGHT
            }
            if (song.genres.any { it.lowercase() in seedGenres }) {
                weight += AUTOPLAY_SHARED_GENRE_WEIGHT
            } else if (song.genres.any { cg -> seedGenres.any { sg -> cg.contains(sg, ignoreCase = true) || sg.contains(cg, ignoreCase = true) } }) {
                weight += AUTOPLAY_RELATED_GENRE_WEIGHT
            }
            if (seedYear != null && song.releaseYear != null && kotlin.math.abs(seedYear - song.releaseYear) <= 5) {
                weight += AUTOPLAY_ERA_PROXIMITY_WEIGHT
            }
            if (song.isFavorite) weight += AUTOPLAY_FAVORITE_WEIGHT
            weight += ln((playCounts[song.id] ?: 0) + 1.0)
            song to weight
        }

        val picks = weightedSampleWithoutReplacement(weighted, AUTOPLAY_BATCH_SIZE)
        if (picks.isNotEmpty()) {
            val state = localState.value
            val isActivelyPlayingOrPreparing = state.currentSong != null && (state.isPlaying || state.playWhenReady || state.isBuffering)
            if (isActivelyPlayingOrPreparing) {
                repository.addToEndOfQueue(picks)
            } else {
                repository.continueWithAutoplay(picks)
            }
        }
    }

    private fun <T> weightedSampleWithoutReplacement(items: List<Pair<T, Double>>, count: Int): List<T> =
        items.map { (item, weight) -> item to Random.nextDouble().pow(1.0 / weight) }
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first }

    private var cachedGuestLibrary: List<Song>? = null

    private suspend fun applyRoomSnapshot(snapshot: RoomSnapshot) {
        val receivedAtElapsedMs = SystemClock.elapsedRealtime()
        val available = cachedGuestLibrary ?: librariesBySource.values.flatMap { library ->
            runCatching { library.getSongs() }.getOrDefault(emptyList())
        }.also { cachedGuestLibrary = it }
        val hostIp = listeningRoomRepository.guestHostAddress
        val resolved = snapshot.tracks.mapNotNull { track ->
            available.firstOrNull { it.matchesRoomTrack(track) }
                ?: snapshot.audioPort?.let { port ->
                    snapshot.roomToken?.let { token -> hostIp?.let { ip -> track.toRelaySong(ip, port, token) } }
                }
        }
        val positionMs = if (snapshot.isPlaying) {
            snapshot.positionMs + (SystemClock.elapsedRealtime() - receivedAtElapsedMs)
        } else {
            snapshot.positionMs
        }
        if (resolved.isNotEmpty()) repository.applyRoomQueue(resolved, positionMs, snapshot.isPlaying)
    }

    private fun isFollowingRoomHost(): Boolean = listeningRoom.value.role == ListeningRoomRole.GUEST

    fun updateSongMetadata(updatedSong: Song) = repository.updateSongMetadata(updatedSong)

    fun applyManualLyrics(song: Song, synced: List<LyricLine>?, plain: String?, provider: String = "LRCLIB") {
        if (playbackState.value.currentSong?.id == song.id) {
            _syncedLyrics.value = synced
            _fallbackLyrics.value = plain
            _lyricsProvider.value = provider
        }
        viewModelScope.launch(Dispatchers.IO) {
            val lrcString = synced?.joinToString("\n") { line ->
                val min = line.timestampMs / 60_000
                val sec = (line.timestampMs % 60_000) / 1000
                val ms = (line.timestampMs % 1000) / 10
                "[%02d:%02d.%02d]%s".format(java.util.Locale.US, min, sec, ms, line.text)
            }
            lrcLibClient.saveLyricsToCache(song.artist, song.title, song.album, lrcString, plain)
        }
    }

    override fun onCleared() {
        stopTicker()
        finishListen()
        listeningRoomRepository.release()
        repository.release()
    }
}

private fun Song.toRoomTrack() = RoomTrack(title, artist, album, durationMs)

private fun Song.matchesRoomTrack(track: RoomTrack): Boolean =
    roomKey(title) == roomKey(track.title) &&
        roomKey(artist) == roomKey(track.artist) &&
        roomKey(album) == roomKey(track.album) &&
        abs(durationMs - track.durationMs) <= 2_000L

private fun roomKey(value: String): String = value.trim().lowercase()

private fun RoomTrack.toRelaySong(hostIp: String, audioPort: Int, roomToken: String): Song {
    val id = "listeningroom:${roomKey(title)}_${roomKey(artist)}_$durationMs"
    val uri = "http://$hostIp:$audioPort/stream".toUri().buildUpon()
        .appendQueryParameter("title", title)
        .appendQueryParameter("artist", artist)
        .appendQueryParameter("duration", durationMs.toString())
        .appendQueryParameter("token", roomToken)
        .build()
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = id,
        durationMs = durationMs,
        contentUri = uri,
        source = MusicSource.ListeningRoomHost
    )
}
