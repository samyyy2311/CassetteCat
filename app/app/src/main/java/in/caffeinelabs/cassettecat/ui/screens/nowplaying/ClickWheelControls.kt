package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.util.LocalAppPreferences
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

// Centre button diameter as a share of the whole wheel, matching the classic click-wheel proportions.
private const val CENTER_FRACTION = 0.38f

// One haptic "click" and one seek step for every this many degrees the wheel is spun.
private const val WHEEL_TICK_DEGREES = 15f
private const val WHEEL_SEEK_STEP_MS = 5_000L

private val WHEEL_MAX_SIZE = 240.dp
private val WHEEL_MIN_SIZE = 150.dp

private enum class WheelZone { MENU, PREVIOUS, NEXT, PLAY_PAUSE, CENTER }

// Dark wheel on dark themes, the white wheel with grey labels on light ones.
private data class WheelColors(
    val ringTop: Color,
    val ringBottom: Color,
    val ringEdge: Color,
    val centerTop: Color,
    val centerBottom: Color,
    val label: Color,
    val pressed: Color
)

@Composable
private fun wheelColors(): WheelColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (dark) {
        WheelColors(
            ringTop = Color(0xFF2A2A2C),
            ringBottom = Color(0xFF151516),
            ringEdge = Color(0x33FFFFFF),
            centerTop = Color(0xFF5A5A5E),
            centerBottom = Color(0xFF3A3A3D),
            label = Color(0xFFE8E8EA),
            pressed = Color(0x22FFFFFF)
        )
    } else {
        WheelColors(
            ringTop = Color(0xFFFDFDFD),
            ringBottom = Color(0xFFE6E6E8),
            ringEdge = Color(0x22000000),
            centerTop = Color(0xFFEDEDEF),
            centerBottom = Color(0xFFD2D2D6),
            label = Color(0xFF9A9AA0),
            pressed = Color(0x14000000)
        )
    }
}

// Scrubber on top, then shuffle · click wheel · repeat. Menu opens the song menu; spinning the wheel scrubs.
@Composable
internal fun ClickWheelControlsRow(
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
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seekable = isSeekablePlayback(durationMs)
    Column(modifier) {
        if (seekable) {
            ScrubberControl(positionMs = positionMs, durationMs = durationMs, onSeek = onSeek)
        } else {
            LiveIndicator(isPlaying = isPlaying, isBuffering = playWhenReady && !isPlaying)
        }
        Spacer(Modifier.height(20.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Leave room for the side toggles, and keep short screens from pushing the artwork off.
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val wheelSize = min(
                min((maxWidth - 112.dp).value, (screenHeight * 0.28f).value),
                WHEEL_MAX_SIZE.value
            ).coerceAtLeast(WHEEL_MIN_SIZE.value).dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_shuffle,
                    contentDescription = stringResource(AppR.string.now_playing_shuffle),
                    tint = if (isShuffleEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggleShuffle
                )
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
                PressDepthIconButton(
                    iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.lucide_ic_repeat_1 else R.drawable.lucide_ic_repeat,
                    contentDescription = stringResource(AppR.string.now_playing_repeat),
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onCycleRepeatMode
                )
            }
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
    val colors = wheelColors()
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalAppPreferences.current.hapticFeedbackEnabled
    val touchSlop = LocalViewConfiguration.current.touchSlop

    var pressedZone by remember { mutableStateOf<WheelZone?>(null) }
    val centerScale by animateFloatAsState(
        targetValue = if (pressedZone == WheelZone.CENTER) 0.95f else 1f,
        animationSpec = tween(90),
        label = "wheelCenterPress"
    )

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

    fun click() {
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        // Only the ring spins; once a finger travels far enough it's a spin, not a tap.
                        if (!spinning && zone != WheelZone.CENTER && currentOnSpinSeek != null &&
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
                                click()
                                currentOnSpinSeek?.invoke(startPosition + steps * WHEEL_SEEK_STEP_MS)
                            }
                        }
                    }
                    if (!spinning && pressedZone == zone) {
                        click()
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
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            drawCircle(Brush.verticalGradient(listOf(colors.ringTop, colors.ringBottom)), radius = r)
            drawCircle(colors.ringEdge, radius = r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            val pressed = pressedZone
            if (pressed != null && pressed != WheelZone.CENTER) {
                val start = when (pressed) {
                    WheelZone.NEXT -> -45f
                    WheelZone.PLAY_PAUSE -> 45f
                    WheelZone.PREVIOUS -> 135f
                    else -> 225f
                }
                drawArc(colors.pressed, startAngle = start, sweepAngle = 90f, useCenter = true)
            }
        }
        // Centre button: a separate raised disc that dips slightly when pressed.
        Canvas(
            Modifier
                .size(size * CENTER_FRACTION)
                .graphicsLayer { scaleX = centerScale; scaleY = centerScale }
        ) {
            val r = this.size.minDimension / 2f
            drawCircle(colors.ringBottom, radius = r + 1.dp.toPx())
            drawCircle(Brush.verticalGradient(listOf(colors.centerTop, colors.centerBottom)), radius = r)
        }

        val labelInset = size * ((1f - CENTER_FRACTION) / 4f + CENTER_FRACTION / 2f)
        val iconSize = (size.value * 0.075f).coerceIn(12f, 18f).dp
        Text(
            text = "MENU",
            color = colors.label,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.06f).coerceIn(10f, 15f).sp,
            letterSpacing = 1.sp,
            modifier = Modifier.offset(y = -labelInset)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_skip_back),
            contentDescription = null,
            tint = colors.label,
            modifier = Modifier.offset(x = -labelInset).size(iconSize)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_skip_forward),
            contentDescription = null,
            tint = colors.label,
            modifier = Modifier.offset(x = labelInset).size(iconSize)
        )
        Row(
            modifier = Modifier.offset(y = labelInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The live state is shown by emphasis: whichever action the button would take next is brighter.
            Icon(
                painter = painterResource(R.drawable.lucide_ic_play),
                contentDescription = null,
                tint = colors.label.copy(alpha = if (playWhenReady) 0.55f else 1f),
                modifier = Modifier.size(iconSize * 0.85f)
            )
            Spacer(Modifier.width(1.dp))
            Icon(
                painter = painterResource(R.drawable.lucide_ic_pause),
                contentDescription = null,
                tint = colors.label.copy(alpha = if (playWhenReady) 1f else 0.55f),
                modifier = Modifier.size(iconSize * 0.85f)
            )
        }
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
