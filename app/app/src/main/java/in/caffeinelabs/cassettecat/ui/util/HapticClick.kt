package `in`.caffeinelabs.cassettecat.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository

// Compose buttons don't haptic by default (unlike some platform Views); wrap click
// handlers with this for a consistent light tap.
@Composable
fun hapticClick(onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())
    val hapticEnabled = preferences.hapticFeedbackEnabled
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
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())
    val hapticEnabled = preferences.hapticFeedbackEnabled
    return { checked ->
        if (hapticEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
        }
        onCheckedChange(checked)
    }
}
