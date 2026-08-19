package `in`.caffeinelabs.cassettecat.data.playback

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import `in`.caffeinelabs.cassettecat.data.library.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val HISTORY_LIMIT = 50

class PlaybackRepository(private val context: Context) {
    private val appPreferencesRepository = `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository(context)
    private var controller: MediaController? = null
    private var replayGainVolume = 1f
    private var currentQueue: List<Song> = emptyList()
    // Snapshot of the order playQueue() was called with, untouched by shuffle
    // or manual drag-reorder: what toggling shuffle off restores.
    private var originalQueue: List<Song> = emptyList()
    private var shuffleEnabled = false
    private var suppressShuffleSync = false
    private val history = ArrayDeque<Song>()
    var onQueueExhausted: (() -> Unit)? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    suspend fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controller = MediaController.Builder(context, token).buildAsync().awaitController(context)
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateState()
                _state.value.currentSong?.let { recordPlayed(it) }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val c = controller
                    val currentIndex = c?.currentMediaItemIndex ?: C.INDEX_UNSET
                    when {
                        c != null && currentIndex != C.INDEX_UNSET && currentIndex < currentQueue.size - 1 -> {
                            c.seekTo(currentIndex + 1, 0L)
                            c.play()
                        }
                        c != null && c.repeatMode == Player.REPEAT_MODE_ALL && currentQueue.isNotEmpty() -> {
                            c.seekTo(0, 0L)
                            c.play()
                        }
                        c != null && c.hasNextMediaItem() -> {
                            c.seekToNextMediaItem()
                            c.play()
                        }
                        else -> onQueueExhausted?.invoke()
                    }
                }
                updateState()
            }
            override fun onRepeatModeChanged(repeatMode: Int) = updateState()
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (suppressShuffleSync) return
                if (shuffleModeEnabled != shuffleEnabled) toggleShuffle()
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) = updateState()
            // Lyrics become known only once container tags parse during load, not at transition time.
            override fun onTracksChanged(tracks: Tracks) {
                val prefs = runBlocking { appPreferencesRepository.preferences.first() }
                replayGainVolume = extractReplayGain(tracks).volumeMultiplier(prefs.replayGainEnabled, prefs.replayGainPreAmpDb)
                controller?.volume = replayGainVolume
                updateState()
            }
            // Safety net: toggleShuffle()/moveInUpNext() already updateState() synchronously,
            // this just re-syncs if the timeline changes some other way.
            override fun onTimelineChanged(timeline: Timeline, reason: Int) = updateState()
        })
        updateState()
    }

    // Offloads MediaItem mapping off the caller's thread (was a visible stall for large
    // libraries); controller calls stay on Main, which MediaController requires.
    suspend fun playQueue(songs: List<Song>, startIndex: Int) {
        currentQueue = songs
        originalQueue = songs
        shuffleEnabled = false
        songs.getOrNull(startIndex)?.let { recordPlayed(it) }
        val mediaItems = withContext(Dispatchers.Default) { songs.map { it.toMediaItem() } }
        controller?.apply {
            setMediaItems(mediaItems, startIndex, 0L)
            prepare()
            play()
        }
    }

    // no play(): loads and seeks without auto-starting audio; no-ops if something's already loaded
    suspend fun restoreQueue(songs: List<Song>, startIndex: Int, positionMs: Long) {
        if (songs.isEmpty() || controller?.currentMediaItem != null) return
        currentQueue = songs
        originalQueue = songs
        val mediaItems = withContext(Dispatchers.Default) { songs.map { it.toMediaItem() } }
        controller?.apply {
            setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.size - 1), positionMs)
            prepare()
        }
    }

    /** Applies a host snapshot without exposing networking concerns to the player UI. */
    suspend fun applyRoomQueue(songs: List<Song>, positionMs: Long, isPlaying: Boolean) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val sameQueue = currentQueue.map { it.id } == songs.map { it.id }
        if (!sameQueue) {
            currentQueue = songs
            originalQueue = songs
            shuffleEnabled = false
            val mediaItems = withContext(Dispatchers.Default) { songs.map { it.toMediaItem() } }
            val prepareStartedAtMs = SystemClock.elapsedRealtime()
            c.setMediaItems(mediaItems, 0, positionMs.coerceAtLeast(0L))
            c.prepare()
            if (isPlaying) correctPositionOnceReady(c, positionMs, prepareStartedAtMs)
        } else if (kotlin.math.abs(c.currentPosition - positionMs) > 400L) {
            c.seekTo(positionMs.coerceAtLeast(0L))
        }
        if (isPlaying) c.play() else c.pause()
        updateState()
    }

    private fun correctPositionOnceReady(c: MediaController, basePositionMs: Long, startedAtMs: Long) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        c.removeListener(this)
                        val bufferedForMs = SystemClock.elapsedRealtime() - startedAtMs
                        if (bufferedForMs > 150L) c.seekTo(basePositionMs + bufferedForMs)
                    }
                    Player.STATE_IDLE -> c.removeListener(this)
                }
            }
        }
        c.addListener(listener)
    }

    fun snapshotForSave(): SavedPlaybackState? {
        val c = controller
        val currentIndex = c?.currentMediaItemIndex ?: C.INDEX_UNSET
        val pos = c?.currentPosition ?: 0L
        if (currentQueue.isEmpty() && history.isEmpty()) return null
        return SavedPlaybackState(
            queueSongIds = currentQueue.map { it.id },
            currentIndex = if (currentIndex != C.INDEX_UNSET) currentIndex else 0,
            positionMs = pos,
            historySongIds = history.map { it.id }
        )
    }

    fun togglePlayPause() {
        controller?.let { c ->
            if (c.isPlaying) {
                c.pause()
            } else {
                if (c.playbackState == Player.STATE_IDLE) {
                    c.prepare()
                } else if (c.playbackState == Player.STATE_ENDED) {
                    val currentIndex = c.currentMediaItemIndex
                    if (currentIndex != C.INDEX_UNSET && currentIndex < currentQueue.size - 1) {
                        c.seekTo(currentIndex + 1, 0L)
                    } else {
                        c.seekTo(0, 0L)
                    }
                }
                c.play()
            }
        }
    }

    fun skipNext() {
        val c = controller ?: return
        val currentIndex = c.currentMediaItemIndex
        if (currentIndex != C.INDEX_UNSET && currentIndex < currentQueue.size - 1) {
            c.seekTo(currentIndex + 1, 0L)
            c.play()
        } else if (c.repeatMode == Player.REPEAT_MODE_ALL && currentQueue.isNotEmpty()) {
            c.seekTo(0, 0L)
            c.play()
        } else if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
            c.play()
        }
    }

    fun skipPrevious() {
        val c = controller ?: return
        if (c.currentPosition > 3000L) {
            c.seekTo(0L)
        } else {
            val currentIndex = c.currentMediaItemIndex
            if (currentIndex != C.INDEX_UNSET && currentIndex > 0) {
                c.seekTo(currentIndex - 1, 0L)
                c.play()
            } else if (c.hasPreviousMediaItem()) {
                c.seekToPreviousMediaItem()
                c.play()
            } else {
                c.seekTo(0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume.coerceIn(0f, 1f)
    }

    fun getVolume(): Float {
        return controller?.volume ?: 1f
    }

    // Single-player crossfade approximation: fades in/out around track boundaries
    // rather than true overlapping playback. fraction is 1f mid-track, 0f at the edges.
    fun setCrossfadeFraction(fraction: Float) {
        controller?.volume = replayGainVolume * fraction.coerceIn(0f, 1f)
    }

    // Real toggle, not ExoPlayer's shuffleModeEnabled (fixed order reused across
    // re-enabling, and MediaController can't reach setShuffleOrder() to force a fresh one):
    // on shuffles the upcoming queue for real, off restores playQueue()'s original order.
    fun toggleShuffle() {
        val c = controller ?: return
        shuffleEnabled = !shuffleEnabled
        if (shuffleEnabled) shuffleUpNext(c) else restoreOriginalOrder(c)
        updateState()
    }

    private fun shuffleUpNext(c: MediaController) {
        val currentIndex = c.currentMediaItemIndex
        val queueSizeBefore = currentQueue.size
        if (currentIndex == C.INDEX_UNSET || currentIndex >= queueSizeBefore - 1) return
        val shuffledUpcoming = currentQueue.subList(currentIndex + 1, queueSizeBefore).shuffled()
        currentQueue = currentQueue.subList(0, currentIndex + 1) + shuffledUpcoming
        c.replaceMediaItems(currentIndex + 1, queueSizeBefore, shuffledUpcoming.map { it.toMediaItem() })
    }

    private fun restoreOriginalOrder(c: MediaController) {
        val currentIndex = c.currentMediaItemIndex
        val queueSizeBefore = currentQueue.size
        if (currentIndex == C.INDEX_UNSET || currentIndex >= queueSizeBefore - 1) return
        // Keep only songs still upcoming, in their original relative order.
        val remainingIds = currentQueue.subList(currentIndex + 1, queueSizeBefore).map { it.id }.toSet()
        val restoredUpcoming = originalQueue.filter { it.id in remainingIds }
        currentQueue = currentQueue.subList(0, currentIndex + 1) + restoredUpcoming
        c.replaceMediaItems(currentIndex + 1, queueSizeBefore, restoredUpcoming.map { it.toMediaItem() })
    }

    // Translates upNext-list positions (what QueueList displays/drags) to absolute queue
    // indices, so the UI never needs to know where "current" sits in the full queue.
    fun moveInUpNext(fromIndexInUpNext: Int, toIndexInUpNext: Int) {
        val c = controller ?: return
        val currentIndex = c.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET) return
        val from = currentIndex + 1 + fromIndexInUpNext
        val to = currentIndex + 1 + toIndexInUpNext
        if (from !in currentQueue.indices || to !in currentQueue.indices || from == to) return
        c.moveMediaItem(from, to)
        currentQueue = currentQueue.toMutableList().apply { add(to, removeAt(from)) }
        updateState()
    }

    /** Inserts tracks immediately after the current item without interrupting playback. */
    fun addToUpNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val currentIndex = c.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET || currentQueue.isEmpty()) return

        val insertAt = (currentIndex + 1).coerceAtMost(currentQueue.size)
        c.addMediaItems(insertAt, songs.map { it.toMediaItem() })
        currentQueue = currentQueue.toMutableList().apply { addAll(insertAt, songs) }
        // The explicit queue is now the source of truth. This also means turning shuffle
        // off preserves newly queued songs instead of silently dropping them.
        originalQueue = currentQueue
        updateState()
    }

    fun continueWithAutoplay(songs: List<Song>) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val currentIndex = c.currentMediaItemIndex
        val insertAt = if (currentIndex == C.INDEX_UNSET) currentQueue.size else (currentIndex + 1).coerceAtMost(currentQueue.size)
        c.addMediaItems(insertAt, songs.map { it.toMediaItem() })
        currentQueue = currentQueue.toMutableList().apply { addAll(insertAt, songs) }
        originalQueue = currentQueue
        if (c.hasNextMediaItem()) c.seekToNextMediaItem()
        c.play()
        updateState()
    }

    /** Removes one upcoming item. The currently playing song can never be removed here. */
    fun removeFromUpNext(songId: String) {
        val c = controller ?: return
        val currentIndex = c.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET) return
        val absoluteIndex = currentQueue
            .drop(currentIndex + 1)
            .indexOfFirst { it.id == songId }
            .takeIf { it >= 0 }
            ?.plus(currentIndex + 1)
            ?: return
        c.removeMediaItem(absoluteIndex)
        currentQueue = currentQueue.toMutableList().apply { removeAt(absoluteIndex) }
        originalQueue = currentQueue
        updateState()
    }

    fun cycleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun currentPositionMs(): Long = controller?.currentPosition ?: 0L

    // Seeks straight to the song's timeline index by id (source-prefixed, unique);
    // shuffle only changes traversal order, not each item's index.
    fun playFromQueue(song: Song) {
        val index = currentQueue.indexOfFirst { it.id == song.id }
        if (index != -1) controller?.seekTo(index, 0L)
    }

    fun recordPlayed(song: Song) {
        val remaining = history.filterNot { it.id == song.id }
        history.clear()
        history.addFirst(song)
        history.addAll(remaining.take(HISTORY_LIMIT - 1))
        updateState()
    }

    fun restoreHistory(songs: List<Song>) {
        history.clear()
        history.addAll(songs)
        updateState()
    }

    fun clearHistory() {
        history.clear()
        updateState()
    }

    fun release() {
        controller?.release()
        controller = null
    }

    // currentQueue stays in lockstep with the real timeline via every reorder function
    // (playQueue, toggleShuffle, moveInUpNext), so a plain slice is correct here.
    private fun updateState() {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        _state.value = PlaybackUiState(
            currentSong = currentQueue.getOrNull(index),
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            durationMs = c.duration.coerceAtLeast(0L),
            isShuffleEnabled = shuffleEnabled,
            repeatMode = c.repeatMode,
            audioSessionId = c.audioSessionId,
            upNext = currentQueue.drop(index + 1),
            previousInQueue = currentQueue.getOrNull(index - 1),
            history = history.toList(),
            currentLyrics = extractLyrics(c.currentTracks)
        )
    }
}

// No lyrics API on Player/MediaMetadata; reuses Media3's own ID3 USLT (MP3) / Vorbis
// LYRICS (FLAC/OGG) parsing instead of adding a tagging dependency.
private fun extractLyrics(tracks: Tracks): String? {
    for (group in tracks.groups) {
        for (i in 0 until group.length) {
            val metadata = group.getTrackFormat(i).metadata ?: continue
            for (j in 0 until metadata.length()) {
                when (val entry = metadata.get(j)) {
                    is BinaryFrame -> if (entry.id == "USLT") parseUsltFrame(entry.data)?.let { return it }
                    is VorbisComment -> if (entry.key == "LYRICS" || entry.key == "UNSYNCEDLYRICS") {
                        return entry.value.trim().ifEmpty { null }
                    }
                }
            }
        }
    }
    return null
}

// ID3v2 USLT payload: 1-byte encoding, 3-byte language code, null-terminated
// descriptor (ignored), then lyrics text to the end of the frame.
internal fun parseUsltFrame(data: ByteArray): String? {
    if (data.size < 4) return null
    val charset = when (data[0].toInt()) {
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> Charsets.ISO_8859_1
    }
    val terminatorWidth = if (data[0].toInt() == 1 || data[0].toInt() == 2) 2 else 1
    var textStart = data.size
    var i = 4
    while (i + terminatorWidth <= data.size) {
        val atTerminator = data[i] == 0.toByte() && (terminatorWidth == 1 || data[i + 1] == 0.toByte())
        if (atTerminator) {
            textStart = i + terminatorWidth
            break
        }
        i += terminatorWidth
    }
    if (textStart >= data.size) return null
    return String(data, textStart, data.size - textStart, charset).trim().ifEmpty { null }
}

private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(contentUri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()
    )
    .build()

// Bridges Guava's ListenableFuture to a suspend call without a Guava/coroutines-guava
// dependency; ContextCompat.getMainExecutor is already available via androidx-core-ktx.
private suspend fun ListenableFuture<MediaController>.awaitController(context: Context): MediaController =
    suspendCancellableCoroutine { cont ->
        addListener({
            runCatching { get() }
                .onSuccess { controller -> if (cont.isActive) cont.resume(controller) }
                .onFailure { ex -> if (cont.isActive) cont.resumeWithException(ex) }
        }, ContextCompat.getMainExecutor(context))
        cont.invokeOnCancellation { cancel(false) }
    }

