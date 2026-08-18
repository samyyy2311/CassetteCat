package `in`.caffeinelabs.cassettecat.ui.theme

import androidx.compose.ui.graphics.Color

// Owned Device theme (default, dark-mode only, see CLAUDE.md). Neutral silver/
// black/off-white dominates every screen. Record Red is the ONE accent, reserved
// for active/interactive states (button states, playing indicators, outlines,
// small icons): never a background, large panel fill, or default chrome color.
// A second "Minimal" theme (pure grayscale, no accent) is planned but deferred;
// keep call sites referencing these tokens rather than literals so it can slot
// in later without a rewrite.

val RecordRed = Color(0xFFC23B30)

val Background = Color(0xFF000000)
val Surface = Color(0xFF1C1A18)
val SurfaceVariant = Color(0xFF262320)

val Silver = Color(0xFFC4C4C0)
val SilverDim = Color(0xFF6E6C68)

val TextPrimary = Color(0xFFF5F0EC)
val TextSecondary = Color(0xFFA8A29A)

// Derived, not part of the given spec: contrast color for the rare case content
// sits on top of a Record Red fill (e.g. a selected/active chip), and a dim
// on-surface tone used only for disabled/inactive states.
val OnRecordRed = Color(0xFF000000)
