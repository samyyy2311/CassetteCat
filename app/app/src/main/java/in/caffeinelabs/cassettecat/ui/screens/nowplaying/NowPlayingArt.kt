package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.prefetchAlbumArt

private const val CAROUSEL_SNAP_MS = 380

@Composable
internal fun AlbumArtCarousel(
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
    val context = LocalContext.current
    LaunchedEffect(previousSong?.id, nextSong?.id) {
        launch { prefetchAlbumArt(context, previousSong) }
        launch { prefetchAlbumArt(context, nextSong) }
    }
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
internal fun AlbumArtCard(
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
                val (startScale, startX, startY) = if (sourceRect != null && localTargetRect != null && localTargetRect.width > 0f) {
                    Triple(
                        (sourceRect.width / localTargetRect.width).coerceIn(0.05f, 1f),
                        sourceRect.left - localTargetRect.left,
                        sourceRect.top - localTargetRect.top
                    )
                } else {
                    Triple(1f, 0f, 0f)
                }
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
internal fun NowPlayingBackdrop(song: Song) {
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
            .background(Color(0xFF0E0D10))
    ) {
        AlbumArt(
            song = song,
            modifier = Modifier
                .fillMaxSize()
                .blur(72.dp)
                .graphicsLayer {
                    alpha = 0.68f
                    scaleX = 2.2f
                    scaleY = 2.2f
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
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.62f),
                            Color.Black.copy(alpha = 0.86f)
                        )
                    )
                )
        )
    }
}
