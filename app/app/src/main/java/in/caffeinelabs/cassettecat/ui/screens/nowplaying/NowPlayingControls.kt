package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SEEK_HAPTIC_TICK_INTERVAL_MS = 250L

internal fun isSeekablePlayback(durationMs: Long): Boolean = durationMs > 0L

@Composable
internal fun AppleMusicPillButton(
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
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        if (isSeekablePlayback(durationMs)) {
            ScrubberControl(positionMs = positionMs, durationMs = durationMs, onSeek = onSeek)
        } else {
            LiveIndicator(isPlaying = isPlaying, isBuffering = playWhenReady && !isPlaying)
        }
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
internal fun LyricsQueueToggleRow(activeView: NowPlayingView, onActiveViewChange: (NowPlayingView) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
private fun ScrubberControl(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val scope = rememberCoroutineScope()

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
                if (preferences.hapticFeedbackEnabled && (lastTick == null || abs(newPositionMs - lastTick) >= SEEK_HAPTIC_TICK_INTERVAL_MS)) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticTickMs = newPositionMs
                }
                dragPositionMs = newPositionMs
            },
            onValueChangeFinished = {
                if (preferences.hapticFeedbackEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
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
            val remainingMs = (durationMs - displayedPositionMs).coerceAtLeast(0L)
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
                isBuffering -> "CONNECTING…"
                isPlaying -> "LIVE"
                else -> "PAUSED"
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
