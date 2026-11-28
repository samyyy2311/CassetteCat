package `in`.caffeinelabs.cassettecat.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Scale-down press feedback without ripple bounds.
fun Modifier.tapScale(onClick: () -> Unit): Modifier = pressScale(onClick = onClick)

fun Modifier.tapScaleSelectable(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    pressScale(onClick = onClick, onLongClick = onLongClick)

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pressScale(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(100), label = "tapScale")
    val haptics = LocalHapticFeedback.current
    val hapticEnabled = LocalAppPreferences.current.hapticFeedbackEnabled

    graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onLongClick = onLongClick?.let {
                {
                    if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    it()
                }
            },
            onClick = {
                if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
        )
}
