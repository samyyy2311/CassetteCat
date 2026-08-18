package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.util.ScreenshotCaptureEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NowPlayingView { PLAYER, QUEUE, LYRICS }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingContent(
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
    fraction: Float = 1f,
    collapsedArtRect: State<Rect?>? = null,
    activeView: NowPlayingView = NowPlayingView.PLAYER,
    onActiveViewChange: (NowPlayingView) -> Unit = {},
    onCollapseRequest: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    playlists: List<Playlist> = emptyList(),
    onNavigateToPlaylist: (String) -> Unit = {},
    drawBehindSystemBars: Boolean = false,
    onHeaderDragProgressChange: (Float) -> Unit = {}
) {
    val state by playbackViewModel.playbackState.collectAsState()
    val positionMs by playbackViewModel.positionMs.collectAsState()
    val isFavorite by playbackViewModel.isCurrentSongFavorite.collectAsState()
    val syncedLyrics by playbackViewModel.syncedLyrics.collectAsState()
    val fallbackLyrics by playbackViewModel.fallbackLyrics.collectAsState()
    val lyricsProvider by playbackViewModel.lyricsProvider.collectAsState()
    val isLoadingLyrics by playbackViewModel.isLoadingLyrics.collectAsState()
    val sleepTimerEndMs by playbackViewModel.sleepTimerEndMs.collectAsState()
    val listeningRoom by playbackViewModel.listeningRoom.collectAsState()
    val song = state.currentSong

    val sheetState = rememberNowPlayingSheetState()
    val context = LocalContext.current
    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val latestSong by rememberUpdatedState(song)

    LaunchedEffect(Unit) {
        ScreenshotCaptureEvents.events.collect {
            if (latestSong != null) sheetState.showScreenshotSuggestion = true
        }
    }

    BackHandler(enabled = activeView != NowPlayingView.PLAYER) { onActiveViewChange(NowPlayingView.PLAYER) }

    val queueListState = rememberLazyListState()
    val lyricsScrollState = rememberScrollState()
    val lyricsListState = rememberLazyListState()
    val density = LocalDensity.current

    var lyricsControlsVisible by remember(song?.id, activeView) { mutableStateOf(false) }
    var queueControlsVisible by remember(song?.id, activeView) { mutableStateOf(false) }
    var userInteractionCounter by remember { mutableStateOf(0) }

    val chromeVisible = when (activeView) {
        NowPlayingView.PLAYER -> true
        NowPlayingView.QUEUE -> queueControlsVisible || !state.isPlaying
        NowPlayingView.LYRICS -> lyricsControlsVisible || !state.isPlaying
    }

    LaunchedEffect(userInteractionCounter, state.isPlaying, activeView, queueControlsVisible, lyricsControlsVisible) {
        if (state.isPlaying) {
            if (activeView == NowPlayingView.QUEUE && queueControlsVisible) {
                delay(4_000L)
                queueControlsVisible = false
            } else if (activeView == NowPlayingView.LYRICS && lyricsControlsVisible) {
                delay(4_000L)
                lyricsControlsVisible = false
            }
        }
    }

    var headerDragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dragScope = rememberCoroutineScope()
    val headerDragCapPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val headerDragRevealPx = with(density) { HEADER_DRAG_REVEAL_DISTANCE.toPx() }
    val currentOnHeaderDragProgressChange by rememberUpdatedState(onHeaderDragProgressChange)

    LaunchedEffect(headerDragOffsetPx, headerDragRevealPx) {
        currentOnHeaderDragProgressChange((headerDragOffsetPx / headerDragRevealPx).coerceIn(0f, 1f))
    }
    DisposableEffect(Unit) {
        onDispose { currentOnHeaderDragProgressChange(0f) }
    }

    val skipNext: () -> Unit = { playbackViewModel.skipNext() }
    val skipPrevious: () -> Unit = { playbackViewModel.skipPrevious() }
    val handleCollapseRequest: () -> Unit = {
        onCollapseRequest()
        dragScope.launch {
            animate(headerDragOffsetPx, headerDragCapPx, animationSpec = tween(HEADER_COLLAPSE_EXIT_MS, easing = SmoothEasing)) { value, _ ->
                headerDragOffsetPx = value
            }
            headerDragOffsetPx = 0f
        }
    }
    val handleHeaderDrag: (Float) -> Unit = { delta ->
        headerDragOffsetPx = (headerDragOffsetPx + delta).coerceIn(0f, headerDragCapPx)
    }
    val handleHeaderSpringBack: () -> Unit = {
        val start = headerDragOffsetPx
        dragScope.launch {
            animate(start, 0f, animationSpec = tween(HEADER_SPRING_BACK_MS, easing = SmoothEasing)) { value, _ ->
                headerDragOffsetPx = value
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .graphicsLayer { translationY = headerDragOffsetPx }
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (song != null) NowPlayingBackdrop(song)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp, bottom = 20.dp)
        ) {
            if (song == null) {
                EmptyState(
                    iconRes = R.drawable.lucide_ic_music,
                    title = "Nothing playing",
                    message = "Choose a song from your library to start listening."
                )
                return@Column
            }

            Crossfade(
                targetState = activeView,
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(VIEW_TRANSITION_MS, easing = SmoothEasing),
                label = "nowPlayingView"
            ) { view ->
                when (view) {
                    NowPlayingView.QUEUE -> NowPlayingQueueView(
                        song = song,
                        state = state,
                        positionMs = positionMs,
                        isFavorite = isFavorite,
                        chromeVisible = chromeVisible,
                        queueListState = queueListState,
                        activeView = activeView,
                        playbackViewModel = playbackViewModel,
                        onActiveViewChange = onActiveViewChange,
                        onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                        onShowMenu = { sheetState.showMenu = true },
                        onShowGoToMenu = { sheetState.showGoToMenu = true },
                        onCollapseRequest = handleCollapseRequest,
                        onHeaderDrag = handleHeaderDrag,
                        onHeaderSpringBack = handleHeaderSpringBack,
                        onSkipNext = skipNext,
                        onSkipPrevious = skipPrevious,
                        onPlaySong = {
                            playbackViewModel.playFromQueue(it)
                            queueControlsVisible = true
                            userInteractionCounter++
                        },
                        onQueueInteraction = {
                            queueControlsVisible = !queueControlsVisible
                            userInteractionCounter++
                        },
                        onQueueScrollDelta = { delta ->
                            if (delta > 1.5f) {
                                queueControlsVisible = true
                                userInteractionCounter++
                            } else if (delta < -1.5f) {
                                queueControlsVisible = false
                            }
                        }
                    )
                    NowPlayingView.LYRICS -> NowPlayingLyricsView(
                        song = song,
                        state = state,
                        positionMs = positionMs,
                        syncedLyrics = syncedLyrics,
                        fallbackLyrics = fallbackLyrics,
                        lyricsProvider = lyricsProvider,
                        isLoadingLyrics = isLoadingLyrics,
                        isFavorite = isFavorite,
                        chromeVisible = chromeVisible,
                        lyricsScrollState = lyricsScrollState,
                        lyricsListState = lyricsListState,
                        activeView = activeView,
                        playbackViewModel = playbackViewModel,
                        onActiveViewChange = onActiveViewChange,
                        onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                        onShowMenu = { sheetState.showMenu = true },
                        onShowGoToMenu = { sheetState.showGoToMenu = true },
                        onCollapseRequest = handleCollapseRequest,
                        onHeaderDrag = handleHeaderDrag,
                        onHeaderSpringBack = handleHeaderSpringBack,
                        onSkipNext = skipNext,
                        onSkipPrevious = skipPrevious,
                        onUserSeekOrInteraction = { isSeek ->
                            if (isSeek) lyricsControlsVisible = true
                            else lyricsControlsVisible = !lyricsControlsVisible
                            userInteractionCounter++
                        },
                        onScrollDelta = { deltaY ->
                            if (deltaY > 1.5f) {
                                lyricsControlsVisible = true
                                userInteractionCounter++
                            } else if (deltaY < -1.5f) {
                                lyricsControlsVisible = false
                            }
                        }
                    )
                    NowPlayingView.PLAYER -> NowPlayingPlayerView(
                        song = song,
                        state = state,
                        positionMs = positionMs,
                        isFavorite = isFavorite,
                        expandFraction = fraction,
                        collapsedArtRect = collapsedArtRect,
                        activeView = activeView,
                        playbackViewModel = playbackViewModel,
                        onActiveViewChange = onActiveViewChange,
                        onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                        onShowMenu = { sheetState.showMenu = true },
                        onShowGoToMenu = { sheetState.showGoToMenu = true },
                        onSkipNext = skipNext,
                        onSkipPrevious = skipPrevious
                    )
                }
            }
        }
    }

    NowPlayingScreenSheetsHost(
        song = song,
        isFavorite = isFavorite,
        sleepTimerEndMs = sleepTimerEndMs,
        playlists = playlists,
        listeningRoom = listeningRoom,
        playbackViewModel = playbackViewModel,
        downloadRepository = downloadRepository,
        context = context,
        sheetState = sheetState,
        onNavigateToArtist = onNavigateToArtist,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToPlaylist = onNavigateToPlaylist
    )
}

internal val HEADER_COLLAPSE_DRAG_THRESHOLD = 80.dp
private val HEADER_DRAG_REVEAL_DISTANCE = 250.dp
internal val HEADER_COLLAPSE_FLING_VELOCITY = 900.dp
private const val HEADER_COLLAPSE_EXIT_MS = 400
private const val HEADER_SPRING_BACK_MS = 320
private const val VIEW_TRANSITION_MS = 550
internal const val CHROME_DRAG_FADE_FLOOR = 0.6f

internal val SmoothEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

@OptIn(ExperimentalSharedTransitionApi::class)
private val sharedElementBoundsTransform = BoundsTransform { _, _ ->
    tween(VIEW_TRANSITION_MS, easing = SmoothEasing)
}
