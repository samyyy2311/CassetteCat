package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomRole
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.listeningroom.statusSubtitle
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackUiState
import `in`.caffeinelabs.cassettecat.data.playback.adjustLyricsSync
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.components.QueueList
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel

@Composable
internal fun NowPlayingPlayerView(
    song: Song,
    state: PlaybackUiState,
    positionMs: Long,
    isFavorite: Boolean,
    listeningRoomState: ListeningRoomState,
    expandFraction: Float,
    collapsedArtRect: State<Rect?>?,
    activeView: NowPlayingView,
    playbackViewModel: PlaybackViewModel,
    onActiveViewChange: (NowPlayingView) -> Unit,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    onShowGoToMenu: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
) {
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.weight(4f))
        AlbumArtCarousel(
            currentSong = song,
            previousSong = state.previousInQueue,
            nextSong = state.upNext.firstOrNull(),
            onSwipeNext = onSkipNext,
            onSwipePrevious = onSkipPrevious,
            collapsedArtRect = collapsedArtRect,
            expandFraction = expandFraction,
            onDoubleTapSeek = { isForward ->
                val stepMs = preferences.seekStepSeconds * 1000L
                val targetMs = if (isForward) {
                    (positionMs + stepMs).coerceAtMost(state.durationMs)
                } else {
                    (positionMs - stepMs).coerceAtLeast(0L)
                }
                playbackViewModel.seekTo(targetMs)
            },
            onSwipeUp = {
                if (preferences.swipeUpLyricsEnabled) {
                    onActiveViewChange(NowPlayingView.LYRICS)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .aspectRatio(1f)
        )
        Spacer(Modifier.weight(3f))
        TitleRow(
            song = song,
            isFavorite = isFavorite,
            showThumbnail = false,
            onToggleFavorite = onToggleFavorite,
            onShowMenu = onShowMenu,
            onArtistClick = onShowGoToMenu
        )
        if (listeningRoomState.role != ListeningRoomRole.NONE) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_users),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.height(14.dp).width(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    listeningRoomState.statusSubtitle(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        val chromeDragAlpha = ((expandFraction - CHROME_DRAG_FADE_FLOOR) / (1f - CHROME_DRAG_FADE_FLOOR))
            .coerceIn(0f, 1f)
        PlaybackControlsRow(
            positionMs = positionMs,
            durationMs = state.durationMs,
            onSeek = { playbackViewModel.seekTo(it) },
            isShuffleEnabled = state.isShuffleEnabled,
            onToggleShuffle = { playbackViewModel.toggleShuffle() },
            onSkipPrevious = onSkipPrevious,
            isPlaying = state.isPlaying,
            onTogglePlayPause = { playbackViewModel.togglePlayPause() },
            onSkipNext = onSkipNext,
            repeatMode = state.repeatMode,
            onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() },
            modifier = Modifier.graphicsLayer { alpha = chromeDragAlpha }
        )
        Spacer(Modifier.height(64.dp))
        LyricsQueueToggleRow(activeView = activeView, onActiveViewChange = onActiveViewChange)
    }
}

@Composable
internal fun NowPlayingQueueView(
    song: Song,
    state: PlaybackUiState,
    positionMs: Long,
    isFavorite: Boolean,
    chromeVisible: Boolean,
    queueListState: LazyListState,
    activeView: NowPlayingView,
    playbackViewModel: PlaybackViewModel,
    onActiveViewChange: (NowPlayingView) -> Unit,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    onShowGoToMenu: () -> Unit,
    onCollapseRequest: () -> Unit,
    onHeaderDrag: (Float) -> Unit,
    onHeaderSpringBack: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onQueueInteraction: () -> Unit,
    onQueueScrollDelta: (Float) -> Unit
) {
    val density = LocalDensity.current
    Column(modifier = Modifier.fillMaxSize()) {
        TitleRow(
            song = song,
            isFavorite = isFavorite,
            showThumbnail = true,
            onToggleFavorite = onToggleFavorite,
            onShowMenu = onShowMenu,
            onArtistClick = onShowGoToMenu,
            onThumbnailClick = { onActiveViewChange(NowPlayingView.PLAYER) },
            onCollapseRequest = onCollapseRequest,
            onHeaderDrag = onHeaderDrag,
            onHeaderSpringBack = onHeaderSpringBack,
            enableHeaderDrag = true,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(16.dp))
        val queueBottomPadDp by animateDpAsState(
            targetValue = if (chromeVisible) 240.dp else 64.dp,
            animationSpec = tween(220, easing = SmoothEasing),
            label = "queueBottomPad"
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            QueueList(
                upNext = state.upNext,
                history = state.history,
                onSongClick = onPlaySong,
                onReorderUpNext = { from, to -> playbackViewModel.moveInUpNext(from, to) },
                onRemoveUpNext = { playbackViewModel.removeFromUpNext(it.id) },
                onClearHistory = { playbackViewModel.clearHistory() },
                onScrollDelta = onQueueScrollDelta,
                bottomPaddingDp = queueBottomPadDp,
                controlsVisible = chromeVisible,
                onInteraction = onQueueInteraction,
                state = queueListState,
                modifier = Modifier.fillMaxSize()
            )
            val queueControlsAlpha by animateFloatAsState(
                targetValue = if (chromeVisible) 1f else 0f,
                animationSpec = tween(220, easing = SmoothEasing),
                label = "queueControlsAlpha"
            )
            val queueControlsSlideY by animateFloatAsState(
                targetValue = if (chromeVisible) 0f else with(density) { 16.dp.toPx() },
                animationSpec = tween(220, easing = SmoothEasing),
                label = "queueControlsSlide"
            )
            if (chromeVisible || queueControlsAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer {
                            alpha = queueControlsAlpha
                            translationY = queueControlsSlideY
                        }
                        .then(
                            if (chromeVisible) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { }
                                )
                            } else Modifier
                        )
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        PlaybackControlsRow(
                            positionMs = positionMs,
                            durationMs = state.durationMs,
                            onSeek = { playbackViewModel.seekTo(it) },
                            isShuffleEnabled = state.isShuffleEnabled,
                            onToggleShuffle = { playbackViewModel.toggleShuffle() },
                            onSkipPrevious = onSkipPrevious,
                            isPlaying = state.isPlaying,
                            onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                            onSkipNext = onSkipNext,
                            repeatMode = state.repeatMode,
                            onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() }
                        )
                        Spacer(Modifier.height(64.dp))
                        LyricsQueueToggleRow(activeView = activeView, onActiveViewChange = onActiveViewChange)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NowPlayingLyricsView(
    song: Song,
    state: PlaybackUiState,
    positionMs: Long,
    syncedLyrics: List<LyricLine>?,
    fallbackLyrics: String?,
    lyricsProvider: String?,
    isLoadingLyrics: Boolean,
    isFavorite: Boolean,
    chromeVisible: Boolean,
    lyricsScrollState: ScrollState,
    lyricsListState: LazyListState,
    activeView: NowPlayingView,
    playbackViewModel: PlaybackViewModel,
    onActiveViewChange: (NowPlayingView) -> Unit,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    onShowGoToMenu: () -> Unit,
    onCollapseRequest: () -> Unit,
    onHeaderDrag: (Float) -> Unit,
    onHeaderSpringBack: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onUserSeekOrInteraction: (Boolean) -> Unit,
    onScrollDelta: (Float) -> Unit
) {
    val density = LocalDensity.current
    var syncOffsetMs by remember(song.id, syncedLyrics) { mutableStateOf(0L) }
    var selectionMode by remember(song.id) { mutableStateOf(false) }
    var selectedIndices by remember(song.id) { mutableStateOf(setOf<Int>()) }
    var showShareSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIndices = emptySet()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TitleRow(
            song = song,
            isFavorite = isFavorite,
            showThumbnail = true,
            onToggleFavorite = onToggleFavorite,
            onShowMenu = onShowMenu,
            onArtistClick = onShowGoToMenu,
            onThumbnailClick = { onActiveViewChange(NowPlayingView.PLAYER) },
            onCollapseRequest = onCollapseRequest,
            onHeaderDrag = onHeaderDrag,
            onHeaderSpringBack = onHeaderSpringBack,
            enableHeaderDrag = true,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LyricsView(
                lyrics = state.currentLyrics ?: fallbackLyrics,
                syncedLyrics = syncedLyrics,
                positionMs = positionMs,
                durationMs = state.durationMs,
                syncOffsetMs = syncOffsetMs,
                artist = song.artist,
                lyricsProvider = lyricsProvider,
                isLoading = isLoadingLyrics,
                isPlaying = state.isPlaying,
                scrollState = lyricsScrollState,
                listState = lyricsListState,
                controlsVisible = chromeVisible && !selectionMode,
                selectionMode = selectionMode,
                selectedIndices = selectedIndices,
                onToggleLineSelection = { idx ->
                    selectedIndices = if (selectedIndices.contains(idx)) {
                        val remaining = selectedIndices - idx
                        if (remaining.isEmpty()) {
                            selectionMode = false
                            emptySet()
                        } else {
                            remaining
                        }
                    } else {
                        if (selectedIndices.size < 5) {
                            selectedIndices + idx
                        } else {
                            setOf(idx)
                        }
                    }
                },
                onStartSelection = { idx ->
                    selectionMode = true
                    selectedIndices = setOf(idx)
                },
                onSeekToLine = {
                    playbackViewModel.seekTo(it)
                    onUserSeekOrInteraction(true)
                },
                onInteraction = {
                    if (selectionMode) {
                        selectionMode = false
                        selectedIndices = emptySet()
                    } else {
                        onUserSeekOrInteraction(false)
                    }
                },
                onScrollDelta = onScrollDelta,
                onReturnToPlayer = { onActiveViewChange(NowPlayingView.PLAYER) },
                modifier = Modifier.fillMaxSize()
            )

            if (selectionMode) {
                FloatingLyricSelectionBar(
                    selectedCount = selectedIndices.size,
                    onCancel = {
                        selectionMode = false
                        selectedIndices = emptySet()
                    },
                    onShare = {
                        if (selectedIndices.isNotEmpty()) {
                            showShareSheet = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                )
            } else {
                val lyricsControlsAlpha by animateFloatAsState(
                    targetValue = if (chromeVisible) 1f else 0f,
                    animationSpec = tween(220, easing = SmoothEasing),
                    label = "lyricsControlsAlpha"
                )
                val lyricsControlsSlideY by animateFloatAsState(
                    targetValue = if (chromeVisible) 0f else with(density) { 16.dp.toPx() },
                    animationSpec = tween(220, easing = SmoothEasing),
                    label = "lyricsControlsSlide"
                )
                if (chromeVisible || lyricsControlsAlpha > 0.001f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .graphicsLayer {
                                alpha = lyricsControlsAlpha
                                translationY = lyricsControlsSlideY
                            }
                            .then(
                                if (chromeVisible) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { }
                                    )
                                } else Modifier
                            )
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            if (!syncedLyrics.isNullOrEmpty()) {
                                LyricsSyncOffsetPill(
                                    syncOffsetMs = syncOffsetMs,
                                    onAdjust = { deltaMs -> syncOffsetMs += deltaMs },
                                    onReset = { syncOffsetMs = 0L },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = 12.dp)
                                )
                            }
                            PlaybackControlsRow(
                                positionMs = positionMs,
                                durationMs = state.durationMs,
                                onSeek = { playbackViewModel.seekTo(it) },
                                isShuffleEnabled = state.isShuffleEnabled,
                                onToggleShuffle = { playbackViewModel.toggleShuffle() },
                                onSkipPrevious = onSkipPrevious,
                                isPlaying = state.isPlaying,
                                onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                                onSkipNext = onSkipNext,
                                repeatMode = state.repeatMode,
                                onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() }
                            )
                            Spacer(Modifier.height(64.dp))
                            LyricsQueueToggleRow(activeView = activeView, onActiveViewChange = onActiveViewChange)
                        }
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        val lines = remember(selectedIndices, syncedLyrics, syncOffsetMs, state.currentLyrics, fallbackLyrics) {
            if (!syncedLyrics.isNullOrEmpty()) {
                val effective = adjustLyricsSync(syncedLyrics, syncOffsetMs)
                selectedIndices.sorted().mapNotNull { idx -> effective.getOrNull(idx)?.text }
            } else {
                val plainLines = (state.currentLyrics ?: fallbackLyrics)?.lines()?.filter { it.isNotBlank() } ?: emptyList()
                selectedIndices.sorted().mapNotNull { idx -> plainLines.getOrNull(idx) }
            }
        }
        LyricShareSheet(
            song = song,
            selectedLines = lines,
            onDismiss = {
                showShareSheet = false
                selectionMode = false
                selectedIndices = emptySet()
            }
        )
    }
}
