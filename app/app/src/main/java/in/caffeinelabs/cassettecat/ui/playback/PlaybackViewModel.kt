@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.ui.playback

import android.app.Application
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import `in`.caffeinelabs.cassettecat.data.library.LibraryRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.local.LocalLibraryRepository
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
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.stats.ListeningStatsRepository
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.data.streaming.jellyfin.JellyfinLibraryRepository
import `in`.caffeinelabs.cassettecat.data.streaming.subsonic.SubsonicLibraryRepository
import java.time.YearMonth
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
import kotlin.math.abs

private const val POSITION_TICK_MS = 500L
private const val SAVE_EVERY_N_TICKS = 20 // ~10s at POSITION_TICK_MS
private const val PLAY_COUNT_MAX_THRESHOLD_MS = 4 * 60 * 1000L
private const val AUTOPLAY_BATCH_SIZE = 20

private data class ListeningBucket(val monthKey: String, val songId: String)
private data class LyricsRequest(val song: Song?, val embeddedLyrics: String?, val lrcLibEnabled: Boolean)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = PlaybackRepository(app)
    private val listeningRoomRepository = LocalListeningRoomRepository(app)

    val playbackState: StateFlow<PlaybackUiState> = repository.state
    val listeningRoom: StateFlow<ListeningRoomState> = listeningRoomRepository.state

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    // Plain construction, no shared DI container, matching LibraryViewModel's own
    // pattern: this only exists to dispatch setFavorite() by source, not to fetch.
    private val streamingServerRepository = StreamingServerRepository(app)
    private val credentialStore = CredentialStore(app)
    private val librariesBySource: Map<MusicSource, LibraryRepository> = mapOf(
        MusicSource.Local to LocalLibraryRepository(app),
        MusicSource.Subsonic to SubsonicLibraryRepository(streamingServerRepository, credentialStore),
        MusicSource.Jellyfin to JellyfinLibraryRepository(streamingServerRepository, credentialStore)
    )

    private val _isCurrentSongFavorite = MutableStateFlow(false)
    val isCurrentSongFavorite: StateFlow<Boolean> = _isCurrentSongFavorite.asStateFlow()

    private val lrcLibClient = LrcLibClient(app.cacheDir)
    private val localLrcLoader = LocalLrcLoader(app)
    private val embeddedLyricsLoader = EmbeddedLyricsLoader(app)
    private val serviceSettingsRepository = ServiceSettingsRepository(app)
    private val appPreferencesRepository = AppPreferencesRepository(app)
    private val appPreferences = appPreferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), `in`.caffeinelabs.cassettecat.data.settings.AppPreferences())
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
    private var playRecordedForSongId: String? = null
    private val accumulatedListeningMs = mutableMapOf<ListeningBucket, Long>()

    // Media3 doesn't push continuous position updates, so this polls while playing
    // instead of the repository pushing it, keeping the repository a pure reactive wrapper.
    private var tickerJob: Job? = null

    init {
        repository.onQueueExhausted = { viewModelScope.launch { maybeAutoplay() } }
        viewModelScope.launch {
            repository.connect()
        }
        viewModelScope.launch {
            playbackState.collect { state ->
                if (state.isPlaying) startTicker() else { stopTicker(); savePlaybackState() }
                if (listeningRoom.value.role == ListeningRoomRole.HOST) publishRoomSnapshot()
            }
        }
        viewModelScope.launch {
            listeningRoomRepository.snapshots.collect { snapshot ->
                if (listeningRoom.value.role == ListeningRoomRole.GUEST) applyRoomSnapshot(snapshot)
            }
        }
        // Ticker only runs while playing, so reset position on song change or a paused
        // track-change would leave the readout stuck on the previous track's position.
        viewModelScope.launch {
            playbackState.map { it.currentSong?.id }.distinctUntilChanged().collect {
                _positionMs.value = repository.currentPositionMs()
            }
        }
        viewModelScope.launch {
            playbackState.map { it.currentSong }.distinctUntilChanged().collect { song ->
                _isCurrentSongFavorite.value = song?.isFavorite ?: false
                if (song != null) {
                    scrobbleManager.onTrackStarted(song)
                    savePlaybackState()
                }
            }
        }
        // only when there's no embedded lyrics for the current song
        viewModelScope.launch {
            playbackState.map { it.currentSong to it.currentLyrics }
                .combine(serviceSettingsRepository.settings) { (song, embedded), settings ->
                    LyricsRequest(song, embedded, settings.lrcLibEnabled)
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
            playbackState.map { it.audioSessionId }.distinctUntilChanged().collect { sessionId ->
                if (sessionId == C.AUDIO_SESSION_ID_UNSET) return@collect
                EqualizerController.attach(sessionId)
                val levels = equalizerSettingsRepository.levels.first()
                EqualizerController.setMasterEnabled(levels.enabled)
                levels.bandLevelsMb.forEachIndexed { band, levelMb -> EqualizerController.setBandLevel(band, levelMb) }
                EqualizerController.setBassBoostStrength(levels.bassBoostStrength)
                EqualizerController.setVirtualizerStrength(levels.virtualizerStrength)
                EqualizerController.setPreampGainMb(levels.preampGainMb)
                EqualizerController.setLoudnessNormalization(levels.loudnessNormalization, levels.preampGainMb)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (isFollowingRoomHost()) return
        viewModelScope.launch { repository.playQueue(songs, startIndex) }
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
            if (playbackState.value.currentSong != null) return@launch
            val resolvedSongs = saved.queueSongIds.mapNotNull { songsById[it] }
            if (resolvedSongs.isEmpty()) return@launch
            // adjust for ids that no longer resolve (deleted files) shifting positions
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

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        repository.setPlaybackSpeed(speed)
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
            (playbackState.value.durationMs - _positionMs.value).coerceAtLeast(1_000L)
        } else {
            durationMs
        }
        _sleepTimerEndMs.value = SystemClock.elapsedRealtime() + effectiveDurationMs
        sleepTimerJob = viewModelScope.launch {
            val fadeMs = if (fadeOut) (fadeSeconds * 1000L).coerceAtMost(effectiveDurationMs / 2) else 0L
            val preFadeMs = (effectiveDurationMs - fadeMs).coerceAtLeast(0L)
            if (preFadeMs > 0) {
                delay(preFadeMs)
            }
            if (fadeOut && fadeMs > 0) {
                sleepFading = true
                repository.setVolumeOverrideActive(true)
                val initialVol = repository.getVolume()
                val steps = 20
                val stepDelay = fadeMs / steps
                for (i in 1..steps) {
                    delay(stepDelay)
                    val factor = 1f - (i.toFloat() / steps.toFloat())
                    repository.setVolume(initialVol * factor)
                }
                if (playbackState.value.isPlaying) togglePlayPause()
                repository.setVolume(initialVol)
                repository.setVolumeOverrideActive(false)
                sleepFading = false
            } else {
                if (playbackState.value.isPlaying) togglePlayPause()
            }
            _sleepTimerEndMs.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerEndMs.value = null
    }

    fun togglePlayPause() { if (!isFollowingRoomHost()) repository.togglePlayPause() }
    fun skipNext() { if (!isFollowingRoomHost()) repository.skipNext() }
    fun skipPrevious() { if (!isFollowingRoomHost()) repository.skipPrevious() }
    fun toggleShuffle() { if (!isFollowingRoomHost()) repository.toggleShuffle() }
    fun cycleRepeatMode() { if (!isFollowingRoomHost()) repository.cycleRepeatMode() }
    fun playFromQueue(song: Song) { if (!isFollowingRoomHost()) repository.playFromQueue(song) }
    fun moveInUpNext(fromIndex: Int, toIndex: Int) { if (!isFollowingRoomHost()) repository.moveInUpNext(fromIndex, toIndex) }
    fun addToUpNext(songs: List<Song>) { if (!isFollowingRoomHost()) repository.addToUpNext(songs) }
    fun removeFromUpNext(songId: String) { if (!isFollowingRoomHost()) repository.removeFromUpNext(songId) }
    fun clearHistory() { if (!isFollowingRoomHost()) repository.clearHistory() }
    fun seekTo(positionMs: Long) {
        if (isFollowingRoomHost()) return
        repository.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun startListeningRoom() = listeningRoomRepository.startRoom {
        playbackState.value.currentSong?.let { listOf(it) + playbackState.value.upNext } ?: emptyList()
    }
    fun findNearbyListeningRooms() = listeningRoomRepository.findNearbyRooms()
    fun joinListeningRoom(room: NearbyListeningRoom) = listeningRoomRepository.joinRoom(room)
    fun joinListeningRoomManually(address: String) = listeningRoomRepository.joinRoomManually(address)
    fun leaveListeningRoom() {
        cachedGuestLibrary = null
        listeningRoomRepository.leaveRoom()
    }

    fun toggleFavoriteForCurrentSong() {
        val song = playbackState.value.currentSong ?: return
        val newValue = !_isCurrentSongFavorite.value
        _isCurrentSongFavorite.value = newValue
        viewModelScope.launch {
            runCatching {
                librariesBySource[song.source]?.setFavorite(song.id, newValue)
                if (newValue && appPreferences.value.autoCacheFavorites && song.source != MusicSource.Local) {
                    `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository.getInstance(getApplication()).download(song)
                }
            }.onFailure { _isCurrentSongFavorite.value = !newValue }
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            var tick = 0
            while (true) {
                _positionMs.value = repository.currentPositionMs()
                applyCrossfade()
                playbackState.value.currentSong?.let { song ->
                    if (song.source != MusicSource.Radio) {
                        val bucket = ListeningBucket(YearMonth.now().toString(), song.id)
                        accumulatedListeningMs[bucket] = (accumulatedListeningMs[bucket] ?: 0L) + POSITION_TICK_MS
                    }
                }
                tick++
                if (tick % 2 == 0 && listeningRoom.value.role == ListeningRoomRole.HOST) {
                    publishRoomSnapshot()
                }
                if (tick % SAVE_EVERY_N_TICKS == 0) {
                    savePlaybackState()
                    flushListeningTime()
                }
                maybeRecordPlay()
                delay(POSITION_TICK_MS)
            }
        }
    }

    private fun flushListeningTime() {
        if (!appPreferences.value.listeningStatsEnabled) {
            accumulatedListeningMs.clear()
            return
        }
        if (accumulatedListeningMs.isEmpty()) return
        val pending = accumulatedListeningMs.toMap()
        accumulatedListeningMs.clear()
        viewModelScope.launch {
            pending.forEach { (bucket, ms) ->
                statsRepository.addListeningTime(bucket.songId, bucket.monthKey, ms)
            }
        }
    }

    // counts a play past 50% or 4 minutes, whichever is sooner (avoids counting skips)
    private fun maybeRecordPlay() {
        if (!appPreferences.value.listeningStatsEnabled) return
        val state = playbackState.value
        val song = state.currentSong ?: return
        if (playRecordedForSongId == song.id) return
        val threshold = minOf(state.durationMs / 2, PLAY_COUNT_MAX_THRESHOLD_MS)
        if (threshold > 0 && _positionMs.value >= threshold) {
            playRecordedForSongId = song.id
            viewModelScope.launch { statsRepository.recordPlay(song.id, YearMonth.now().toString()) }
            scrobbleManager.onTrackPlayed(song)
        }
    }

    private fun applyCrossfade() {
        val fadeMs = appPreferences.value.crossfadeSeconds * 1000L
        val dur = playbackState.value.durationMs
        if (fadeMs <= 0 || dur <= 0 || sleepFading) return
        val pos = _positionMs.value
        val fadeIn = (pos.toFloat() / fadeMs).coerceIn(0f, 1f)
        val fadeOut = ((dur - pos).toFloat() / fadeMs).coerceIn(0f, 1f)
        repository.setCrossfadeFraction(minOf(fadeIn, fadeOut))
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        flushListeningTime()
    }

    private fun savePlaybackState() {
        val snapshot = repository.snapshotForSave() ?: return
        viewModelScope.launch { stateRepository.save(snapshot) }
    }

    private fun publishRoomSnapshot() {
        val current = playbackState.value.currentSong ?: return
        val queue = listOf(current) + playbackState.value.upNext
        listeningRoomRepository.publish(
            RoomSnapshot(
                tracks = queue.map { it.toRoomTrack() },
                positionMs = repository.currentPositionMs(),
                isPlaying = playbackState.value.isPlaying
            )
        )
    }

    private suspend fun maybeAutoplay() {
        if (!appPreferences.value.autoplayEnabled) return
        if (listeningRoom.value.role != ListeningRoomRole.NONE) return
        val exclude = (playbackState.value.history.map { it.id } + listOfNotNull(playbackState.value.currentSong?.id)).toSet()
        val available = librariesBySource.values.flatMap { library ->
            runCatching { library.getSongs() }.getOrDefault(emptyList())
        }
        val picks = available.filterNot { it.id in exclude }.shuffled().take(AUTOPLAY_BATCH_SIZE)
        if (picks.isNotEmpty()) repository.continueWithAutoplay(picks)
    }

    private var cachedGuestLibrary: List<Song>? = null

    private suspend fun applyRoomSnapshot(snapshot: RoomSnapshot) {
        val receivedAtElapsedMs = SystemClock.elapsedRealtime()
        val available = cachedGuestLibrary ?: librariesBySource.values.flatMap { library ->
            runCatching { library.getSongs() }.getOrDefault(emptyList())
        }.also { cachedGuestLibrary = it }
        val hostIp = listeningRoomRepository.guestHostAddress
        val resolved = snapshot.tracks.mapNotNull { track ->
            available.firstOrNull { it.matchesRoomTrack(track) }
                ?: snapshot.audioPort?.let { port -> hostIp?.let { ip -> track.toRelaySong(ip, port) } }
        }
        val positionMs = if (snapshot.isPlaying) {
            snapshot.positionMs + (SystemClock.elapsedRealtime() - receivedAtElapsedMs)
        } else {
            snapshot.positionMs
        }
        if (resolved.isNotEmpty()) repository.applyRoomQueue(resolved, positionMs, snapshot.isPlaying)
    }

    private fun isFollowingRoomHost(): Boolean = listeningRoom.value.role == ListeningRoomRole.GUEST

    override fun onCleared() {
        stopTicker()
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

private fun RoomTrack.toRelaySong(hostIp: String, audioPort: Int): Song {
    val id = "listeningroom:${roomKey(title)}_${roomKey(artist)}_$durationMs"
    val uri = "http://$hostIp:$audioPort/stream".toUri().buildUpon()
        .appendQueryParameter("title", title)
        .appendQueryParameter("artist", artist)
        .appendQueryParameter("duration", durationMs.toString())
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
