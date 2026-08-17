package `in`.caffeinelabs.cassettecat.ui.playback

import android.app.Application
import android.os.SystemClock
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

private data class ListeningBucket(val monthKey: String, val songId: String)
private data class LyricsRequest(val song: Song?, val embeddedLyrics: String?, val lrcLibEnabled: Boolean)

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

    private val lrcLibClient = LrcLibClient()
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

    private val stateRepository = PlaybackStateRepository(app)
    private val statsRepository = ListeningStatsRepository(app)
    private val equalizerSettingsRepository = EqualizerSettingsRepository(app)
    private var hasAttemptedRestore = false
    private var playRecordedForSongId: String? = null
    private val accumulatedListeningMs = mutableMapOf<ListeningBucket, Long>()

    // Media3 doesn't push continuous position updates, so this polls while playing
    // instead of the repository pushing it, keeping the repository a pure reactive wrapper.
    private var tickerJob: Job? = null

    init {
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
                    if (!embedded.isNullOrBlank()) {
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
                                lrcLibClient.fetchLyrics(song.artist, song.title, song.album)?.let { result ->
                                    _syncedLyrics.value = result.syncedLyrics
                                    _fallbackLyrics.value = result.plainLyrics
                                    _lyricsProvider.value = "LRCLIB"
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
                levels.bandLevelsMb.forEachIndexed { band, levelMb -> EqualizerController.setBandLevel(band, levelMb) }
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (isFollowingRoomHost()) return
        viewModelScope.launch { repository.playQueue(songs, startIndex) }
    }

    // once per process, and only into an idle session
    fun restoreIfNeeded(allSongs: List<Song>) {
        if (hasAttemptedRestore) return
        hasAttemptedRestore = true
        if (playbackState.value.currentSong != null) return
        viewModelScope.launch {
            val saved = stateRepository.load() ?: return@launch
            val songsById = allSongs.associateBy { it.id }
            val resolvedSongs = saved.queueSongIds.mapNotNull { songsById[it] }
            if (resolvedSongs.isEmpty()) return@launch
            // adjust for ids that no longer resolve (deleted files) shifting positions
            val adjustedIndex = saved.queueSongIds.take(saved.currentIndex).count { it in songsById }
            repository.restoreQueue(resolvedSongs, adjustedIndex.coerceIn(0, resolvedSongs.size - 1), saved.positionMs)
        }
    }
    private var sleepTimerJob: Job? = null
    private val _sleepTimerEndMs = MutableStateFlow<Long?>(null)
    val sleepTimerEndMs: StateFlow<Long?> = _sleepTimerEndMs.asStateFlow()

    fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        _sleepTimerEndMs.value = SystemClock.elapsedRealtime() + durationMs
        sleepTimerJob = viewModelScope.launch {
            delay(durationMs)
            if (playbackState.value.isPlaying) togglePlayPause()
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

    fun startListeningRoom() = listeningRoomRepository.startRoom()
    fun findNearbyListeningRooms() = listeningRoomRepository.findNearbyRooms()
    fun joinListeningRoom(room: NearbyListeningRoom) = listeningRoomRepository.joinRoom(room)
    fun leaveListeningRoom() = listeningRoomRepository.leaveRoom()

    // Optimistic: flips immediately, reverts on failure. Doesn't retroactively update
    // the Library list row, that corrects itself on the next refresh.
    fun toggleFavoriteForCurrentSong() {
        val song = playbackState.value.currentSong ?: return
        val newValue = !_isCurrentSongFavorite.value
        _isCurrentSongFavorite.value = newValue
        viewModelScope.launch {
            runCatching {
                librariesBySource[song.source]?.setFavorite(song.id, newValue)
            }.onFailure { _isCurrentSongFavorite.value = !newValue }
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            var tick = 0
            while (true) {
                _positionMs.value = repository.currentPositionMs()
                playbackState.value.currentSong?.let { song ->
                    val bucket = ListeningBucket(YearMonth.now().toString(), song.id)
                    accumulatedListeningMs[bucket] = (accumulatedListeningMs[bucket] ?: 0L) + POSITION_TICK_MS
                }
                tick++
                if (tick % 4 == 0 && listeningRoom.value.role == ListeningRoomRole.HOST) {
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
        }
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

    private suspend fun applyRoomSnapshot(snapshot: RoomSnapshot) {
        val available = librariesBySource.values.flatMap { library ->
            runCatching { library.getSongs() }.getOrDefault(emptyList())
        }
        val resolved = snapshot.tracks.mapNotNull { track ->
            available.firstOrNull { it.matchesRoomTrack(track) }
        }
        if (resolved.isNotEmpty()) repository.applyRoomQueue(resolved, snapshot.positionMs, snapshot.isPlaying)
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
