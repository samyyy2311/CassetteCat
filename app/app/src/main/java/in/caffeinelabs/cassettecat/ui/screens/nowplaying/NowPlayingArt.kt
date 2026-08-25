package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.NowPlayingBackdropStyle
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
import `in`.caffeinelabs.cassettecat.ui.components.prefetchAlbumArt
import `in`.caffeinelabs.cassettecat.ui.theme.ArtworkAtmospherePalette
import `in`.caffeinelabs.cassettecat.ui.theme.extractArtworkAtmospherePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CAROUSEL_SNAP_MS = 380
private const val SWIPE_UP_TRIGGER_DISTANCE = 80f

@Composable
internal fun AlbumArtCarousel(
    currentSong: Song,
    previousSong: Song?,
    nextSong: Song?,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    collapsedArtRect: State<Rect?>?,
    expandFraction: Float,
    modifier: Modifier = Modifier,
    onSwipeUp: (() -> Unit)? = null,
    isPlaying: Boolean = true
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
                onSwipeUp = if (isCurrentSong) onSwipeUp else null,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun AlbumArtCard(
    song: Song,
    modifier: Modifier = Modifier,
    collapsedArtRect: State<Rect?>? = null,
    expandFraction: Float = 1f,
    onSwipeUp: (() -> Unit)? = null,
    isPlaying: Boolean = true
) {
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val cornerRadius = preferences.albumArtCornerRadiusDp.dp
    val haptics = LocalHapticFeedback.current

    var expandedArtRect by remember(song.id) { mutableStateOf<Rect?>(null) }
    val targetRect = expandedArtRect
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                if (expandedArtRect != bounds) expandedArtRect = bounds
            }
            .pointerInput(preferences.swipeUpLyricsEnabled) {
                if (preferences.swipeUpLyricsEnabled && onSwipeUp != null) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDragY = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                if (totalDragY < -SWIPE_UP_TRIGGER_DISTANCE) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeUp.invoke()
                                }
                                break
                            }
                            totalDragY += change.positionChange().y
                            if (totalDragY < -SWIPE_UP_TRIGGER_DISTANCE) change.consume()
                        }
                    }
                }
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
        Box(
            Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = Color.Black.copy(alpha = 0.45f),
                    spotColor = Color.Black.copy(alpha = 0.70f)
                )
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            if (song.source == MusicSource.Radio) {
                var artwork by remember(song.id) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(song.id) { artwork = loadSongArtwork(context, song) }
                val loadedArtwork = artwork
                if (loadedArtwork != null) {
                    Image(
                        bitmap = loadedArtwork.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    RotatingVinylPlaceholder(isPlaying = isPlaying, modifier = Modifier.fillMaxSize())
                }
            } else {
                AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.12f),
                        0.68f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.45f)
                    )
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        RoundedCornerShape(cornerRadius)
                    )
            )
        }
    }
}

private const val VINYL_ROTATION_MS = 6000

@Composable
private fun RotatingVinylPlaceholder(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = VINYL_ROTATION_MS, easing = LinearEasing)
                )
            }
        }
    }
    val needleAngle by animateFloatAsState(
        targetValue = if (isPlaying) 0f else -30f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "needleLift"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        VinylDisc(
            modifier = Modifier
                .fillMaxSize(0.66f)
                .aspectRatio(1f)
                .graphicsLayer { rotationZ = rotation.value % 360f }
        )
        Tonearm(
            liftAngleDeg = needleAngle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxSize(0.44f)
                .aspectRatio(1f)
        )
    }
}

@Composable
private fun VinylDisc(modifier: Modifier = Modifier) {
    val labelColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color(0xFF141414), radius = radius, center = center)
        var grooveRadius = radius * 0.94f
        while (grooveRadius > radius * 0.42f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = grooveRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            grooveRadius -= radius * 0.045f
        }
        drawCircle(color = labelColor, radius = radius * 0.34f, center = center)
        drawCircle(color = Color(0xFF141414), radius = radius * 0.05f, center = center)
    }
}

@Composable
private fun Tonearm(liftAngleDeg: Float, modifier: Modifier = Modifier) {
    val armColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val pivot = Offset(size.width * 0.82f, size.height * 0.10f)
        val restTip = Offset(size.width * 0.28f, size.height * 0.80f)
        rotate(degrees = liftAngleDeg, pivot = pivot) {
            drawLine(
                color = armColor,
                start = pivot,
                end = restTip,
                strokeWidth = size.minDimension * 0.05f,
                cap = StrokeCap.Round
            )
            drawCircle(color = armColor, radius = size.minDimension * 0.09f, center = pivot)
            drawCircle(color = armColor, radius = size.minDimension * 0.045f, center = restTip)
        }
    }
}

@Composable
internal fun NowPlayingBackdrop(song: Song) {
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val backdropStyle = preferences.nowPlayingBackdropStyle

    var palette by remember(song.id) { mutableStateOf<ArtworkAtmospherePalette?>(null) }

    LaunchedEffect(song.id) {
        val bitmap = withContext(Dispatchers.IO) { loadSongArtwork(context, song) }
        if (bitmap != null) {
            val extracted = withContext(Dispatchers.Default) { extractArtworkAtmospherePalette(bitmap) }
            palette = extracted
        } else {
            palette = null
        }
    }

    val topColorTarget = palette?.topColor ?: Color(0xFF1E1E24)
    val centerColorTarget = palette?.centerColor ?: Color(0xFF141418)
    val accentColorTarget = palette?.accentColor ?: Color(0xFF282832)
    val darkBaseTarget = palette?.darkBase ?: Color(0xFF08080A)

    val animatedTopColor by animateColorAsState(
        targetValue = topColorTarget,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "ambientTopColor"
    )
    val animatedCenterColor by animateColorAsState(
        targetValue = centerColorTarget,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "ambientCenterColor"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = accentColorTarget,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "ambientAccentColor"
    )
    val animatedDarkBase by animateColorAsState(
        targetValue = darkBaseTarget,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "ambientDarkBase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(animatedDarkBase)
    ) {
        when (if (preferences.showNowPlayingBlur) backdropStyle else NowPlayingBackdropStyle.OLED_BLACK) {
            NowPlayingBackdropStyle.OLED_BLACK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF000000))
                )
            }
            NowPlayingBackdropStyle.AMBIENT_GLOW,
            NowPlayingBackdropStyle.LIQUID_GRADIENT -> {
                val transition = rememberInfiniteTransition(label = "ambientAtmosphere")
                val pulse by transition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8_000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ambientPulse"
                )

                // Layer 1: Velvet Atmospheric Base Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to animatedTopColor.copy(alpha = 0.50f),
                                0.36f to animatedCenterColor.copy(alpha = 0.32f),
                                0.72f to animatedDarkBase,
                                1.00f to animatedDarkBase
                            )
                        )
                )

                // Layer 2: 3-Node Seamless Atmospheric Mesh Glow
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = pulse }
                ) {
                    val w = size.width
                    val h = size.height

                    // Main upper aura directly behind album art
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedTopColor.copy(alpha = 0.55f),
                                animatedTopColor.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.50f, h * 0.28f),
                            radius = w * 1.05f
                        ),
                        center = Offset(w * 0.50f, h * 0.28f),
                        radius = w * 1.05f
                    )

                    // Secondary harmonic tone off-center right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedCenterColor.copy(alpha = 0.40f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.82f, h * 0.50f),
                            radius = w * 0.90f
                        ),
                        center = Offset(w * 0.82f, h * 0.50f),
                        radius = w * 0.90f
                    )

                    // Tertiary accent tone off-center left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedAccentColor.copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.18f, h * 0.42f),
                            radius = w * 0.80f
                        ),
                        center = Offset(w * 0.18f, h * 0.42f),
                        radius = w * 0.80f
                    )
                }

                // Layer 3: Top Status-Bar Scrim (Clean, legible system icons)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Black.copy(alpha = 0.36f),
                                0.15f to Color.Transparent
                            )
                        )
                )

                // Layer 4: Bottom Transport Contrast Scrim (Deep contrast for controls & track details)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.46f to Color.Transparent,
                                0.72f to Color.Black.copy(alpha = 0.35f),
                                1.00f to Color.Black.copy(alpha = 0.88f)
                            )
                        )
                )
            }
        }
    }
}
