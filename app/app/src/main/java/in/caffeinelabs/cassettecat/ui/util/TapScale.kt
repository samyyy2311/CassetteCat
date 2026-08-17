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

// scale-down press feedback, no default ripple (avoids the rectangular bounds on block-shaped rows/cards)
fun Modifier.tapScale(onClick: () -> Unit): Modifier = pressScale(onClick = onClick)

// Same feedback, plus long-press, for the few rows that need both (e.g. multi-select).
// Kept separate from tapScale rather than adding an optional param to it: Kotlin's
// trailing-lambda-without-parens call sites (.tapScale { ... }, used throughout the app)
// require the function-typed param to be last, while the positional call sites
// (.tapScale(onClick), also used throughout) require it to be first — no single parameter
// order satisfies both existing call styles.
fun Modifier.tapScaleSelectable(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    pressScale(onClick = onClick, onLongClick = onLongClick)

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pressScale(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(100), label = "tapScale")
    val haptics = LocalHapticFeedback.current
    graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onLongClick = onLongClick?.let {
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    it()
                }
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
        )
}
