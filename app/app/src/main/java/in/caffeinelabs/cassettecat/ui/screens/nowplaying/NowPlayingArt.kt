package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import `in`.caffeinelabs.cassettecat.ui.util.LocalAppPreferences
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
                AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
            }
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
        targetValue = if (isPlaying) 0f else -22f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "needleLift"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0F0F12)),
        contentAlignment = Alignment.Center
    ) {
        // 1. Turntable Platter Deck Base
        TurntablePlatter(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .aspectRatio(1f)
        )

        // 2. Spinning Vinyl Disc with Grooves and Dynamic Light Reflections
        VinylDisc(
            modifier = Modifier
                .fillMaxSize(0.74f)
                .aspectRatio(1f)
                .graphicsLayer { rotationZ = rotation.value % 360f }
        )

        // 3. Full-Deck Engineered Tonearm with Armrest Post & Needle on Grooves
        Tonearm(
            liftAngleDeg = needleAngle,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun TurntablePlatter(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Platter Outer Machined Bevel Rim
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    Color(0xFF282830),
                    Color(0xFF1E1E22),
                    Color(0xFF383842),
                    Color(0xFF1E1E22),
                    Color(0xFF282830)
                ),
                center = center
            ),
            radius = radius,
            center = center
        )

        // Strobe calibration ring
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = radius * 0.98f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Anti-Static Rubber Platter Mat
        drawCircle(
            color = Color(0xFF131316),
            radius = radius * 0.94f,
            center = center
        )

        // Concentric Mat Rings
        var matRing = radius * 0.88f
        while (matRing > radius * 0.38f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.035f),
                radius = matRing,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            matRing -= radius * 0.12f
        }
    }
}

@Composable
private fun VinylDisc(modifier: Modifier = Modifier) {
    val labelColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Deep Glossy Black Vinyl Base
        drawCircle(color = Color(0xFF0C0C0E), radius = radius, center = center)

        // Outer Beveled Edge
        drawCircle(
            color = Color.White.copy(alpha = 0.12f),
            radius = radius * 0.99f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Dynamic Dual Light Reflections (Specular Sheen Cones)
        drawCircle(
            brush = Brush.sweepGradient(
                0.00f to Color.White.copy(alpha = 0.00f),
                0.10f to Color.White.copy(alpha = 0.07f),
                0.16f to Color.White.copy(alpha = 0.14f),
                0.22f to Color.White.copy(alpha = 0.07f),
                0.32f to Color.White.copy(alpha = 0.00f),
                0.60f to Color.White.copy(alpha = 0.00f),
                0.66f to Color.White.copy(alpha = 0.07f),
                0.72f to Color.White.copy(alpha = 0.14f),
                0.78f to Color.White.copy(alpha = 0.07f),
                0.88f to Color.White.copy(alpha = 0.00f),
                1.00f to Color.White.copy(alpha = 0.00f)
            ),
            radius = radius * 0.96f,
            center = center
        )

        // Realistic High-Density Groove Bands
        var groove = radius * 0.94f
        val innerGrooveLimit = radius * 0.44f
        var step = 0
        while (groove > innerGrooveLimit) {
            val alpha = if (step % 4 == 0) 0.09f else 0.04f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = groove,
                center = center,
                style = Stroke(width = 0.75.dp.toPx())
            )
            groove -= radius * 0.032f
            step++
        }

        // Dead Wax / Run-out Groove Zone
        drawCircle(
            color = Color(0xFF16161A),
            radius = radius * 0.44f,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = radius * 0.40f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Center Album Label
        drawCircle(
            color = labelColor,
            radius = radius * 0.35f,
            center = center
        )
        // Label Concentric Micro-rings
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = radius * 0.30f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.20f),
            radius = radius * 0.20f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Spindle Hole & Center Metallic Brass Ring
        drawCircle(color = Color(0xFFC4A052), radius = radius * 0.07f, center = center)
        drawCircle(color = Color(0xFF0C0C0E), radius = radius * 0.045f, center = center)
    }
}

@Composable
private fun Tonearm(liftAngleDeg: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pivot = Offset(w * 0.84f, h * 0.16f)
        val playingTip = Offset(w * 0.68f, h * 0.62f)
        val armRestPos = Offset(w * 0.85f, h * 0.66f)

        // Fixed Armrest Post on the turntable deck
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 5.dp.toPx(),
            center = armRestPos
        )
        drawCircle(
            color = Color(0xFF64748B),
            radius = 3.dp.toPx(),
            center = armRestPos
        )
        drawLine(
            color = Color(0xFF94A3B8),
            start = armRestPos + Offset(-4.dp.toPx(), 0f),
            end = armRestPos + Offset(4.dp.toPx(), 0f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Animated Tonearm Assembly
        rotate(degrees = liftAngleDeg, pivot = pivot) {
            // Shadow behind the tonearm
            drawLine(
                color = Color.Black.copy(alpha = 0.45f),
                start = pivot + Offset(3.dp.toPx(), 3.dp.toPx()),
                end = playingTip + Offset(3.dp.toPx(), 3.dp.toPx()),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // S-Curve Metallic Arm Tube in Brushed Chrome
            val armPath = Path().apply {
                moveTo(pivot.x, pivot.y)
                cubicTo(
                    pivot.x - w * 0.08f, pivot.y + h * 0.18f,
                    playingTip.x + w * 0.08f, playingTip.y - h * 0.18f,
                    playingTip.x, playingTip.y
                )
            }
            drawPath(
                path = armPath,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFF1F5F9),
                        Color(0xFF94A3B8),
                        Color(0xFFE2E8F0),
                        Color(0xFF64748B)
                    ),
                    start = pivot,
                    end = playingTip
                ),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Cartridge / Headshell at the needle tip (angled tangentially along the groove)
            val headshellPath = Path().apply {
                moveTo(playingTip.x - 7.dp.toPx(), playingTip.y - 10.dp.toPx())
                lineTo(playingTip.x + 5.dp.toPx(), playingTip.y - 6.dp.toPx())
                lineTo(playingTip.x + 2.dp.toPx(), playingTip.y + 12.dp.toPx())
                lineTo(playingTip.x - 10.dp.toPx(), playingTip.y + 8.dp.toPx())
                close()
            }
            drawPath(path = headshellPath, color = Color(0xFF1E293B))

            // Red Stylus Cartridge Accent Needle Point
            drawCircle(
                color = Color(0xFFEF4444),
                radius = 2.5.dp.toPx(),
                center = playingTip + Offset(-3.dp.toPx(), 4.dp.toPx())
            )

            // Gimbal Pivot Base Assembly
            drawCircle(
                color = Color(0xFF1E293B),
                radius = 16.dp.toPx(),
                center = pivot
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFE2E8F0), Color(0xFF475569)),
                    center = pivot,
                    radius = 12.dp.toPx()
                ),
                radius = 12.dp.toPx(),
                center = pivot
            )
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 5.dp.toPx(),
                center = pivot
            )

            // Rear Counterweight Cylindrical Ring
            val counterweightCenter = pivot + Offset(11.dp.toPx(), -11.dp.toPx())
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF94A3B8), Color(0xFF334155), Color(0xFF94A3B8)),
                    start = counterweightCenter - Offset(8.dp.toPx(), 8.dp.toPx()),
                    end = counterweightCenter + Offset(8.dp.toPx(), 8.dp.toPx())
                ),
                radius = 9.dp.toPx(),
                center = counterweightCenter
            )
        }
    }
}

@Composable
internal fun NowPlayingBackdrop(song: Song) {
    val context = LocalContext.current
    val preferences = LocalAppPreferences.current
    val backdropStyle = preferences.nowPlayingBackdropStyle

    var palette by remember { mutableStateOf<ArtworkAtmospherePalette?>(null) }

    LaunchedEffect(song.id) {
        val bitmap = withContext(Dispatchers.IO) { loadSongArtwork(context, song) }
        if (bitmap != null) {
            val extracted = withContext(Dispatchers.Default) { extractArtworkAtmospherePalette(bitmap) }
            palette = extracted
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
            NowPlayingBackdropStyle.ATMOSPHERE_BLUR -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = song,
                        animationSpec = tween(600, easing = FastOutSlowInEasing),
                        label = "atmosphereBlurCrossfade"
                    ) { targetSong ->
                        AlbumArt(
                            song = targetSong,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(50.dp)
                                .graphicsLayer { scaleX = 1.25f; scaleY = 1.25f }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }
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
