package `in`.caffeinelabs.cassettecat.ui.util

import androidx.compose.runtime.compositionLocalOf

/** The song the player has loaded, so every song row can mark it without each screen passing it down. */
data class PlayingSong(val id: String? = null, val isPlaying: Boolean = false)

val LocalPlayingSong = compositionLocalOf { PlayingSong() }
