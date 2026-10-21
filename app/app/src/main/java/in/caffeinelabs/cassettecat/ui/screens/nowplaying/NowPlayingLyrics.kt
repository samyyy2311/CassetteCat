package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.animate
import androidx.compose.animation.shrinkVertically
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.caffeinelabs.cassettecat.data.playback.AudioWaveformHolder
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.data.playback.adjustLyricsSync
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.LyricsFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexSansFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.MonocraftFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SilkscreenFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.VT323FontFamily
import `in`.caffeinelabs.cassettecat.ui.util.LocalAppPreferences
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
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
    onSearchLyrics: () -> Unit = {},
    onReturnToPlayer: () -> Unit = onInteraction
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())

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
        "LRCLIB" -> stringResource(AppR.string.lyrics_provider_lrclib)
        "Embedded metadata" -> stringResource(AppR.string.lyrics_provider_file_metadata)
        "Local file" -> stringResource(AppR.string.lyrics_provider_local_file)
        else -> if (!lyrics.isNullOrBlank() || !syncedLyrics.isNullOrEmpty()) {
            stringResource(AppR.string.lyrics_provider_file_metadata)
        } else {
            null
        }
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
                    if (firstMs >= 4_500L) {
                        add(LyricDisplayItem.Gap(fromMs = 0L, toMs = (firstMs - 150L).coerceAtLeast(0L)))
                    }

                    effectiveSyncedLyrics.forEachIndexed { i, line ->
                        val nextMs = effectiveSyncedLyrics.getOrNull(i + 1)?.timestampMs
                        val words = line.text.split(" ").filter { it.isNotBlank() }
                        // How long a line is sung is only a guess, so it's generous: slow singers hold words, and a
                        // break shown while they're still singing is worse than one shown a little late.
                        val sungMs = (words.size * 500L + 1_000L).coerceIn(2_500L, 8_000L)
                        if (nextMs != null) {
                            val lineInterval = (nextMs - line.timestampMs).coerceAtLeast(100L)
                            if (lineInterval >= sungMs + 5_000L) {
                                add(LyricDisplayItem.Line(line, i, sungMs))

                                val gapStart = line.timestampMs + sungMs
                                val gapEnd = nextMs - 150L
                                if (gapEnd - gapStart >= 4_000L) {
                                    add(LyricDisplayItem.Gap(fromMs = gapStart, toMs = gapEnd))
                                }
                            } else {
                                val maxAllowed = maxOf(400L, lineInterval - 120L)
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
                            add(LyricDisplayItem.Line(line, i, sungMs))

                            if (durationMs > 0L) {
                                val outroStart = line.timestampMs + sungMs
                                val outroEnd = durationMs - 500L
                                if (outroEnd - outroStart >= 6_000L) {
                                    add(LyricDisplayItem.Gap(fromMs = outroStart, toMs = outroEnd))
                                }
                            }
                        }
                    }
                }
            }

            val effectivePositionMs = positionMs

            val activeDisplayIndex by remember(displayItems, effectivePositionMs) {
                derivedStateOf {
                    displayItems.indexOfLast { item ->
                        when (item) {
                            is LyricDisplayItem.Line -> item.lyricLine.timestampMs <= effectivePositionMs
                            is LyricDisplayItem.Gap -> item.fromMs <= effectivePositionMs
                        }
                    }.coerceAtLeast(0)
                }
            }
            val currentActiveItem = displayItems.getOrNull(activeDisplayIndex)

            val activeLineIndex by remember(effectiveSyncedLyrics, effectivePositionMs) {
                derivedStateOf {
                    // -1 during the intro, so the first line reads as the next one rather than a past one.
                    effectiveSyncedLyrics.indexOfLast { it.timestampMs <= effectivePositionMs }
                }
            }

            // Every line, the first one included, is held this far down while it's sung.
            val activeLineTop = 124.dp
            // Room below the lyrics for the last lines to reach that height too, so the credits only come into view
            // once the song's lines are done.
            val density = LocalDensity.current
            var listHeightPx by remember { mutableIntStateOf(0) }
            val endRoom = with(density) { (listHeightPx * 0.7f).toDp() }

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
                val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeDisplayIndex }
                if (target == null) {
                    listState.animateScrollToItem(activeDisplayIndex)
                } else {
                    // An eased glide to the next line; the default spring snaps there. Each frame covers its share of
                    // what's left, measured again, so a break collapsing on the way doesn't leave the line short.
                    listState.scroll {
                        var covered = 0f
                        animate(0f, 1f, animationSpec = tween(700, easing = FastOutSlowInEasing)) { eased, _ ->
                            val offset = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == activeDisplayIndex }?.offset ?: return@animate
                            if (covered < 1f) scrollBy(offset * (eased - covered) / (1f - covered))
                            covered = eased
                        }
                    }
                }
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

            val lyricsTextAlign = if (preferences.lyricsAlignment == `in`.caffeinelabs.cassettecat.data.settings.LyricsAlignment.CENTER) {
                TextAlign.Center
            } else {
                TextAlign.Start
            }
            val lyricsHorizontalAlignment = if (preferences.lyricsAlignment == `in`.caffeinelabs.cassettecat.data.settings.LyricsAlignment.CENTER) {
                Alignment.CenterHorizontally
            } else {
                Alignment.Start
            }
            val fontScale = preferences.lyricsFontSize.scaleMultiplier
            val lyricsFont = when (preferences.lyricsFontFamily) {
                LyricsFontFamily.SPACE_GROTESK -> SpaceGroteskFontFamily
                LyricsFontFamily.IBM_PLEX_SANS -> IbmPlexSansFontFamily
                LyricsFontFamily.IBM_PLEX_MONO -> IbmPlexMonoFontFamily
                LyricsFontFamily.SILKSCREEN -> SilkscreenFontFamily
                LyricsFontFamily.VT323 -> VT323FontFamily
                LyricsFontFamily.MONOCRAFT -> MonocraftFontFamily
                LyricsFontFamily.SYSTEM_DEFAULT -> FontFamily.SansSerif
                LyricsFontFamily.SYSTEM_SERIF -> FontFamily.Serif
                LyricsFontFamily.SYSTEM_MONO -> FontFamily.Monospace
            }
            val lyricStyle = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = lyricsFont,
                fontWeight = FontWeight.Bold,
                fontSize = (32 * fontScale).sp,
                lineHeight = (40 * fontScale).sp,
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
                    top = activeLineTop,
                    bottom = maxOf(lyricBottomPadDp, endRoom)
                ),
                horizontalAlignment = lyricsHorizontalAlignment,
                modifier = modifier
                    .fillMaxSize()
                    .onSizeChanged { listHeightPx = it.height }
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
                    key = { idx, item ->
                        when (item) {
                            is LyricDisplayItem.Line -> "line_${item.lyricLine.timestampMs}_${idx}"
                            is LyricDisplayItem.Gap -> "gap_${item.fromMs}_${idx}"
                        }
                    }
                ) { displayIdx, item ->
                    when (item) {
                        is LyricDisplayItem.Gap -> {
                            val isInGap = effectivePositionMs in item.fromMs..item.toMs
                            // A break only takes space while it's on, so lines keep even spacing around it.
                            AnimatedVisibility(
                                visible = isInGap,
                                enter = expandVertically(tween(220, easing = SmoothEasing)),
                                exit = shrinkVertically(tween(220, easing = SmoothEasing))
                            ) {
                                GapItemView(
                                    isInGap = isInGap,
                                    isPlaying = isPlaying,
                                    progress = ((effectivePositionMs - item.fromMs).toFloat() /
                                        (item.toMs - item.fromMs).coerceAtLeast(1L)).coerceIn(0f, 1f),
                                    isCenterAligned = lyricsTextAlign == TextAlign.Center,
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
                        }

                        is LyricDisplayItem.Line -> {
                            val line = item.lyricLine
                            val index = item.originalIndex
                            val isSelected = selectedIndices.contains(index)
                            val ahead = index - activeLineIndex
                            // A short intro has no break before it, so the first line is in place but not lit until it starts.
                            val isActive = !selectionMode &&
                                currentActiveItem is LyricDisplayItem.Line &&
                                currentActiveItem.originalIndex == index &&
                                line.timestampMs <= effectivePositionMs

                            val lineScale by animateFloatAsState(
                                targetValue = if (isActive || isSelected) 1f else 0.98f,
                                animationSpec = tween(400, easing = SmoothEasing),
                                label = "lyricLineScale"
                            )

                            val lyricOpacity by animateFloatAsState(
                                targetValue = when {
                                    selectionMode -> if (isSelected) 1.00f else 0.35f
                                    isActive -> 1.00f
                                    userIsDragging -> 0.45f
                                    // The ones to come fade the further away they are. Lines just sung stay dim as they move
                                    // up and out under the top edge, rather than vanishing at once.
                                    ahead > 0 -> maxOf(0.12f, 0.45f - (ahead - 1) * 0.1f)
                                    else -> maxOf(0f, 0.3f + (ahead + 1) * 0.15f)
                                },
                                animationSpec = tween(280, easing = SmoothEasing),
                                label = "lyricLineOpacity"
                            )

                            // Lines further ahead soften. Android draws the blur from version 12; earlier ones skip it.
                            val lineBlur by animateDpAsState(
                                targetValue = if (isActive || selectionMode || userIsDragging || ahead < 3) 0.dp
                                    else minOf(6, (ahead - 2) * 2).dp,
                                animationSpec = tween(280, easing = SmoothEasing),
                                label = "lyricLineBlur"
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
                                    transformOrigin = TransformOrigin(if (lyricsTextAlign == TextAlign.Center) 0.5f else 0f, 0.5f)
                                }
                                .blur(lineBlur, BlurredEdgeTreatment.Unbounded)
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
                        LyricsCredits(artist, providerCredit, onSearchLyrics)
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
                LyricsCredits(artist, providerCredit, onSearchLyrics)
                Spacer(Modifier.height(lyricBottomPadDp))
            }
        }

        isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(AppR.string.lyrics_loading),
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
                onSearchLyrics = onSearchLyrics,
                modifier = modifier
            )
        }
    }
}

// A break between lines: the dots light up one by one as it passes, and gently pulse while playing.
@Composable
private fun GapItemView(
    isInGap: Boolean,
    isPlaying: Boolean,
    progress: Float,
    isCenterAligned: Boolean = true,
    onClick: () -> Unit
) {
    val preferences = LocalAppPreferences.current
    val dotColor = if (preferences.lyricsActiveStyle == `in`.caffeinelabs.cassettecat.data.settings.LyricsActiveStyle.ACCENT_GLOW) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val pulse = rememberInfiniteTransition(label = "gapPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "gapPulseScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isCenterAligned) Arrangement.Center else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.graphicsLayer {
                val scale = if (isInGap && isPlaying) pulseScale else 1f
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(if (isCenterAligned) 0.5f else 0f, 0.5f)
            }
        ) {
            repeat(3) { index ->
                val lit by animateFloatAsState(
                    targetValue = if (progress > index / 3f) 1f else 0.3f,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "gapDotLit"
                )
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .graphicsLayer { alpha = lit }
                        .background(dotColor, CircleShape)
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

    val progressAnim = remember { Animatable(0f) }
    val safeDuration = item.vocalDurationMs.coerceAtLeast(1L)
    val currentPositionMs by rememberUpdatedState(positionMs)
    // Position ticks arrive several times a second; restarting the fill on each one makes it stutter,
    // so it is only resynced when playback jumps (a seek) away from where the fill expects to be.
    var resyncToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(positionMs) {
        val expectedMs = line.timestampMs + (progressAnim.value * safeDuration).toLong()
        val fillFinished = progressAnim.value >= 1f && positionMs >= line.timestampMs + safeDuration
        if (!fillFinished && abs(positionMs - expectedMs) > 400L) resyncToken++
    }

    LaunchedEffect(line.timestampMs, safeDuration, isPlaying, resyncToken) {
        val currentElapsed = (currentPositionMs - line.timestampMs).coerceIn(0L, safeDuration)
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

    val onSurface = MaterialTheme.colorScheme.onSurface
    val preferences = LocalAppPreferences.current
    val activeWordColor = if (preferences.lyricsActiveStyle == `in`.caffeinelabs.cassettecat.data.settings.LyricsActiveStyle.ACCENT_GLOW) {
        MaterialTheme.colorScheme.tertiary
    } else {
        onSurface
    }

    // The line is laid out once and drawn twice, dim and lit. Each frame only the lit copy's mask changes, which
    // is a cheap redraw; rebuilding the styled text every frame meant a new text layout each time.
    val text = remember(words) { words.joinToString(" ") }
    val wordRanges = remember(words) {
        var start = 0
        words.map { word -> (start until start + word.length).also { start += word.length + 1 } }
    }
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val wordBounds = remember(layout, wordRanges) {
        layout?.let { result -> wordRanges.map { range -> result.getPathForRange(range.first, range.last + 1).getBounds() } }
    }

    Box(modifier = modifier) {
        Text(
            text = text,
            style = lyricStyle,
            color = onSurface.copy(alpha = 0.5f),
            textAlign = lyricStyle.textAlign,
            onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = text,
            style = lyricStyle,
            color = activeWordColor,
            textAlign = lyricStyle.textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val bounds = wordBounds ?: return@drawWithContent
                    val progress = progressAnim.value
                    bounds.forEachIndexed { wordIdx, rect ->
                        val (slotStart, slotEnd) = wordWeights.getOrElse(wordIdx) { 0f to 1f }
                        // Each word lights over its own share of the line, as it's sung; a short word still takes a
                        // quarter of a second, so it fades rather than blinks.
                        // The last words start earlier when that quarter-second would outlast the line.
                        val span = maxOf(slotEnd - slotStart, 250f / safeDuration).coerceAtMost(1f)
                        val start = minOf(slotStart, 1f - span)
                        val linear = ((progress - start) / span).coerceIn(0f, 1f)
                        val lit = linear * linear * (3f - 2f * linear)
                        if (lit < 1f) {
                            drawRect(
                                color = Color.Black.copy(alpha = 1f - lit),
                                topLeft = rect.topLeft,
                                size = rect.size,
                                blendMode = BlendMode.DstOut
                            )
                        }
                    }
                }
        )
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
                    contentDescription = stringResource(AppR.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = if (selectedCount == 0) {
                    stringResource(AppR.string.lyrics_select_up_to_five)
                } else {
                    pluralStringResource(AppR.plurals.lyrics_selected_count, selectedCount, selectedCount)
                },
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
                    text = stringResource(AppR.string.share_card_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedCount > 0) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WaveformBar(
    weight: Float,
    amplitude: Float,
    isPlaying: Boolean,
    index: Int
) {
    val targetHeight = if (isPlaying) (8f + 56f * (weight * 0.4f + 0.6f) * amplitude).dp else 8.dp
    val barHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(60, easing = FastOutSlowInEasing),
        label = "barHeight$index"
    )
    Box(
        modifier = Modifier
            .width(6.dp)
            .height(barHeight)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.tertiary)
    )
}

@Composable
internal fun InstrumentalWaveformView(
    artist: String?,
    isPlaying: Boolean,
    onReturnToPlayer: () -> Unit,
    onSearchLyrics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val barWeights = remember { floatArrayOf(0.42f, 0.72f, 0.56f, 1.0f, 0.62f, 0.82f, 0.46f) }
    val musicAmplitudes by AudioWaveformHolder.amplitudes.collectAsStateWithLifecycle()
    val hasAudioSignal = remember(musicAmplitudes) {
        musicAmplitudes.any { it > 0.02f }
    }

    val transition = rememberInfiniteTransition(label = "waveformPulse")

    val b1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(580, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b3"
    )
    val b4 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b4"
    )
    val b5 by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b5"
    )
    val b6 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(490, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b6"
    )
    val b7 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.80f,
        animationSpec = infiniteRepeatable(tween(540, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "b7"
    )

    val fallbackAmplitudes = listOf(b1, b2, b3, b4, b5, b6, b7)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(64.dp)
        ) {
            barWeights.forEachIndexed { index, weight ->
                val amp = if (isPlaying) {
                    if (hasAudioSignal) musicAmplitudes.getOrElse(index) { 0.15f }
                    else fallbackAmplitudes[index]
                } else {
                    0f
                }
                WaveformBar(
                    weight = weight,
                    amplitude = amp,
                    isPlaying = isPlaying,
                    index = index
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(AppR.string.lyrics_instrumental),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (!artist.isNullOrBlank()) {
                stringResource(AppR.string.lyrics_composed_by, artist)
            } else {
                stringResource(AppR.string.lyrics_no_lyrics)
            },
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = IbmPlexSansFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        CircleShape
                    )
                    .tapScale(onClick = onSearchLyrics)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(AppR.string.lyrics_search_online),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        CircleShape
                    )
                    .tapScale(onClick = onReturnToPlayer)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_disc_3),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(AppR.string.lyrics_album_art),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** Who wrote the song, where the lyrics came from, and a way to look for a better match. */
@Composable
private fun LyricsCredits(artist: String?, providerCredit: String?, onSearchLyrics: () -> Unit) {
    Column {
        if (!artist.isNullOrBlank()) {
            Text(
                text = stringResource(AppR.string.lyrics_written_by, artist),
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
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSearchLyrics() }
                .padding(vertical = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(AppR.string.lyrics_search_match),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
