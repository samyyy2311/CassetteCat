package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.media3.common.Player
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomRole
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.listeningroom.NearbyListeningRoom
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.PlaylistCoverArt
import `in`.caffeinelabs.cassettecat.ui.components.QueueList
import `in`.caffeinelabs.cassettecat.ui.screens.library.splitArtists
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.shareSongs
import `in`.caffeinelabs.cassettecat.ui.util.ScreenshotCaptureEvents
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NowPlayingView { PLAYER, QUEUE, LYRICS }

private enum class LyricsControlsState { HIDDEN, VISIBLE_TIMED, VISIBLE_PINNED }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingContent(
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
    fraction: Float = 1f,
    // Mini-player thumbnail bounds (see MiniPlayerRow's onThumbnailBoundsChange), used to
    // morph the big art into it on collapse instead of cross-fading. Taken as State, not
    // Rect, so it's only read in the draw-phase graphicsLayer below, not at composition
    // time, which would recompose this whole composable every drag frame.
    collapsedArtRect: State<Rect?>? = null,
    activeView: NowPlayingView = NowPlayingView.PLAYER,
    onActiveViewChange: (NowPlayingView) -> Unit = {},
    onCollapseRequest: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    playlists: List<Playlist> = emptyList(),
    onNavigateToPlaylist: (String) -> Unit = {},
    // Lyrics can be composed at the activity root, behind status/navigation bars. Content
    // still observes their safe area while its atmospheric background uses the full window.
    drawBehindSystemBars: Boolean = false,
    // Only fires from the Queue/Lyrics header drag. In Player mode the scaffold's native
    // swipe already reveals what's behind it; this approximates that for the other two
    // views by fading the sheet's fill in step with the header drag instead.
    onHeaderDragProgressChange: (Float) -> Unit = {}
) {
    val state by playbackViewModel.playbackState.collectAsState()
    val positionMs by playbackViewModel.positionMs.collectAsState()
    val isFavorite by playbackViewModel.isCurrentSongFavorite.collectAsState()
    val syncedLyrics by playbackViewModel.syncedLyrics.collectAsState()
    val fallbackLyrics by playbackViewModel.fallbackLyrics.collectAsState()
    val lyricsProvider by playbackViewModel.lyricsProvider.collectAsState()
    val sleepTimerEndMs by playbackViewModel.sleepTimerEndMs.collectAsState()
    val listeningRoom by playbackViewModel.listeningRoom.collectAsState()
    val song = state.currentSong
    var showMenu by remember { mutableStateOf(false) }
    var showGoToMenu by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showSleepTimerPicker by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    var showOutputPicker by remember { mutableStateOf(false) }
    var showListeningRoom by remember { mutableStateOf(false) }
    var showScreenshotSuggestion by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val latestSong by rememberUpdatedState(song)

    // Android only tells us that a screenshot was taken. When there is a current song, turn that
    // signal into one useful, dismissible action instead of inspecting or retaining the image.
    LaunchedEffect(Unit) {
        ScreenshotCaptureEvents.events.collect {
            if (latestSong != null) showScreenshotSuggestion = true
        }
    }

    BackHandler(enabled = activeView != NowPlayingView.PLAYER) { onActiveViewChange(NowPlayingView.PLAYER) }

    // Hoisted list state keeps the current timed lyric centred without recreating the list.
    val queueListState = rememberLazyListState()
    val lyricsScrollState = rememberScrollState()
    val lyricsListState = rememberLazyListState()
    val density = LocalDensity.current
    // Lyrics stay focused by default. A tap reveals the transport controls briefly; timed
    // lyric auto-scrolls never affect this state.
    // Both Queue and Lyrics stay completely hidden and unobtrusive by default.
    // They ONLY reveal when nudged (e.g. background tap, song/line select, or intentional scroll up),
    // and automatically fade back away after 4.5 seconds.
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

    val onQueueScroll: (Float) -> Unit = { delta ->
        if (delta > 1.5f) {
            queueControlsVisible = true
            userInteractionCounter++
        } else if (delta < -1.5f) {
            queueControlsVisible = false
        }
    }


    var headerDragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dragScope = rememberCoroutineScope()
    // Full screen height, not a small fixed cap, so the drag tracks the finger the whole
    // way instead of feeling like it gives up partway (a ~120dp cap did that).
    val headerDragCapPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    // Shorter than headerDragCapPx: the reveal should complete well before the drag
    // reaches its visual cap, not be gated behind dragging most of the screen height.
    val headerDragRevealPx = with(density) { HEADER_DRAG_REVEAL_DISTANCE.toPx() }
    val currentOnHeaderDragProgressChange by rememberUpdatedState(onHeaderDragProgressChange)
    LaunchedEffect(headerDragOffsetPx, headerDragRevealPx) {
        currentOnHeaderDragProgressChange((headerDragOffsetPx / headerDragRevealPx).coerceIn(0f, 1f))
    }
    // Belt-and-suspenders: guards against this getting torn down mid-animation (unmounted
    // once the real sheet collapse drops the expand fraction low enough, cancelling the
    // coroutine driving headerDragOffsetPx to 0 before it finishes) and leaving the
    // reported progress stuck non-zero. onDispose is guaranteed to run when that happens.
    DisposableEffect(Unit) {
        onDispose { currentOnHeaderDragProgressChange(0f) }
    }
    val skipNext: () -> Unit = { playbackViewModel.skipNext() }
    val skipPrevious: () -> Unit = { playbackViewModel.skipPrevious() }
    val handleCollapseRequest: () -> Unit = {
        // Keeps the local drag visual animating off-screen instead of snapping to 0, so it
        // doesn't look like the gesture stalls while the separate, slower sheet animation starts.
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
                NowPlayingView.QUEUE -> Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TitleRow(
                        song = song,
                        isFavorite = isFavorite,
                        showThumbnail = true,
                        onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                        onShowMenu = { showMenu = true },
                        onArtistClick = { showGoToMenu = true },
                        onThumbnailClick = { onActiveViewChange(NowPlayingView.PLAYER) },
                        onCollapseRequest = handleCollapseRequest,
                        onHeaderDrag = handleHeaderDrag,
                        onHeaderSpringBack = handleHeaderSpringBack,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    val queueBottomPadDp by animateDpAsState(
                        targetValue = if (chromeVisible) 240.dp else 64.dp,
                        animationSpec = spring(
                            dampingRatio = 0.86f,
                            stiffness = 400f
                        ),
                        label = "queueBottomPad"
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        QueueList(
                            upNext = state.upNext,
                            history = state.history,
                            onSongClick = {
                                playbackViewModel.playFromQueue(it)
                                queueControlsVisible = true
                                userInteractionCounter++
                            },
                            onReorderUpNext = { from, to -> playbackViewModel.moveInUpNext(from, to) },
                            onRemoveUpNext = { playbackViewModel.removeFromUpNext(it.id) },
                            onClearHistory = { playbackViewModel.clearHistory() },
                            onScrollDelta = onQueueScroll,
                            bottomPaddingDp = queueBottomPadDp,
                            controlsVisible = chromeVisible,
                            onInteraction = {
                                queueControlsVisible = !queueControlsVisible
                                userInteractionCounter++
                            },
                            state = queueListState,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Controls overlay — animated alpha/slide with touch shielding
                            val queueControlsAlpha by animateFloatAsState(
                                targetValue = if (chromeVisible) 1f else 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = 400f
                                ),
                                label = "queueControlsAlpha"
                            )
                            val queueControlsSlideY by animateFloatAsState(
                                targetValue = if (chromeVisible) 0f else with(density) { 16.dp.toPx() },
                                animationSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = 400f
                                ),
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
                                                onClick = { /* Consume clicks so they don't click items behind controls */ }
                                            )
                                        } else Modifier
                                    )
                            ) {
                                Column(
                                    Modifier.fillMaxWidth()
                                ) {
                                    PlaybackControlsRow(
                                        positionMs = positionMs,
                                        durationMs = state.durationMs,
                                        onSeek = { playbackViewModel.seekTo(it) },
                                        isShuffleEnabled = state.isShuffleEnabled,
                                        onToggleShuffle = { playbackViewModel.toggleShuffle() },
                                        onSkipPrevious = skipPrevious,
                                        isPlaying = state.isPlaying,
                                        onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                                        onSkipNext = skipNext,
                                        repeatMode = state.repeatMode,
                                        onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() }
                                    )
                                    Spacer(Modifier.height(64.dp))
                                    LyricsQueueToggleRow(activeView = activeView, onActiveViewChange = onActiveViewChange)
                                }
                            }
                        }
                    }
                NowPlayingView.LYRICS -> Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TitleRow(
                        song = song,
                        isFavorite = isFavorite,
                        showThumbnail = true,
                        onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                        onShowMenu = { showMenu = true },
                        onArtistClick = { showGoToMenu = true },
                        onThumbnailClick = { onActiveViewChange(NowPlayingView.PLAYER) },
                        onCollapseRequest = handleCollapseRequest,
                        onHeaderDrag = handleHeaderDrag,
                        onHeaderSpringBack = handleHeaderSpringBack,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LyricsView(
                            lyrics = state.currentLyrics ?: fallbackLyrics,
                            syncedLyrics = syncedLyrics,
                            positionMs = positionMs,
                            durationMs = state.durationMs,
                            artist = song?.artist,
                            lyricsProvider = lyricsProvider,
                            isPlaying = state.isPlaying,
                            scrollState = lyricsScrollState,
                            listState = lyricsListState,
                            controlsVisible = chromeVisible,
                            onSeekToLine = {
                                playbackViewModel.seekTo(it)
                                lyricsControlsVisible = true
                                userInteractionCounter++
                            },
                            onInteraction = {
                                lyricsControlsVisible = !lyricsControlsVisible
                                userInteractionCounter++
                            },
                            onScrollDelta = { deltaY ->
                                if (deltaY > 1.5f) {
                                    lyricsControlsVisible = true
                                    userInteractionCounter++
                                } else if (deltaY < -1.5f) {
                                    lyricsControlsVisible = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                            // Controls overlay — always composed, never removed from tree,
                            // so there is no pop-in on re-show.
                            val lyricsControlsAlpha by animateFloatAsState(
                                targetValue = if (chromeVisible) 1f else 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = 400f
                                ),
                                label = "lyricsControlsAlpha"
                            )
                            val lyricsControlsSlideY by animateFloatAsState(
                                targetValue = if (chromeVisible) 0f else with(density) { 16.dp.toPx() },
                                animationSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = 400f
                                ),
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
                                                onClick = { /* Intercept tap so it doesn't click lyric lines underneath */ }
                                            )
                                        } else Modifier
                                    )
                            ) {
                                Column(
                                    Modifier.fillMaxWidth()
                                ) {
                                    PlaybackControlsRow(
                                        positionMs = positionMs,
                                        durationMs = state.durationMs,
                                        onSeek = { playbackViewModel.seekTo(it) },
                                        isShuffleEnabled = state.isShuffleEnabled,
                                        onToggleShuffle = { playbackViewModel.toggleShuffle() },
                                        onSkipPrevious = skipPrevious,
                                        isPlaying = state.isPlaying,
                                        onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                                        onSkipNext = skipNext,
                                        repeatMode = state.repeatMode,
                                        onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() }
                                    )
                                    Spacer(Modifier.height(64.dp))
                                    LyricsQueueToggleRow(activeView = activeView, onActiveViewChange = onActiveViewChange)
                                }
                            }
                        }
                    }
                NowPlayingView.PLAYER -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                        Spacer(Modifier.weight(4f))
                        AlbumArtCarousel(
                            currentSong = song,
                            previousSong = state.previousInQueue,
                            nextSong = state.upNext.firstOrNull(),
                            onSwipeNext = skipNext,
                            onSwipePrevious = skipPrevious,
                            collapsedArtRect = collapsedArtRect,
                            expandFraction = fraction,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .aspectRatio(1f)
                        )
                        Spacer(Modifier.weight(3f))
                        TitleRow(
                            song = song,
                            isFavorite = isFavorite,
                            showThumbnail = false,
                            onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                            onShowMenu = { showMenu = true },
                            onArtistClick = { showGoToMenu = true }
                        )
                        Spacer(Modifier.height(20.dp))
                        // Falls to 0 at fraction == CHROME_DRAG_FADE_FLOOR, not 0, so this
                        // fades away early during a collapse drag rather than dragging down
                        // the screen while still visible.
                        val chromeDragAlpha = ((fraction - CHROME_DRAG_FADE_FLOOR) / (1f - CHROME_DRAG_FADE_FLOOR))
                            .coerceIn(0f, 1f)
                        PlaybackControlsRow(
                            positionMs = positionMs,
                            durationMs = state.durationMs,
                            onSeek = { playbackViewModel.seekTo(it) },
                            isShuffleEnabled = state.isShuffleEnabled,
                            onToggleShuffle = { playbackViewModel.toggleShuffle() },
                            onSkipPrevious = skipPrevious,
                            isPlaying = state.isPlaying,
                            onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                            onSkipNext = skipNext,
                            repeatMode = state.repeatMode,
                            onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() },
                            modifier = Modifier.graphicsLayer { alpha = chromeDragAlpha }
                        )
                        Spacer(Modifier.height(64.dp))
                        LyricsQueueToggleRow(activeView = activeView, onActiveViewChange = onActiveViewChange)
                    }
                }
            }
        }

    if (showMenu) {
        song?.let { currentSong ->
            NowPlayingActionsSheet(
                song = currentSong,
                isFavorite = isFavorite,
                sleepTimerEndMs = sleepTimerEndMs,
                onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                onShare = { shareSongs(context, listOf(currentSong)) },
                onAddToQueue = { playbackViewModel.addToUpNext(listOf(currentSong)) },
                onDownload = { downloadRepository.download(currentSong) },
                onOpenCredits = { showCredits = true },
                onOpenOutputPicker = { showOutputPicker = true },
                onOpenListeningRoom = { showListeningRoom = true },
                onOpenSleepTimer = { showSleepTimerPicker = true },
                onDismiss = { showMenu = false }
            )
        }
    }
    if (showGoToMenu) {
        song?.let { currentSong ->
            NowPlayingGoToSheet(
                song = currentSong,
                playlists = playlists,
                onNavigateToArtist = { artist ->
                    showGoToMenu = false
                    onNavigateToArtist(artist)
                },
                onNavigateToAlbum = { albumId ->
                    showGoToMenu = false
                    onNavigateToAlbum(albumId)
                },
                onNavigateToPlaylist = { playlistId ->
                    showGoToMenu = false
                    onNavigateToPlaylist(playlistId)
                },
                onOpenPlaylistPicker = {
                    showGoToMenu = false
                    showPlaylistPicker = true
                },
                onDismiss = { showGoToMenu = false }
            )
        }
    }
    if (showPlaylistPicker) {
        NowPlayingPlaylistPicker(
            playlists = playlists,
            onSelect = { playlistId ->
                showPlaylistPicker = false
                onNavigateToPlaylist(playlistId)
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    if (showSleepTimerPicker) {
        SleepTimerPickerSheet(
            currentEndMs = sleepTimerEndMs,
            onSelect = { durationMs ->
                playbackViewModel.startSleepTimer(durationMs)
                showSleepTimerPicker = false
            },
            onCancel = {
                playbackViewModel.cancelSleepTimer()
                showSleepTimerPicker = false
            },
            onDismiss = { showSleepTimerPicker = false }
        )
    }
    if (showCredits) {
        song?.let { currentSong ->
            SongCreditsSheet(song = currentSong, onDismiss = { showCredits = false })
        }
    }
    if (showOutputPicker) {
        AudioOutputSheet(
            onOpenBluetoothSettings = {
                showOutputPicker = false
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            },
            onDismiss = { showOutputPicker = false }
        )
    }
    if (showListeningRoom) {
        ListeningRoomSheet(
            state = listeningRoom,
            onStart = playbackViewModel::startListeningRoom,
            onFindNearby = playbackViewModel::findNearbyListeningRooms,
            onJoin = playbackViewModel::joinListeningRoom,
            onLeave = playbackViewModel::leaveListeningRoom,
            onDismiss = { showListeningRoom = false }
        )
    }
    if (showScreenshotSuggestion) {
        song?.let { currentSong ->
            ScreenshotSuggestionSheet(
                song = currentSong,
                onShare = {
                    showScreenshotSuggestion = false
                    shareSongs(context, listOf(currentSong))
                },
                onViewCredits = {
                    showScreenshotSuggestion = false
                    showCredits = true
                },
                onDismiss = { showScreenshotSuggestion = false }
            )
        }
    }
}
}

private val HEADER_COLLAPSE_DRAG_THRESHOLD = 80.dp
private val HEADER_DRAG_REVEAL_DISTANCE = 250.dp
private val HEADER_COLLAPSE_FLING_VELOCITY = 900.dp
private const val HEADER_COLLAPSE_EXIT_MS = 400
private const val HEADER_SPRING_BACK_MS = 320
private const val VIEW_TRANSITION_MS = 550
private const val SEEK_HAPTIC_TICK_INTERVAL_MS = 1000L
private const val CHROME_REVEAL_MS = 420
private const val CHROME_HIDE_MS = 320
private const val LYRICS_CONTROLS_TIMEOUT_MS = 4_000L
private const val QUEUE_CONTROLS_IDLE_TIMEOUT_MS = 1_200L
private const val CHROME_DRAG_FADE_FLOOR = 0.6f

// True ease-in-out: gentle at both ends. Plain tween, not spring, per the "mechanical,
// not bouncy" motion brief.
private val SmoothEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

// Material You "Emphasized" easing pair — fast start / soft landing (decelerate) for
// content entering, and gentle start / fast exit (accelerate) for content leaving.
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

// Slower/rounder tween than this started at (200ms, FastOutSlowInEasing), still not a
// spring, so the art's bounds morph moves in lockstep with the fade in transitionSpec below.
@OptIn(ExperimentalSharedTransitionApi::class)
private val sharedElementBoundsTransform = BoundsTransform { _, _ ->
    tween(VIEW_TRANSITION_MS, easing = SmoothEasing)
}

// HorizontalPager's default fling/snap is a spring; this keeps every track-swipe snap in
// the carousel on the same tween-based motion language as the rest of the screen.
private const val CAROUSEL_SNAP_MS = 380

// Real pager, not a hand-rolled drag: [previousSong, currentSong, nextSong] form a sliding
// window so a drag reveals the neighbor's art from the edge (cf. Spotify), and
// HorizontalPager's fling/snap guarantees it always lands on a page.
//
// Settling on a non-current page is the commit signal: calls the skip callback and lets
// real queue state catch up; key(currentSong.id, ...) then rebuilds a fresh pager centered
// on the new song. Seamless because the old pager was already still on that song's art.
@Composable
private fun AlbumArtCarousel(
    currentSong: Song,
    previousSong: Song?,
    nextSong: Song?,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    collapsedArtRect: State<Rect?>?,
    expandFraction: Float,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    key(currentSong.id, previousSong?.id, nextSong?.id) {
        val windowSongs = remember(currentSong.id, previousSong?.id, nextSong?.id) {
            buildList {
                if (previousSong != null) add(previousSong)
                add(currentSong)
                if (nextSong != null) add(nextSong)
            }
        }
        val currentIndex = if (previousSong != null) 1 else 0
        val pagerState = rememberPagerState(initialPage = currentIndex) { windowSongs.size }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { settled ->
                if (settled != currentIndex) {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    if (settled > currentIndex) onSwipeNext() else onSwipePrevious()
                    pagerState.animateScrollToPage(currentIndex)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = tween(CAROUSEL_SNAP_MS, easing = SmoothEasing)
            )
        ) { page ->
            val isCurrentSong = windowSongs[page].id == currentSong.id
            AlbumArtCard(
                song = windowSongs[page],
                collapsedArtRect = if (isCurrentSong) collapsedArtRect else null,
                expandFraction = if (isCurrentSong) expandFraction else 1f,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AlbumArtCard(
    song: Song,
    collapsedArtRect: State<Rect?>? = null,
    expandFraction: Float = 1f,
    modifier: Modifier = Modifier
) {
    var expandedArtRect by remember(song.id) { mutableStateOf<Rect?>(null) }
    val targetRect = expandedArtRect
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                if (expandedArtRect != bounds) expandedArtRect = bounds
            }
            .graphicsLayer {
                val sourceRect = collapsedArtRect?.value
                val localTargetRect = targetRect
                val progress = SmoothEasing.transform(expandFraction.coerceIn(0f, 1f))
                val hasSourceAndTarget = sourceRect != null && localTargetRect != null && localTargetRect.width > 0f
                val startScale = if (hasSourceAndTarget && sourceRect != null && localTargetRect != null) {
                    (sourceRect.width / localTargetRect.width).coerceIn(0.05f, 1f)
                } else 1f
                val startX = if (hasSourceAndTarget && sourceRect != null && localTargetRect != null) sourceRect.left - localTargetRect.left else 0f
                val startY = if (hasSourceAndTarget && sourceRect != null && localTargetRect != null) sourceRect.top - localTargetRect.top else 0f
                val inverseProgress = 1f - progress
                scaleX = startScale + (1f - startScale) * progress
                scaleY = startScale + (1f - startScale) * progress
                translationX = startX * inverseProgress
                translationY = startY * inverseProgress
                transformOrigin = TransformOrigin(0f, 0f)
            }
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.12f),
                        0.68f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.10f)
                    )
                )
            )
        }
    }
}

@Composable
private fun TitleRow(
    song: Song,
    isFavorite: Boolean,
    showThumbnail: Boolean,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    onArtistClick: () -> Unit = {},
    onThumbnailClick: () -> Unit = {},
    onCollapseRequest: () -> Unit = {},
    onHeaderDrag: (Float) -> Unit = {},
    onHeaderSpringBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    // rememberUpdatedState so the pointerInput(Unit) coroutine (launched once, on first
    // composition) always calls the latest callbacks, not whatever was passed on that first frame.
    val currentOnCollapseRequest by rememberUpdatedState(onCollapseRequest)
    val currentOnHeaderDrag by rememberUpdatedState(onHeaderDrag)
    val currentOnHeaderSpringBack by rememberUpdatedState(onHeaderSpringBack)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showThumbnail) {
                    Modifier.pointerInput(Unit) {
                        val collapseThresholdPx = with(density) { HEADER_COLLAPSE_DRAG_THRESHOLD.toPx() }
                        val flingVelocityPx = with(density) { HEADER_COLLAPSE_FLING_VELOCITY.toPx() }
                        var netDragPx = 0f
                        var velocityTracker = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = {
                                netDragPx = 0f
                                velocityTracker = VelocityTracker()
                            },
                            onDragCancel = {
                                netDragPx = 0f
                                currentOnHeaderSpringBack()
                            },
                            onDragEnd = {
                                val flingVelocity = velocityTracker.calculateVelocity().y
                                if (netDragPx > collapseThresholdPx || flingVelocity > flingVelocityPx) {
                                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                    currentOnCollapseRequest()
                                } else {
                                    currentOnHeaderSpringBack()
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                netDragPx += dragAmount
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                currentOnHeaderDrag(dragAmount)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showThumbnail) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = hapticClick(onThumbnailClick))
            ) {
                AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = if (showThumbnail) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.tapScale(onArtistClick)
            )
        }
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_heart,
            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
            tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onToggleFavorite
        )
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_ellipsis_vertical,
            contentDescription = "More",
            onClick = onShowMenu
        )
    }
}

// Lyric display items — sealed so Kotlin smart-cast works inside LazyColumn lambdas.
private sealed interface LyricDisplayItem {
    // Wraps a LyricLine, its position in the original syncedLyrics list, and its estimated vocal duration.
    data class Line(
        val lyricLine: LyricLine,
        val originalIndex: Int,
        val vocalDurationMs: Long
    ) : LyricDisplayItem

    // Sentinel for an instrumental gap between two consecutive lines or song intro.
    data class Gap(val fromMs: Long, val toMs: Long) : LyricDisplayItem
}

@Composable
private fun LyricsView(
    lyrics: String?,
    syncedLyrics: List<LyricLine>?,
    positionMs: Long,
    durationMs: Long = 0L,
    artist: String? = null,
    lyricsProvider: String? = null,
    isPlaying: Boolean,
    scrollState: ScrollState,
    listState: LazyListState,
    controlsVisible: Boolean,
    onInteraction: () -> Unit,
    // Seeks the player to that line's start timestamp; also surfaces controls briefly.
    onSeekToLine: (Long) -> Unit = {},
    // Called when user scrolls with delta (positive = scroll up/pull down, negative = scroll down)
    onScrollDelta: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val providerCredit = when (lyricsProvider) {
        "LRCLIB" -> "Lyrics provided by LRCLIB"
        "Embedded metadata" -> "Lyrics from file metadata"
        "Local file" -> "Lyrics from local file"
        else -> if (!lyrics.isNullOrBlank() || !syncedLyrics.isNullOrEmpty()) "Lyrics from file metadata" else null
    }

    // Animate the bottom padding so lyrics fill the full viewport when controls hide.
    val lyricBottomPadDp by animateDpAsState(
        targetValue = if (controlsVisible) 240.dp else 64.dp,
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = 400f
        ),
        label = "lyricBottomPad"
    )

    when {
        !syncedLyrics.isNullOrEmpty() -> {
            // Pre-build display list with comprehensive gap handling (intro, bridges, outro)
            val displayItems: List<LyricDisplayItem> = remember(syncedLyrics, durationMs) {
                buildList {
                    // 1. Song Intro Gap: if vocals begin > 2.5s into the track
                    val firstMs = syncedLyrics.first().timestampMs
                    if (firstMs >= 2_500L) {
                        add(LyricDisplayItem.Gap(fromMs = 0L, toMs = (firstMs - 120L).coerceAtLeast(0L)))
                    }

                    syncedLyrics.forEachIndexed { i, line ->
                        val nextMs = syncedLyrics.getOrNull(i + 1)?.timestampMs
                        val words = line.text.split(" ").filter { it.isNotBlank() }
                        if (nextMs != null) {
                            val lineInterval = (nextMs - line.timestampMs).coerceAtLeast(100L)
                            if (lineInterval >= 5_500L) {
                                // There is an instrumental break between lines (>= 5.5s)
                                val maxBreak = (lineInterval - 1_500L).coerceAtLeast(1_200L)
                                val vocalDuration = minOf(words.size * 280L + 600L, 4_200L)
                                    .coerceAtLeast(1_200L)
                                    .coerceAtMost(maxBreak)
                                add(LyricDisplayItem.Line(line, i, vocalDuration))

                                val gapStart = line.timestampMs + vocalDuration + 250L
                                val gapEnd = nextMs - 120L
                                if (gapEnd - gapStart >= 1_500L) {
                                    add(LyricDisplayItem.Gap(fromMs = gapStart, toMs = gapEnd))
                                }
                            } else {
                                // Golden ratio for vocal phrasing: ~84% of interval
                                val maxAllowed = maxOf(350L, lineInterval - 120L)
                                val minTarget = (words.size * 180L).coerceAtLeast(250L)
                                val rawCalculated = (lineInterval * 0.84f).toLong()
                                val vocalDuration = if (maxAllowed >= minTarget) {
                                    rawCalculated.coerceIn(minTarget, maxAllowed)
                                } else {
                                    maxAllowed
                                }
                                add(LyricDisplayItem.Line(line, i, vocalDuration))
                            }
                        } else {
                            // Last lyric line
                            val vocalMs = (words.size * 260L + 500L).coerceIn(1_800L, 4_500L)
                            add(LyricDisplayItem.Line(line, i, vocalMs))

                            // 3. Song Outro Gap: if song continues playing for > 3.0s after final lyric
                            if (durationMs > 0L) {
                                val outroStart = line.timestampMs + vocalMs + 300L
                                val outroEnd = durationMs - 500L
                                if (outroEnd - outroStart >= 2_500L) {
                                    add(LyricDisplayItem.Gap(fromMs = outroStart, toMs = outroEnd))
                                }
                            }
                        }
                    }
                }
            }

            // Lead compensation (+90ms) to compensate for audio DAC latency & vocal onset
            val effectivePositionMs = positionMs + 90L

            // Find current active item index in displayItems
            val activeDisplayIndex = remember(effectivePositionMs, displayItems) {
                displayItems.indexOfLast { item ->
                    when (item) {
                        is LyricDisplayItem.Line -> item.lyricLine.timestampMs <= effectivePositionMs
                        is LyricDisplayItem.Gap  -> item.fromMs <= effectivePositionMs
                    }
                }.coerceAtLeast(0)
            }
            val currentActiveItem = displayItems.getOrNull(activeDisplayIndex)

            // Original line index for surrounding line opacity calculation
            val activeLineIndex = remember(effectivePositionMs, syncedLyrics) {
                syncedLyrics.indexOfLast { it.timestampMs <= effectivePositionMs }.coerceAtLeast(0)
            }

            // ── Clean Natural Top Padding & Bottom Overflow ──────────────────────
            val density = LocalDensity.current
            val centerTopPad = 24.dp
            val activeOffsetPx = with(density) { 100.dp.roundToPx() }

            var userScrollToken by remember { mutableStateOf(0) }
            var userIsDragging by remember { mutableStateOf(false) }

            LaunchedEffect(userScrollToken) {
                if (userScrollToken > 0) {
                    userIsDragging = true
                    delay(3_500L)
                    userIsDragging = false
                }
            }

            LaunchedEffect(activeDisplayIndex, userIsDragging) {
                if (userIsDragging) return@LaunchedEffect
                listState.animateScrollToItem(
                    index = activeDisplayIndex,
                    scrollOffset = if (activeDisplayIndex > 0) -activeOffsetPx else 0
                )
            }

            val currentOnScrollDelta by rememberUpdatedState(onScrollDelta)
            val lyricsScrollHideConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (source == NestedScrollSource.UserInput && abs(available.y) > 0.5f) {
                            userScrollToken++
                            currentOnScrollDelta(available.y)
                        }
                        return Offset.Zero
                    }
                }
            }

            // Apple Music-style: left-aligned, bold SpaceGrotesk text with 38sp line height
            val lyricStyle = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Start
            )

            val fadeStart by animateFloatAsState(
                targetValue = if (controlsVisible) 0.48f else 0.85f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 400f
                ),
                label = "lyricsFadeStart"
            )
            val fadeEnd by animateFloatAsState(
                targetValue = if (controlsVisible) 0.65f else 0.98f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 400f
                ),
                label = "lyricsFadeEnd"
            )

            LazyColumn(
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = centerTopPad,
                    bottom = lyricBottomPadDp
                ),
                horizontalAlignment = Alignment.Start,
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(lyricsScrollHideConnection)
                    .clickable { onInteraction() }
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.04f to Color.Black.copy(alpha = 0.35f),
                                0.09f to Color.Black,
                                fadeStart to Color.Black,
                                ((fadeStart + fadeEnd) / 2f) to Color.Black.copy(alpha = 0.35f),
                                fadeEnd to Color.Transparent,
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                itemsIndexed(
                    items = displayItems,
                    key = { _, it -> when (it) {
                        is LyricDisplayItem.Line -> it.lyricLine.timestampMs
                        is LyricDisplayItem.Gap  -> "gap_${it.fromMs}"
                    }}
                ) { displayIdx, item ->
                    when (item) {
                        is LyricDisplayItem.Gap -> {
                            val isInGap = effectivePositionMs in item.fromMs..item.toMs
                            val dotsAlpha by animateFloatAsState(
                                targetValue = if (isInGap) 1.0f else 0.22f,
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                                label = "gapDotsAlpha"
                            )
                            val infiniteTransition = rememberInfiniteTransition(label = "dots")
                            val dot1Scale by infiniteTransition.animateFloat(
                                initialValue = 0.85f, targetValue = 1.35f,
                                animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "dot1Scale"
                            )
                            val dot1Alpha by infiniteTransition.animateFloat(
                                initialValue = 0.35f, targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "dot1Alpha"
                            )
                            val dot2Scale by infiniteTransition.animateFloat(
                                initialValue = 0.85f, targetValue = 1.35f,
                                animationSpec = infiniteRepeatable(tween(550, 180, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "dot2Scale"
                            )
                            val dot2Alpha by infiniteTransition.animateFloat(
                                initialValue = 0.35f, targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(tween(550, 180, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "dot2Alpha"
                            )
                            val dot3Scale by infiniteTransition.animateFloat(
                                initialValue = 0.85f, targetValue = 1.35f,
                                animationSpec = infiniteRepeatable(tween(550, 360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "dot3Scale"
                            )
                            val dot3Alpha by infiniteTransition.animateFloat(
                                initialValue = 0.35f, targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(tween(550, 360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "dot3Alpha"
                            )
                            val onSurface = MaterialTheme.colorScheme.onSurface
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        userIsDragging = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSeekToLine(item.fromMs)
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(displayIdx, 0)
                                        }
                                    }
                                    .graphicsLayer { alpha = dotsAlpha }
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .graphicsLayer {
                                            scaleX = if (isInGap) dot1Scale else 1f
                                            scaleY = if (isInGap) dot1Scale else 1f
                                            alpha = if (isInGap) dot1Alpha else 0.3f
                                        }
                                        .background(onSurface, CircleShape)
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .graphicsLayer {
                                            scaleX = if (isInGap) dot2Scale else 1f
                                            scaleY = if (isInGap) dot2Scale else 1f
                                            alpha = if (isInGap) dot2Alpha else 0.3f
                                        }
                                        .background(onSurface, CircleShape)
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .graphicsLayer {
                                            scaleX = if (isInGap) dot3Scale else 1f
                                            scaleY = if (isInGap) dot3Scale else 1f
                                            alpha = if (isInGap) dot3Alpha else 0.3f
                                        }
                                        .background(onSurface, CircleShape)
                                )
                            }
                        }

                        is LyricDisplayItem.Line -> {
                            val line = item.lyricLine
                            val index = item.originalIndex
                            val distanceFromActive = kotlin.math.abs(index - activeLineIndex)
                            val isActive = currentActiveItem is LyricDisplayItem.Line && currentActiveItem.originalIndex == index

                            val lineScale by animateFloatAsState(
                                targetValue = if (isActive) 1.025f else 0.965f,
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = 220f
                                ),
                                label = "lyricLineScale"
                            )

                            val lyricOpacity by animateFloatAsState(
                                targetValue = when {
                                    isActive -> 1.00f
                                    distanceFromActive == 1 -> 0.44f
                                    distanceFromActive == 2 -> 0.26f
                                    distanceFromActive == 3 -> 0.14f
                                    else -> 0.08f
                                },
                                animationSpec = spring(
                                    dampingRatio = 0.9f,
                                    stiffness = 300f
                                ),
                                label = "lyricLineOpacity"
                            )

                            val lineModifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    userIsDragging = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSeekToLine(line.timestampMs)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(displayIdx, 0)
                                    }
                                }
                                .graphicsLayer {
                                    alpha = lyricOpacity
                                    scaleX = lineScale
                                    scaleY = lineScale
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                }
                                .padding(horizontal = 24.dp, vertical = 13.dp)

                            if (isActive) {
                                ActiveLyricLine(
                                    item = item,
                                    positionMs = positionMs,
                                    isPlaying = isPlaying,
                                    lyricStyle = lyricStyle,
                                    modifier = lineModifier
                                )
                            } else {
                                Text(
                                    text = line.text,
                                    style = lyricStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start,
                                    modifier = lineModifier
                                )
                            }
                        }
                    }
                }

                // Apple Music-style credits footer at the end of lyrics
                item(key = "lyrics_credits_footer") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 48.dp)
                    ) {
                        if (!artist.isNullOrBlank()) {
                            Text(
                                text = "Written by $artist",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        if (!providerCredit.isNullOrBlank()) {
                            Text(
                                text = providerCredit,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }

        lyrics != null -> {
            LaunchedEffect(scrollState.isScrollInProgress) {
                if (scrollState.isScrollInProgress) onScrollDelta(-10f)
            }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .clickable { onInteraction() }
            ) {
                Spacer(Modifier.height(48.dp))
                Text(
                    text = lyrics,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(36.dp))
                if (!artist.isNullOrBlank()) {
                    Text(
                        text = "Written by $artist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (!providerCredit.isNullOrBlank()) {
                    Text(
                        text = providerCredit,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
                Spacer(Modifier.height(lyricBottomPadDp))
            }
        }

        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No lyrics available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActiveLyricLine(
    item: LyricDisplayItem.Line,
    positionMs: Long,
    isPlaying: Boolean,
    lyricStyle: TextStyle,
    modifier: Modifier
) {
    val line = item.lyricLine
    val words = remember(line.text) { line.text.split(" ").filter { it.isNotBlank() } }
    val wordWeights = remember(words) {
        val total = words.sumOf { it.length + 1 }.toFloat().coerceAtLeast(1f)
        var acc = 0f
        words.map { word ->
            val start = acc / total
            acc += word.length + 1
            start to (acc / total)
        }
    }

    val effectivePositionMs = positionMs + 90L
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(line.timestampMs, item.vocalDurationMs, positionMs, isPlaying) {
        val safeDuration = item.vocalDurationMs.coerceAtLeast(1L)
        val currentElapsed = (effectivePositionMs - line.timestampMs).coerceIn(0L, safeDuration)
        val initialProgress = (currentElapsed.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        progressAnim.snapTo(initialProgress)
        if (isPlaying && initialProgress < 1f) {
            val remainingMs = ((1f - initialProgress) * safeDuration).toLong().coerceAtLeast(50L)
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingMs.toInt(),
                    easing = LinearEasing
                )
            )
        }
    }

    val progress = progressAnim.value
    val onSurface = MaterialTheme.colorScheme.onSurface

    val annotatedString = buildAnnotatedString {
        words.forEachIndexed { wordIdx, word ->
            val (slotStart, slotEnd) = wordWeights.getOrElse(wordIdx) { 0f to 1f }
            val wordAlpha = when {
                progress >= slotEnd -> 1.0f
                progress >= slotStart -> {
                    val linearFraction = if (slotEnd > slotStart) {
                        ((progress - slotStart) / (slotEnd - slotStart)).coerceIn(0f, 1f)
                    } else 1f
                    val smoothFraction = linearFraction * linearFraction * (3f - 2f * linearFraction)
                    lerp(0.38f, 1.0f, smoothFraction)
                }
                else -> 0.38f
            }
            withStyle(SpanStyle(color = onSurface.copy(alpha = wordAlpha))) {
                append(word)
            }
            if (wordIdx < words.size - 1) append(" ")
        }
    }

    Text(
        text = annotatedString,
        style = lyricStyle,
        textAlign = TextAlign.Start,
        modifier = modifier
    )
}

@Composable
private fun NowPlayingBackdrop(song: Song) {
    val transition = rememberInfiniteTransition(label = "artworkAtmosphere")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "artworkDrift"
    )
    val density = LocalDensity.current
    val horizontalDrift = with(density) { 22.dp.toPx() }
    val verticalDrift = with(density) { 14.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141416))
    ) {
        AlbumArt(
            song = song,
            modifier = Modifier
                .fillMaxSize()
                .blur(72.dp)
                .graphicsLayer {
                    alpha = 0.92f
                    scaleX = 1.45f
                    scaleY = 1.45f
                    translationX = (drift - 0.5f) * horizontalDrift
                    translationY = (0.5f - drift) * verticalDrift
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.68f)
                        )
                    )
                )
        )
    }
}

// Sheet and dialog definitions extracted to NowPlayingSheets.kt


@Composable
private fun AppleMusicPillButton(
    iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isActive) onSurface.copy(alpha = 0.25f) else onSurface.copy(alpha = 0.10f)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (isActive) onSurface else onSurface.copy(alpha = 0.70f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// Scrubber + shuffle/transport/repeat row. Shared by all three views (Player renders
// it tightly grouped under the title with a trailing flex spacer; Queue/Lyrics render
// it as a fixed-spacing footer under their own list) so the controls themselves don't
// have to be duplicated even though their surrounding layout differs per view.
@Composable
private fun PlaybackControlsRow(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    isShuffleEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    repeatMode: Int,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        ScrubberControl(positionMs = positionMs, durationMs = durationMs, onSeek = onSeek)
        // Wider than a typical 16dp internal gap: Apple Music leaves noticeably more room
        // between the time readout and the transport row than any other gap in this view.
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffleEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onToggleShuffle
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportButton(
                    iconRes = R.drawable.lucide_ic_skip_back,
                    size = 56.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onSkipPrevious
                )
                Spacer(Modifier.width(20.dp))
                Crossfade(
                    targetState = isPlaying,
                    animationSpec = tween(durationMillis = 150, easing = SmoothEasing),
                    label = "playPause"
                ) { playing ->
                    TransportButton(
                        iconRes = if (playing) R.drawable.lucide_ic_pause else R.drawable.lucide_ic_play,
                        size = 72.dp,
                        tint = MaterialTheme.colorScheme.tertiary,
                        onClick = onTogglePlayPause,
                        accented = playing
                    )
                }
                Spacer(Modifier.width(20.dp))
                TransportButton(
                    iconRes = R.drawable.lucide_ic_skip_forward,
                    size = 56.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onSkipNext
                )
            }
            PressDepthIconButton(
                iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.lucide_ic_repeat_1 else R.drawable.lucide_ic_repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onCycleRepeatMode
            )
        }
    }
}

@Composable
private fun LyricsQueueToggleRow(activeView: NowPlayingView, onActiveViewChange: (NowPlayingView) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_mic_vocal,
            contentDescription = if (activeView == NowPlayingView.LYRICS) "Close lyrics" else "Lyrics",
            tint = if (activeView == NowPlayingView.LYRICS) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = {
                onActiveViewChange(if (activeView == NowPlayingView.LYRICS) NowPlayingView.PLAYER else NowPlayingView.LYRICS)
            }
        )
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_list,
            contentDescription = if (activeView == NowPlayingView.QUEUE) "Close queue" else "Queue",
            tint = if (activeView == NowPlayingView.QUEUE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = {
                onActiveViewChange(if (activeView == NowPlayingView.QUEUE) NowPlayingView.PLAYER else NowPlayingView.QUEUE)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrubberControl(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    var dragPositionMs by remember { mutableStateOf<Long?>(null) }
    var lastHapticTickMs by remember { mutableStateOf<Long?>(null) }
    val displayedPositionMs = dragPositionMs ?: positionMs
    val haptics = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = displayedPositionMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(0f)),
            onValueChange = { value ->
                val newPositionMs = value.toLong()
                val lastTick = lastHapticTickMs
                if (lastTick == null || abs(newPositionMs - lastTick) >= SEEK_HAPTIC_TICK_INTERVAL_MS) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticTickMs = newPositionMs
                }
                dragPositionMs = newPositionMs
            },
            onValueChangeFinished = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                dragPositionMs?.let(onSeek)
                dragPositionMs = null
                lastHapticTickMs = null
            },
            valueRange = 0f..durationMs.toFloat().coerceAtLeast(0f),
            track = { sliderState -> FaderTrack(sliderState) },
            thumb = { FaderThumb() }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(displayedPositionMs), style = readoutStyle())
            Text(formatTime(durationMs), style = readoutStyle())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaderTrack(sliderState: SliderState) {
    val grooveColor = MaterialTheme.colorScheme.outlineVariant
    val filledColor = MaterialTheme.colorScheme.secondary
    val range = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(1f)
    val fraction = ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(color = grooveColor.copy(alpha = 0.5f), cornerRadius = CornerRadius(2.dp.toPx()))
            var x = 0f
            val tick = 6.dp.toPx()
            while (x < size.width) {
                drawLine(grooveColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                x += tick
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(filledColor, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun FaderThumb() {
    Box(
        modifier = Modifier
            .size(width = 10.dp, height = 22.dp)
            .shadow(3.dp, RoundedCornerShape(3.dp))
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary)
                ),
                RoundedCornerShape(3.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
    )
}

@Composable
private fun readoutStyle() = MaterialTheme.typography.bodyMedium.copy(
    fontFamily = IbmPlexMonoFontFamily,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
