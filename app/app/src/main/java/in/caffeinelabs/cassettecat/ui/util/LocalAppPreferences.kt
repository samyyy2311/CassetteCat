package `in`.caffeinelabs.cassettecat.ui.util

import androidx.compose.runtime.compositionLocalOf
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences

val LocalAppPreferences = compositionLocalOf { AppPreferences() }
