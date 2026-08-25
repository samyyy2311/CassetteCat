package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
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
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val art: @Composable (Modifier) -> Unit = { modifier ->
        AlbumArtCarousel(
            currentSong = song,
            previousSong = state.previousInQueue,
            nextSong = state.upNext.firstOrNull(),
            onSwipeNext = onSkipNext,
            onSwipePrevious = onSkipPrevious,
            collapsedArtRect = collapsedArtRect,
            expandFraction = expandFraction,
            onSwipeUp = {
                if (preferences.swipeUpLyricsEnabled) {
                    onActiveViewChange(NowPlayingView.LYRICS)
                }
            },
            isPlaying = state.isPlaying,
            modifier = modifier
        )
    }

    val info: @Composable () -> Unit = {
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
            ListeningRoomStatusPill(listeningRoomState)
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
            playWhenReady = state.playWhenReady,
            onTogglePlayPause = { playbackViewModel.togglePlayPause() },
            onSkipNext = onSkipNext,
            repeatMode = state.repeatMode,
            onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() },
            modifier = Modifier.graphicsLayer { alpha = chromeDragAlpha }
        )
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            art(
                Modifier
                    .fillMaxHeight(0.85f)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
            )
            Spacer(Modifier.width(28.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                info()
                Spacer(Modifier.height(24.dp))
                LyricsQueueToggleRow(activeView = activeView, onActiveViewChange = onActiveViewChange)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.weight(4f))
            art(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .aspectRatio(1f)
            )
            Spacer(Modifier.weight(3f))
            info()
            Spacer(Modifier.height(64.dp))
            LyricsQueueToggleRow(activeView = activeView, onActiveViewChange = onActiveViewChange)
        }
    }
}

@Composable
internal fun ListeningRoomStatusPill(listeningRoomState: ListeningRoomState) {
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
    onSaveQueue: (() -> Unit)?,
    onQueueInteraction: () -> Unit,
    onQueueScrollDelta: (Float) -> Unit
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        val upNextHeaderIndex = if (state.history.isNotEmpty()) state.history.size + 1 else 0
        queueListState.scrollToItem(upNextHeaderIndex)
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
                onSaveQueue = onSaveQueue,
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
                            playWhenReady = state.playWhenReady,
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
    var syncOffsetMs by remember(song.id, syncedLyrics) { mutableLongStateOf(0L) }
    var showSyncTuner by remember(song.id) { mutableStateOf(false) }
    var selectionMode by remember(song.id) { mutableStateOf(false) }
    var selectedIndices by remember(song.id) { mutableStateOf(setOf<Int>()) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showLrcLibSearchSheet by remember { mutableStateOf(false) }

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
        if (!syncedLyrics.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSyncTuner) {
                    LyricsSyncTunerBar(
                        syncOffsetMs = syncOffsetMs,
                        onNudge = { delta -> syncOffsetMs = (syncOffsetMs + delta).coerceIn(-10_000L, 10_000L) },
                        onReset = { syncOffsetMs = 0L },
                        onClose = { showSyncTuner = false }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                            .clickable { showSyncTuner = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_timer),
                            contentDescription = "Tune Lyrics Sync",
                            tint = if (syncOffsetMs != 0L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (syncOffsetMs == 0L) "Sync" else String.format(Locale.US, "%+dms", syncOffsetMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                            color = if (syncOffsetMs != 0L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
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
                onSearchLyrics = { showLrcLibSearchSheet = true },
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
                        .navigationBarsPadding()
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
                            PlaybackControlsRow(
                                positionMs = positionMs,
                                durationMs = state.durationMs,
                                onSeek = { playbackViewModel.seekTo(it) },
                                isShuffleEnabled = state.isShuffleEnabled,
                                onToggleShuffle = { playbackViewModel.toggleShuffle() },
                                onSkipPrevious = onSkipPrevious,
                                isPlaying = state.isPlaying,
                                playWhenReady = state.playWhenReady,
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

    if (showLrcLibSearchSheet) {
        LrcLibSearchSheet(
            song = song,
            playbackViewModel = playbackViewModel,
            onDismiss = { showLrcLibSearchSheet = false }
        )
    }
}

@Composable
private fun LyricsSyncTunerBar(
    syncOffsetMs: Long,
    onNudge: (Long) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNudge(-100L)
                }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text("-100ms", style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (syncOffsetMs != 0L) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReset()
                }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            val text = if (syncOffsetMs == 0L) "Sync: 0ms" else String.format(Locale.US, "Sync: %+dms", syncOffsetMs)
            Text(
                text,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = if (syncOffsetMs != 0L) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNudge(100L)
                }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text("+100ms", style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily))
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_x),
                contentDescription = "Close Sync Tuner",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
