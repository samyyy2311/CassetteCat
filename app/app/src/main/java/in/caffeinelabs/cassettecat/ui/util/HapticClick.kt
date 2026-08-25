package `in`.caffeinelabs.cassettecat.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Compose buttons don't haptic by default (unlike some platform Views); wrap click
// handlers with this for a consistent light tap.
@Composable
fun hapticClick(onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val hapticEnabled = LocalAppPreferences.current.hapticFeedbackEnabled
    return {
        if (hapticEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
        }
        onClick()
    }
}

@Composable
fun hapticToggle(onCheckedChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val haptics = LocalHapticFeedback.current
    val hapticEnabled = LocalAppPreferences.current.hapticFeedbackEnabled
    return { checked ->
        if (hapticEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
        }
        onCheckedChange(checked)
    }
}
