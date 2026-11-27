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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
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
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.util.LocalAppPreferences
import com.composables.icons.lucide.R
import kotlin.math.PI
import kotlin.math.abs
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

private enum class WheelZone { MENU, PREVIOUS, NEXT, PLAY_PAUSE }

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
        ProgressReadout(positionMs, durationMs, onSeek, isPlaying, playWhenReady) {
            Row {
                ShuffleButton(isShuffleEnabled, onToggleShuffle)
                RepeatButton(repeatMode, onCycleRepeatMode)
            }
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
                awaitEachGesture {
                    var startPosition = 0L
                    val tapped = trackWheelGesture(
                        touchSlop = touchSlop,
                        canSpin = currentOnSpinSeek != null,
                        onPressedChange = { zone ->
                            // Each spin seeks relative to where the song was when the finger landed.
                            if (zone != null && pressedZone == null) startPosition = currentPosition
                            pressedZone = zone
                        },
                        onStep = { steps ->
                            tick()
                            currentOnSpinSeek?.invoke(startPosition + steps * WHEEL_SEEK_STEP_MS)
                        }
                    )
                    pressedZone = null
                    if (tapped != null) press()
                    when (tapped) {
                        WheelZone.MENU -> currentOnMenu()
                        WheelZone.PREVIOUS -> currentOnPrevious()
                        WheelZone.NEXT -> currentOnNext()
                        WheelZone.PLAY_PAUSE -> currentOnPlayPause()
                        null -> Unit
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val scheme = MaterialTheme.colorScheme
        val labelColor = scheme.onSurfaceVariant
        fun zoneTint(zone: WheelZone) = if (pressedZone == zone) scheme.tertiary else labelColor
        val pressedShade = scheme.onSurface.copy(alpha = 0.08f)

        // The ring has the transport buttons' cap, outline and shadow, and a press shades its quarter.
        val cap = Brush.linearGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerLowest))
        Canvas(Modifier.fillMaxSize().shadow(5.dp, CircleShape)) {
            val hairline = 1.dp.toPx()
            drawCircle(cap)
            drawCircle(scheme.outlineVariant, radius = this.size.minDimension / 2f - hairline / 2f, style = Stroke(hairline))
            val pressed = pressedZone ?: return@Canvas
            val start = when (pressed) {
                WheelZone.NEXT -> -45f
                WheelZone.PLAY_PAUSE -> 45f
                WheelZone.PREVIOUS -> 135f
                WheelZone.MENU -> 225f
            }
            drawArc(pressedShade, startAngle = start, sweepAngle = 90f, useCenter = true)
        }

        // The centre is the app's transport button, accented while playing like the classic play button.
        TransportButton(
            iconRes = null,
            size = size * CENTER_FRACTION,
            tint = scheme.tertiary,
            onClick = onTogglePlayPause,
            contentDescription = playPauseLabel,
            accented = playWhenReady
        )

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

// Follows one touch on the wheel and returns the quarter it tapped, or null for a spin or a press
// that slid off. Only travel around the ring makes a spin, so a finger slipping inwards or outwards
// still taps, and crossing the centre swings the angle without going round, so it isn't counted.
private suspend fun AwaitPointerEventScope.trackWheelGesture(
    touchSlop: Float,
    canSpin: Boolean,
    onPressedChange: (WheelZone?) -> Unit,
    onStep: (Int) -> Unit
): WheelZone? {
    // Read for each gesture, as the wheel resizes in place.
    val radius = size.width / 2f
    val center = Offset(radius, radius)
    val down = awaitFirstDown(requireUnconsumed = false)
    val zone = zoneAt(down.position, center, radius) ?: return null
    down.consume()
    onPressedChange(zone)

    var spinning = false
    var lastAngle = angleOf(down.position, center)
    var lastPosition = down.position
    var offRing = false
    var accumulated = 0f
    var steps = 0
    val touchRadius = (down.position - center).getDistance()
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
        if (!change.pressed) break
        change.consume()
        lastPosition = change.position
        val angle = angleOf(change.position, center)
        if ((change.position - center).getDistance() <= radius * CENTER_FRACTION) {
            offRing = true
            continue
        }
        if (offRing) {
            offRing = false
            lastAngle = angle
        }
        var delta = angle - lastAngle
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        lastAngle = angle
        accumulated += delta
        if (!spinning && canSpin && abs(accumulated) * PI.toFloat() / 180f * touchRadius > touchSlop) {
            spinning = true
            onPressedChange(null)
        }
        if (spinning) {
            val newSteps = (accumulated / WHEEL_TICK_DEGREES).toInt()
            if (newSteps != steps) {
                steps = newSteps
                onStep(steps)
            }
        }
    }
    // A press counts only when the finger lifts over the quarter it went down on.
    return zone.takeIf { !spinning && zoneAt(lastPosition, center, radius) == zone }
}

// Degrees clockwise from the positive x-axis, 0..360, in screen coordinates (y down).
private fun angleOf(point: Offset, center: Offset): Float {
    val deg = (atan2(point.y - center.y, point.x - center.x) * 180f / PI).toFloat()
    return if (deg < 0f) deg + 360f else deg
}

private fun zoneAt(point: Offset, center: Offset, radius: Float): WheelZone? {
    val distance = hypot(point.x - center.x, point.y - center.y)
    if (distance > radius) return null
    if (distance <= radius * CENTER_FRACTION) return null
    val angle = angleOf(point, center)
    return when {
        angle >= 315f || angle < 45f -> WheelZone.NEXT
        angle < 135f -> WheelZone.PLAY_PAUSE
        angle < 225f -> WheelZone.PREVIOUS
        else -> WheelZone.MENU
    }
}
