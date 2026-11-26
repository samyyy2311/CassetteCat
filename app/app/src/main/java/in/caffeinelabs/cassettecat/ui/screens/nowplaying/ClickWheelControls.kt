package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.util.LocalAppPreferences
import com.composables.icons.lucide.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

// Centre button diameter as a share of the whole wheel, matching the classic click-wheel proportions.
private const val CENTER_FRACTION = 0.36f

// One haptic "click" and one seek step for every this many degrees the wheel is spun.
private const val WHEEL_TICK_DEGREES = 15f
private const val WHEEL_SEEK_STEP_MS = 5_000L

private val WHEEL_MAX_SIZE = 264.dp
private val WHEEL_MIN_SIZE = 160.dp

private enum class WheelZone { MENU, PREVIOUS, NEXT, PLAY_PAUSE, CENTER }

// Scrubber on top with shuffle and repeat between its times, then the click wheel.
// Menu opens the song menu; spinning the wheel scrubs.
@Composable
internal fun ClickWheelControlsRow(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onSkipPrevious: () -> Unit,
    isPlaying: Boolean,
    playWhenReady: Boolean = isPlaying,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onMenu: () -> Unit,
    isShuffleEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    repeatMode: Int,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seekable = isSeekablePlayback(durationMs)
    Column(modifier) {
        if (seekable) {
            ScrubberControl(positionMs = positionMs, durationMs = durationMs, onSeek = onSeek) {
                val scheme = MaterialTheme.colorScheme
                Row {
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_shuffle,
                        contentDescription = stringResource(AppR.string.now_playing_shuffle),
                        tint = if (isShuffleEnabled) scheme.tertiary else scheme.onSurfaceVariant,
                        onClick = onToggleShuffle
                    )
                    PressDepthIconButton(
                        iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.lucide_ic_repeat_1 else R.drawable.lucide_ic_repeat,
                        contentDescription = stringResource(AppR.string.now_playing_repeat),
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) scheme.tertiary else scheme.onSurfaceVariant,
                        onClick = onCycleRepeatMode
                    )
                }
            }
        } else {
            LiveIndicator(isPlaying = isPlaying, isBuffering = playWhenReady && !isPlaying)
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Short screens get a smaller wheel, so the artwork keeps its room.
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val wheelSize = min(min(maxWidth.value * 0.68f, (screenHeight * 0.3f).value), WHEEL_MAX_SIZE.value)
                .coerceAtLeast(WHEEL_MIN_SIZE.value).dp
            ClickWheel(
                size = wheelSize,
                playWhenReady = playWhenReady,
                onMenu = onMenu,
                onSkipPrevious = onSkipPrevious,
                onSkipNext = onSkipNext,
                onTogglePlayPause = onTogglePlayPause,
                onSpinSeek = if (seekable) { target -> onSeek(target.coerceIn(0L, durationMs)) } else null,
                positionMs = positionMs
            )
        }
    }
}

@Composable
private fun ClickWheel(
    size: Dp,
    playWhenReady: Boolean,
    onMenu: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSpinSeek: ((Long) -> Unit)?,
    positionMs: Long
) {
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val hapticsEnabled = LocalAppPreferences.current.hapticFeedbackEnabled
    val touchSlop = LocalViewConfiguration.current.touchSlop

    var pressedZone by remember { mutableStateOf<WheelZone?>(null) }

    val currentOnMenu by rememberUpdatedState(onMenu)
    val currentOnPrevious by rememberUpdatedState(onSkipPrevious)
    val currentOnNext by rememberUpdatedState(onSkipNext)
    val currentOnPlayPause by rememberUpdatedState(onTogglePlayPause)
    val currentOnSpinSeek by rememberUpdatedState(onSpinSeek)
    // Each spin seeks relative to where the song was when the finger landed.
    val currentPosition by rememberUpdatedState(positionMs)

    val menuLabel = stringResource(AppR.string.now_playing_menu)
    val previousLabel = stringResource(AppR.string.now_playing_previous)
    val nextLabel = stringResource(AppR.string.now_playing_next)
    val playPauseLabel = stringResource(AppR.string.now_playing_play_pause)
    val wheelLabel = stringResource(AppR.string.now_playing_click_wheel)

    // A press feels like the app's other buttons; spinning ticks like a rotary dial, as in the A to Z rail.
    fun press() {
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
    }

    fun tick() {
        if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    Box(
        modifier = Modifier
            .size(size)
            .semantics {
                contentDescription = wheelLabel
                customActions = listOf(
                    CustomAccessibilityAction(playPauseLabel) { currentOnPlayPause(); true },
                    CustomAccessibilityAction(nextLabel) { currentOnNext(); true },
                    CustomAccessibilityAction(previousLabel) { currentOnPrevious(); true },
                    CustomAccessibilityAction(menuLabel) { currentOnMenu(); true }
                )
            }
            .pointerInput(Unit) {
                val radius = this.size.width / 2f
                val center = Offset(radius, radius)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val zone = zoneAt(down.position, center, radius) ?: return@awaitEachGesture
                    down.consume()
                    pressedZone = zone

                    var spinning = false
                    var lastAngle = angleOf(down.position, center)
                    var accumulated = 0f
                    var steps = 0
                    val startPosition = currentPosition
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        // Once a finger travels far enough it's a spin, not a tap.
                        if (!spinning && currentOnSpinSeek != null &&
                            (change.position - down.position).getDistance() > touchSlop
                        ) {
                            spinning = true
                            pressedZone = null
                        }
                        if (spinning) {
                            val angle = angleOf(change.position, center)
                            var delta = angle - lastAngle
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            lastAngle = angle
                            accumulated += delta
                            val newSteps = (accumulated / WHEEL_TICK_DEGREES).toInt()
                            if (newSteps != steps) {
                                steps = newSteps
                                tick()
                                currentOnSpinSeek?.invoke(startPosition + steps * WHEEL_SEEK_STEP_MS)
                            }
                        }
                    }
                    if (!spinning && pressedZone == zone) {
                        press()
                        when (zone) {
                            WheelZone.MENU -> currentOnMenu()
                            WheelZone.PREVIOUS -> currentOnPrevious()
                            WheelZone.NEXT -> currentOnNext()
                            WheelZone.PLAY_PAUSE, WheelZone.CENTER -> currentOnPlayPause()
                        }
                    }
                    pressedZone = null
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val scheme = MaterialTheme.colorScheme
        val labelColor = scheme.onSurfaceVariant
        fun zoneTint(zone: WheelZone) = if (pressedZone == zone) scheme.tertiary else labelColor
        val pressedShade = scheme.onSurface.copy(alpha = 0.08f)

        // A see-through ring lit from above, so the artwork's colour shows through, around a centre
        // with the transport buttons' cap and accent edge. A press shades its quarter or the centre.
        val ring = Brush.verticalGradient(listOf(scheme.onSurface.copy(alpha = 0.1f), scheme.onSurface.copy(alpha = 0.03f)))
        val cap = Brush.verticalGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerLowest))
        Canvas(Modifier.fillMaxSize()) {
            val hairline = 1.dp.toPx()
            drawCircle(ring)
            drawCircle(scheme.outlineVariant, radius = this.size.minDimension / 2f - hairline / 2f, style = Stroke(hairline))
            val pressed = pressedZone
            if (pressed != null && pressed != WheelZone.CENTER) {
                val start = when (pressed) {
                    WheelZone.NEXT -> -45f
                    WheelZone.PLAY_PAUSE -> 45f
                    WheelZone.PREVIOUS -> 135f
                    else -> 225f
                }
                drawArc(pressedShade, startAngle = start, sweepAngle = 90f, useCenter = true)
            }
            val centerRadius = this.size.minDimension * CENTER_FRACTION / 2f
            drawCircle(cap, radius = centerRadius)
            drawCircle(scheme.tertiary, radius = centerRadius, style = Stroke(1.5.dp.toPx()))
            if (pressed == WheelZone.CENTER) drawCircle(pressedShade, radius = centerRadius)
        }

        // Play and pause carry the accent, as the classic layout's play button does.
        val playPauseTint = if (pressedZone == WheelZone.PLAY_PAUSE) scheme.onSurface else scheme.tertiary
        val labelInset = size * ((1f - CENTER_FRACTION) / 4f + CENTER_FRACTION / 2f)
        val iconSize = (size.value * 0.1f).coerceIn(16f, 24f).dp
        Text(
            text = menuLabel.uppercase(),
            color = zoneTint(WheelZone.MENU),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.offset(y = -labelInset)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_skip_back),
            contentDescription = null,
            tint = zoneTint(WheelZone.PREVIOUS),
            modifier = Modifier.offset(x = -labelInset).size(iconSize)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_skip_forward),
            contentDescription = null,
            tint = zoneTint(WheelZone.NEXT),
            modifier = Modifier.offset(x = labelInset).size(iconSize)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_play),
            contentDescription = null,
            // The action the button would take next is the brighter of the two.
            tint = playPauseTint.copy(alpha = if (playWhenReady) 0.5f else 1f),
            modifier = Modifier.offset(x = -iconSize * 0.45f, y = labelInset).size(iconSize * 0.8f)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_pause),
            contentDescription = null,
            tint = playPauseTint.copy(alpha = if (playWhenReady) 1f else 0.5f),
            modifier = Modifier.offset(x = iconSize * 0.45f, y = labelInset).size(iconSize * 0.8f)
        )
    }
}

// Degrees clockwise from the positive x-axis, 0..360, in screen coordinates (y down).
private fun angleOf(point: Offset, center: Offset): Float {
    val deg = (atan2(point.y - center.y, point.x - center.x) * 180f / PI).toFloat()
    return if (deg < 0f) deg + 360f else deg
}

private fun zoneAt(point: Offset, center: Offset, radius: Float): WheelZone? {
    val distance = hypot(point.x - center.x, point.y - center.y)
    if (distance > radius) return null
    if (distance <= radius * CENTER_FRACTION) return WheelZone.CENTER
    val angle = angleOf(point, center)
    return when {
        angle >= 315f || angle < 45f -> WheelZone.NEXT
        angle < 135f -> WheelZone.PLAY_PAUSE
        angle < 225f -> WheelZone.PREVIOUS
        else -> WheelZone.MENU
    }
}
