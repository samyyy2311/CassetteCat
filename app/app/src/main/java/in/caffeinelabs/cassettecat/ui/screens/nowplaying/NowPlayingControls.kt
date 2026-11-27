package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.playback.SongWaveforms
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.SeekBarStyle
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.LocalAppPreferences
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin

private const val SEEK_HAPTIC_TICK_INTERVAL_MS = 250L
private const val POSITION_EASE_MS = 250
private const val POSITION_JUMP_MS = 1_000f

internal fun isSeekablePlayback(durationMs: Long): Boolean = durationMs > 0L

@Composable
internal fun PlaybackControlsRow(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    isShuffleEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    isPlaying: Boolean,
    playWhenReady: Boolean = isPlaying,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    repeatMode: Int,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier,
    waveform: FloatArray? = null
) {
    Column(modifier) {
        ProgressReadout(positionMs, durationMs, onSeek, isPlaying, playWhenReady, waveform)
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShuffleButton(isShuffleEnabled, onToggleShuffle)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportButton(
                    iconRes = R.drawable.lucide_ic_skip_back,
                    size = 56.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onSkipPrevious
                )
                Spacer(Modifier.width(20.dp))
                Crossfade(
                    targetState = playWhenReady,
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
            RepeatButton(repeatMode, onCycleRepeatMode)
        }
    }
}

// The seek bar for songs, or the live indicator for streams without a length. [centerContent] sits
// between the seek bar's times.
@Composable
internal fun ProgressReadout(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    waveform: FloatArray? = null,
    centerContent: (@Composable () -> Unit)? = null
) {
    if (isSeekablePlayback(durationMs)) {
        ScrubberControl(positionMs, durationMs, onSeek, isPlaying, waveform, centerContent)
    } else {
        LiveIndicator(isPlaying = isPlaying, isBuffering = playWhenReady && !isPlaying)
        if (centerContent != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { centerContent() }
        }
    }
}

@Composable
internal fun ShuffleButton(isShuffleEnabled: Boolean, onToggleShuffle: () -> Unit) {
    PressDepthIconButton(
        iconRes = R.drawable.lucide_ic_shuffle,
        contentDescription = stringResource(AppR.string.now_playing_shuffle),
        tint = if (isShuffleEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onToggleShuffle
    )
}

@Composable
internal fun RepeatButton(repeatMode: Int, onCycleRepeatMode: () -> Unit) {
    PressDepthIconButton(
        iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.lucide_ic_repeat_1 else R.drawable.lucide_ic_repeat,
        contentDescription = stringResource(AppR.string.now_playing_repeat),
        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onCycleRepeatMode
    )
}

@Composable
internal fun LyricsQueueToggleRow(
    activeView: NowPlayingView,
    onActiveViewChange: (NowPlayingView) -> Unit,
    onOpenDevices: (() -> Unit)? = null,
    deviceName: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_mic_vocal,
            contentDescription = stringResource(
                if (activeView == NowPlayingView.LYRICS) AppR.string.now_playing_close_lyrics
                else AppR.string.now_playing_lyrics
            ),
            tint = if (activeView == NowPlayingView.LYRICS) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = {
                onActiveViewChange(if (activeView == NowPlayingView.LYRICS) NowPlayingView.PLAYER else NowPlayingView.LYRICS)
            }
        )
        if (onOpenDevices != null) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_monitor,
                contentDescription = deviceName ?: stringResource(AppR.string.desktop_remote_devices),
                tint = if (deviceName != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onOpenDevices
            )
        }
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_list,
            contentDescription = stringResource(
                if (activeView == NowPlayingView.QUEUE) AppR.string.now_playing_close_queue
                else AppR.string.now_playing_queue
            ),
            tint = if (activeView == NowPlayingView.QUEUE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = {
                onActiveViewChange(if (activeView == NowPlayingView.QUEUE) NowPlayingView.PLAYER else NowPlayingView.QUEUE)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScrubberControl(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    isPlaying: Boolean,
    // The song's loudness for the Waveform style; without it that style draws a line.
    waveform: FloatArray?,
    // Sits between the two times, for layouts with no other room for it.
    centerContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences = LocalAppPreferences.current
    val scope = rememberCoroutineScope()

    var dragPositionMs by remember { mutableStateOf<Long?>(null) }
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }
    var lastHapticTickMs by remember { mutableStateOf<Long?>(null) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(positionMs, pendingSeekMs) {
        val pending = pendingSeekMs
        if (pending != null && abs(positionMs - pending) < 1200L) {
            pendingSeekMs = null
        }
    }

    // A new song's length can arrive a frame before its position. Until a fresh position comes in,
    // the old song's one is ignored, so the bar starts the new song at zero instead of flicking to the end.
    var knownDurationMs by remember { mutableLongStateOf(durationMs) }
    var stalePositionMs by remember { mutableStateOf<Long?>(null) }
    if (durationMs != knownDurationMs) {
        knownDurationMs = durationMs
        stalePositionMs = positionMs
    } else if (stalePositionMs != null && positionMs != stalePositionMs) {
        stalePositionMs = null
    }
    val currentPositionMs = if (stalePositionMs != null) 0L else positionMs

    val isDragging = dragPositionMs != null
    val effectivePositionMs = dragPositionMs ?: pendingSeekMs ?: currentPositionMs
    val safeDuration = durationMs.coerceAtLeast(0L).toFloat()

    val targetSliderValue = effectivePositionMs.toFloat().coerceIn(0f, safeDuration.coerceAtLeast(1f))
    // The position arrives every quarter second, so easing over the same time keeps the bar moving
    // steadily. A new song or a seek jumps straight there instead of sweeping across the bar.
    var previousTarget by remember { mutableFloatStateOf(targetSliderValue) }
    val jumped = abs(targetSliderValue - previousTarget) > POSITION_JUMP_MS
    SideEffect { previousTarget = targetSliderValue }
    val animatedSliderValue by animateFloatAsState(
        targetValue = targetSliderValue,
        animationSpec = if (isDragging || pendingSeekMs != null || jumped) {
            snap()
        } else {
            tween(durationMillis = POSITION_EASE_MS, easing = LinearEasing)
        },
        label = "scrubberSliderPosition"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (isDragging || pendingSeekMs != null) targetSliderValue else animatedSliderValue,
            onValueChange = { value ->
                val newPositionMs = value.toLong()
                val lastTick = lastHapticTickMs
                if (preferences.hapticFeedbackEnabled && (lastTick == null || abs(newPositionMs - lastTick) >= SEEK_HAPTIC_TICK_INTERVAL_MS)) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticTickMs = newPositionMs
                }
                dragPositionMs = newPositionMs
            },
            onValueChangeFinished = {
                val target = dragPositionMs
                if (preferences.hapticFeedbackEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                if (target != null) {
                    pendingSeekMs = target
                    onSeek(target)
                }
                dragPositionMs = null
                lastHapticTickMs = null
            },
            valueRange = 0f..safeDuration.coerceAtLeast(1f),
            track = { sliderState ->
                when (preferences.seekBarStyle) {
                    SeekBarStyle.FADER -> FaderTrack(sliderState, isActive = isDragging)
                    SeekBarStyle.LINE -> LineTrack(sliderState, isActive = isDragging)
                    SeekBarStyle.WAVY -> WavyTrack(sliderState, isActive = isDragging, isPlaying = isPlaying)
                    SeekBarStyle.WAVEFORM -> if (waveform != null) {
                        WaveformTrack(sliderState, waveform)
                    } else {
                        LineTrack(sliderState, isActive = isDragging)
                    }
                }
            },
            thumb = {
                when (preferences.seekBarStyle) {
                    SeekBarStyle.FADER -> FaderThumb(isActive = isDragging)
                    SeekBarStyle.LINE, SeekBarStyle.WAVEFORM -> LineThumb(isActive = isDragging)
                    SeekBarStyle.WAVY -> WavyThumb()
                }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatTime(effectivePositionMs), style = readoutStyle())
            centerContent?.invoke()
            val remainingMs = (durationMs - effectivePositionMs).coerceAtLeast(0L)
            val durationText = if (preferences.showRemainingTime) "-${formatTime(remainingMs)}" else formatTime(durationMs)
            Text(
                text = durationText,
                style = readoutStyle(),
                modifier = Modifier.clickable {
                    scope.launch {
                        appPreferencesRepository.setShowRemainingTime(!preferences.showRemainingTime)
                    }
                }
            )
        }
    }
}

@Composable
internal fun LiveIndicator(isPlaying: Boolean = true, isBuffering: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveWaveform(isPlaying = isPlaying && !isBuffering)
        Spacer(Modifier.width(10.dp))
        Text(
            when {
                isBuffering -> stringResource(AppR.string.now_playing_connecting)
                isPlaying -> stringResource(AppR.string.now_playing_live)
                else -> stringResource(AppR.string.now_playing_paused)
            },
            style = readoutStyle()
        )
    }
}

private const val WAVEFORM_BAR_COUNT = 5

@Composable
private fun LiveWaveform(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "liveWaveform")
    val color = if (isPlaying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(WAVEFORM_BAR_COUNT) { index ->
            val heightFraction = if (isPlaying) {
                val animated by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 480 + index * 90, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 90)
                    ),
                    label = "waveBar$index"
                )
                animated
            } else {
                0.3f
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp * heightFraction)
                    .background(color, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaderTrack(sliderState: SliderState, isActive: Boolean) {
    val grooveColor = MaterialTheme.colorScheme.outlineVariant
    val filledColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
        animationSpec = tween(80, easing = LinearEasing),
        label = "faderTrackFillColor"
    )
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

// The desktop app's seek bar: a thin line that thickens while dragged.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineTrack(sliderState: SliderState, isActive: Boolean) {
    val height by animateDpAsState(if (isActive) 5.dp else 3.dp, tween(100), label = "lineTrackHeight")
    val range = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(1f)
    val fraction = ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
    ) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary, shape))
    }
}

// The system media controls' look: the played part ripples while music plays and lies flat when
// paused or dragged, with a gap either side of the handle.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WavyTrack(sliderState: SliderState, isActive: Boolean, isPlaying: Boolean) {
    val fraction = sliderFraction(sliderState)
    val amplitude by animateDpAsState(if (isPlaying && !isActive) 3.dp else 0.dp, tween(400), label = "wavyAmplitude")
    val phase by rememberInfiniteTransition(label = "wavyPhase").animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing)),
        label = "wavyPhaseValue"
    )
    val played = MaterialTheme.colorScheme.tertiary
    val rest = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val stroke = 4.dp.toPx()
        val gap = 6.dp.toPx()
        val mid = size.height / 2f
        val at = size.width * fraction
        val wavelength = 32.dp.toPx()
        val waveEnd = at - gap
        if (waveEnd > 0f) {
            val path = Path().apply {
                moveTo(0f, mid)
                var x = 0f
                while (x < waveEnd) {
                    // The wave eases in over its first half-length, so it doesn't start with a jolt.
                    val ease = (x / (wavelength / 2f)).coerceAtMost(1f)
                    lineTo(x, mid + amplitude.toPx() * ease * sin(x / wavelength * 2f * PI.toFloat() - phase))
                    x += 1.5f
                }
                lineTo(waveEnd, mid + amplitude.toPx() * sin(waveEnd / wavelength * 2f * PI.toFloat() - phase))
            }
            drawPath(path, played, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        val restStart = at + gap
        if (restStart < size.width) drawLine(rest, Offset(restStart, mid), Offset(size.width, mid), stroke, StrokeCap.Round)
    }
}

@Composable
private fun WavyThumb() {
    Box(Modifier.size(width = 4.dp, height = 20.dp).background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(50)))
}

// The song's loudness as bars, filled in up to the playing point.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveformTrack(sliderState: SliderState, waveform: FloatArray) {
    val fraction = sliderFraction(sliderState)
    val played = MaterialTheme.colorScheme.tertiary
    val rest = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val slot = size.width / waveform.size
        val barWidth = slot * 0.6f
        waveform.forEachIndexed { index, level ->
            val height = (size.height * level).coerceAtLeast(barWidth)
            val left = index * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color = if ((index + 0.5f) / waveform.size <= fraction) played else rest,
                topLeft = Offset(left, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun sliderFraction(sliderState: SliderState): Float {
    val range = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(1f)
    return ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
}

// Loads the playing song's shape when the Waveform seek bar is chosen.
@Composable
internal fun rememberWaveform(song: Song?): FloatArray? {
    val context = LocalContext.current
    val style = LocalAppPreferences.current.seekBarStyle
    return produceState<FloatArray?>(null, song?.id, style) {
        value = if (song != null && style == SeekBarStyle.WAVEFORM) SongWaveforms.load(context, song) else null
    }.value
}

// Shown only while dragging, as on the desktop. Its width stays fixed, as the slider insets the track by it.
@Composable
private fun LineThumb(isActive: Boolean) {
    val alpha by animateFloatAsState(if (isActive) 1f else 0f, tween(100), label = "lineThumbAlpha")
    Box(Modifier.size(width = 10.dp, height = 18.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
                .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(5.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(5.dp))
        )
    }
}

@Composable
private fun FaderThumb(isActive: Boolean) {
    val width by animateDpAsState(if (isActive) 14.dp else 10.dp, tween(80, easing = LinearEasing), label = "faderThumbWidth")
    val height by animateDpAsState(if (isActive) 27.dp else 22.dp, tween(80, easing = LinearEasing), label = "faderThumbHeight")
    val elevation by animateDpAsState(if (isActive) 6.dp else 3.dp, tween(80, easing = LinearEasing), label = "faderThumbElevation")
    val borderColor = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .shadow(elevation, RoundedCornerShape(3.dp))
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary)
                ),
                RoundedCornerShape(3.dp)
            )
            .border(if (isActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(3.dp))
    )
}

@Composable
internal fun readoutStyle() = MaterialTheme.typography.bodyMedium.copy(
    fontFamily = IbmPlexMonoFontFamily,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
