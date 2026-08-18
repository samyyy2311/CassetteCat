package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackUiState
import `in`.caffeinelabs.cassettecat.ui.components.QueueList
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel

@Composable
internal fun NowPlayingPlayerView(
    song: Song,
    state: PlaybackUiState,
    positionMs: Long,
    isFavorite: Boolean,
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
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(16.dp))
        val queueBottomPadDp by animateDpAsState(
            targetValue = if (chromeVisible) 240.dp else 64.dp,
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f),
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
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f),
                label = "queueControlsAlpha"
            )
            val queueControlsSlideY by animateFloatAsState(
                targetValue = if (chromeVisible) 0f else with(density) { 16.dp.toPx() },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f),
                label = "queueControlsSlide"
            )
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
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LyricsView(
                lyrics = state.currentLyrics ?: fallbackLyrics,
                syncedLyrics = syncedLyrics,
                positionMs = positionMs,
                durationMs = state.durationMs,
                artist = song.artist,
                lyricsProvider = lyricsProvider,
                isLoading = isLoadingLyrics,
                isPlaying = state.isPlaying,
                scrollState = lyricsScrollState,
                listState = lyricsListState,
                controlsVisible = chromeVisible,
                onSeekToLine = {
                    playbackViewModel.seekTo(it)
                    onUserSeekOrInteraction(true)
                },
                onInteraction = { onUserSeekOrInteraction(false) },
                onScrollDelta = onScrollDelta,
                modifier = Modifier.fillMaxSize()
            )
            val lyricsControlsAlpha by animateFloatAsState(
                targetValue = if (chromeVisible) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f),
                label = "lyricsControlsAlpha"
            )
            val lyricsControlsSlideY by animateFloatAsState(
                targetValue = if (chromeVisible) 0f else with(density) { 16.dp.toPx() },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f),
                label = "lyricsControlsSlide"
            )
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
