package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.data.playback.adjustLyricsSync
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexSansFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal sealed interface LyricDisplayItem {
    data class Line(
        val lyricLine: LyricLine,
        val originalIndex: Int,
        val vocalDurationMs: Long
    ) : LyricDisplayItem

    data class Gap(val fromMs: Long, val toMs: Long) : LyricDisplayItem
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LyricsView(
    lyrics: String?,
    syncedLyrics: List<LyricLine>?,
    positionMs: Long,
    modifier: Modifier = Modifier,
    durationMs: Long = 0L,
    syncOffsetMs: Long = 0L,
    artist: String? = null,
    lyricsProvider: String? = null,
    isLoading: Boolean = false,
    isPlaying: Boolean,
    scrollState: ScrollState,
    listState: LazyListState,
    controlsVisible: Boolean,
    onInteraction: () -> Unit,
    selectionMode: Boolean = false,
    selectedIndices: Set<Int> = emptySet(),
    onToggleLineSelection: (Int) -> Unit = {},
    onStartSelection: (Int) -> Unit = {},
    onSeekToLine: (Long) -> Unit = {},
    onScrollDelta: (Float) -> Unit = {},
    onReturnToPlayer: () -> Unit = onInteraction
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())

    if (preferences.keepScreenOnLyrics) {
        val activity = context as? android.app.Activity
        DisposableEffect(activity) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val providerCredit = when (lyricsProvider) {
        "LRCLIB" -> "Lyrics provided by LRCLIB"
        "Embedded metadata" -> "Lyrics from file metadata"
        "Local file" -> "Lyrics from local file"
        else -> if (!lyrics.isNullOrBlank() || !syncedLyrics.isNullOrEmpty()) "Lyrics from file metadata" else null
    }

    val lyricBottomPadDp by animateDpAsState(
        targetValue = if (controlsVisible) 240.dp else 64.dp,
        animationSpec = tween(220, easing = SmoothEasing),
        label = "lyricBottomPad"
    )

    val effectiveSyncedLyrics = remember(syncedLyrics, syncOffsetMs) {
        if (syncedLyrics == null || syncOffsetMs == 0L) syncedLyrics else adjustLyricsSync(syncedLyrics, syncOffsetMs)
    }

    when {
        !effectiveSyncedLyrics.isNullOrEmpty() -> {
            val displayItems: List<LyricDisplayItem> = remember(effectiveSyncedLyrics, durationMs) {
                buildList {
                    val firstMs = effectiveSyncedLyrics.first().timestampMs
                    if (firstMs >= 2_500L) {
                        add(LyricDisplayItem.Gap(fromMs = 0L, toMs = (firstMs - 120L).coerceAtLeast(0L)))
                    }

                    effectiveSyncedLyrics.forEachIndexed { i, line ->
                        val nextMs = effectiveSyncedLyrics.getOrNull(i + 1)?.timestampMs
                        val words = line.text.split(" ").filter { it.isNotBlank() }
                        if (nextMs != null) {
                            val lineInterval = (nextMs - line.timestampMs).coerceAtLeast(100L)
                            if (lineInterval >= 5_500L) {
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
                            val vocalMs = (words.size * 260L + 500L).coerceIn(1_800L, 4_500L)
                            add(LyricDisplayItem.Line(line, i, vocalMs))

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

            val effectivePositionMs = positionMs + 90L

            val activeDisplayIndex by remember(displayItems, effectivePositionMs) {
                derivedStateOf {
                    displayItems.indexOfLast { item ->
                        when (item) {
                            is LyricDisplayItem.Line -> item.lyricLine.timestampMs <= effectivePositionMs
                            is LyricDisplayItem.Gap  -> item.fromMs <= effectivePositionMs
                        }
                    }.coerceAtLeast(0)
                }
            }
            val currentActiveItem = displayItems.getOrNull(activeDisplayIndex)

            val activeLineIndex by remember(effectiveSyncedLyrics, effectivePositionMs) {
                derivedStateOf {
                    effectiveSyncedLyrics.indexOfLast { it.timestampMs <= effectivePositionMs }.coerceAtLeast(0)
                }
            }

            val density = LocalDensity.current
            val centerTopPad = 24.dp
            val activeOffsetPx = with(density) { 100.dp.roundToPx() }

            var userScrollToken by remember { mutableIntStateOf(0) }
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

            val lyricsTextAlign = if (preferences.lyricsAlignment == `in`.caffeinelabs.cassettecat.data.settings.LyricsAlignment.CENTER) TextAlign.Center else TextAlign.Start
            val lyricsHorizontalAlignment = if (preferences.lyricsAlignment == `in`.caffeinelabs.cassettecat.data.settings.LyricsAlignment.CENTER) Alignment.CenterHorizontally else Alignment.Start
            val fontScale = preferences.lyricsFontSize.scaleMultiplier
            val lyricStyle = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (28 * fontScale).sp,
                lineHeight = (38 * fontScale).sp,
                letterSpacing = (-0.3).sp,
                textAlign = lyricsTextAlign
            )

            val fadeStart by animateFloatAsState(
                targetValue = if (controlsVisible) 0.48f else 0.85f,
                animationSpec = tween(220, easing = SmoothEasing),
                label = "lyricsFadeStart"
            )
            val fadeEnd by animateFloatAsState(
                targetValue = if (controlsVisible) 0.65f else 0.98f,
                animationSpec = tween(220, easing = SmoothEasing),
                label = "lyricsFadeEnd"
            )

            val gradientBrush = remember(fadeStart, fadeEnd) {
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.04f to Color.Black.copy(alpha = 0.35f),
                    0.09f to Color.Black,
                    fadeStart to Color.Black,
                    ((fadeStart + fadeEnd) / 2f) to Color.Black.copy(alpha = 0.35f),
                    fadeEnd to Color.Transparent,
                    1.0f to Color.Transparent
                )
            }

            LazyColumn(
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = centerTopPad,
                    bottom = lyricBottomPadDp
                ),
                horizontalAlignment = lyricsHorizontalAlignment,
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(lyricsScrollHideConnection)
                    .clickable { onInteraction() }
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = gradientBrush,
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                itemsIndexed(
                    items = displayItems,
                    key = { idx, it -> when (it) {
                        is LyricDisplayItem.Line -> "line_${it.lyricLine.timestampMs}_${idx}"
                        is LyricDisplayItem.Gap  -> "gap_${it.fromMs}_${idx}"
                    }}
                ) { displayIdx, item ->
                    when (item) {
                        is LyricDisplayItem.Gap -> {
                            val isInGap = effectivePositionMs in item.fromMs..item.toMs
                            GapItemView(
                                isInGap = isInGap,
                                onClick = {
                                    userIsDragging = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSeekToLine(item.fromMs)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(displayIdx, 0)
                                    }
                                }
                            )
                        }

                        is LyricDisplayItem.Line -> {
                            val line = item.lyricLine
                            val index = item.originalIndex
                            val isSelected = selectedIndices.contains(index)
                            val distanceFromActive = abs(index - activeLineIndex)
                            val isActive = !selectionMode && currentActiveItem is LyricDisplayItem.Line && currentActiveItem.originalIndex == index

                            val lineScale by animateFloatAsState(
                                targetValue = if (isActive || isSelected) 1.025f else 0.965f,
                                animationSpec = tween(220, easing = SmoothEasing),
                                label = "lyricLineScale"
                            )

                            val lyricOpacity by animateFloatAsState(
                                targetValue = when {
                                    selectionMode -> if (isSelected) 1.00f else 0.35f
                                    isActive -> 1.00f
                                    distanceFromActive == 1 -> 0.44f
                                    distanceFromActive == 2 -> 0.26f
                                    distanceFromActive == 3 -> 0.14f
                                    else -> 0.08f
                                },
                                animationSpec = tween(220, easing = SmoothEasing),
                                label = "lyricLineOpacity"
                            )

                            val lineModifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (selectionMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onToggleLineSelection(index)
                                        } else {
                                            userIsDragging = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSeekToLine(line.timestampMs)
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(displayIdx, 0)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectionMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onStartSelection(index)
                                        }
                                    }
                                )
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
                                    textAlign = lyricsTextAlign,
                                    modifier = lineModifier
                                )
                            }
                        }
                    }
                }

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
                        fontFamily = IbmPlexSansFontFamily,
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

        isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Loading lyrics...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> {
            InstrumentalWaveformView(
                artist = artist,
                isPlaying = isPlaying,
                onReturnToPlayer = onReturnToPlayer,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun GapItemView(
    isInGap: Boolean,
    onClick: () -> Unit
) {
    val dotsAlpha by animateFloatAsState(
        targetValue = if (isInGap) 1.0f else 0.22f,
        animationSpec = tween(220, easing = SmoothEasing),
        label = "gapDotsAlpha"
    )
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer { alpha = dotsAlpha }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (isInGap) {
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

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = dot1Scale
                        scaleY = dot1Scale
                        alpha = dot1Alpha
                    }
                    .background(onSurface, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = dot2Scale
                        scaleY = dot2Scale
                        alpha = dot2Alpha
                    }
                    .background(onSurface, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = dot3Scale
                        scaleY = dot3Scale
                        alpha = dot3Alpha
                    }
                    .background(onSurface, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = 0.3f }
                    .background(onSurface, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = 0.3f }
                    .background(onSurface, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = 0.3f }
                    .background(onSurface, CircleShape)
            )
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

    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(line.timestampMs, item.vocalDurationMs, isPlaying) {
        val effectivePositionMs = positionMs + 90L
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
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())
    val activeWordColor = if (preferences.lyricsActiveStyle == `in`.caffeinelabs.cassettecat.data.settings.LyricsActiveStyle.ACCENT_GLOW) {
        MaterialTheme.colorScheme.tertiary
    } else {
        onSurface
    }

    val dimColor = onSurface.copy(alpha = 0.38f)
    val annotatedString = buildAnnotatedString {
        words.forEachIndexed { wordIdx, word ->
            val (slotStart, slotEnd) = wordWeights.getOrElse(wordIdx) { 0f to 1f }
            val revealFraction = when {
                progress >= slotEnd -> 1f
                progress >= slotStart -> {
                    val linearFraction = if (slotEnd > slotStart) {
                        ((progress - slotStart) / (slotEnd - slotStart)).coerceIn(0f, 1f)
                    } else 1f
                    linearFraction * linearFraction * (3f - 2f * linearFraction)
                }
                else -> 0f
            }
            val color = lerp(dimColor, activeWordColor, revealFraction)
            withStyle(SpanStyle(color = color)) {
                append(word)
            }
            if (wordIdx < words.size - 1) append(" ")
        }
    }

    Text(
        text = annotatedString,
        style = lyricStyle,
        textAlign = lyricStyle.textAlign,
        modifier = modifier
    )
}

@Composable
internal fun LyricsSyncOffsetPill(
    syncOffsetMs: Long,
    onAdjust: (Long) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .clickable { onAdjust(-500L) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "-0.5s",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .then(if (syncOffsetMs != 0L) Modifier.clickable(onClick = onReset) else Modifier)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_clock),
                contentDescription = null,
                tint = if (syncOffsetMs != 0L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (syncOffsetMs != 0L) "Sync %+.1fs".format(Locale.US, syncOffsetMs / 1000f) else "In sync",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = if (syncOffsetMs != 0L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (syncOffsetMs != 0L) {
                Text(
                    "(reset)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .clickable { onAdjust(500L) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+0.5s",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun FloatingLyricSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onCancel)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_x),
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = if (selectedCount == 0) "Select up to 5 lines" else "$selectedCount selected",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selectedCount > 0) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .then(
                    if (selectedCount > 0) Modifier.tapScale(onClick = onShare)
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_share_2),
                    contentDescription = null,
                    tint = if (selectedCount > 0) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Share Card",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedCount > 0) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun InstrumentalWaveformView(
    artist: String?,
    isPlaying: Boolean,
    onReturnToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveformPulse")

    val bar1 by transition.animateFloat(
        initialValue = 0.25f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(580, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.40f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.20f, targetValue = 0.70f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b3"
    )
    val bar4 by transition.animateFloat(
        initialValue = 0.50f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b4"
    )
    val bar5 by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.60f,
        animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b5"
    )
    val bar6 by transition.animateFloat(
        initialValue = 0.35f, targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(490, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b6"
    )
    val bar7 by transition.animateFloat(
        initialValue = 0.20f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(540, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b7"
    )

    val bars = listOf(bar1, bar2, bar3, bar4, bar5, bar6, bar7)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Equalizer waveform bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(48.dp)
        ) {
            bars.forEach { amplitude ->
                val barHeight = if (isPlaying) (amplitude * 44f).coerceAtLeast(6f).dp else 8.dp
                Box(
                    modifier = Modifier
                        .width(4.5.dp)
                        .height(barHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Title
        Text(
            text = "Instrumental",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        // Subtitle
        Text(
            text = if (!artist.isNullOrBlank()) "Composed by $artist" else "This track has no lyrics",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = IbmPlexSansFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Return to Album Art Pill Button
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .tapScale(onClick = onReturnToPlayer)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_disc_3),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "View Album Art",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
