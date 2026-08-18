package `in`.caffeinelabs.cassettecat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Owned Device theme (default, dark-mode only, see CLAUDE.md).
//
// primary/secondary map to silver, not Record Red: Material3's default filled Button/FAB
// use `primary` as background fill, and red must never be a large fill. Record Red lives
// on tertiary/error instead (Material3's small-accent roles), so the deferred "Minimal"
// (grayscale) theme can swap in later via tokens instead of a rewrite.
private val ActiveContainer = Color(0xFF3A1512)

private val OwnedDeviceColorScheme = darkColorScheme(
    primary = Silver,
    onPrimary = Background,
    primaryContainer = SurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = SilverDim,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = RecordRed,
    onTertiary = OnRecordRed,
    tertiaryContainer = ActiveContainer,
    onTertiaryContainer = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    // Cards read surfaceContainer directly; unset would leak Material3's default
    // purple tint onto an all-neutral surface.
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF141210),
    surfaceContainer = Surface,
    surfaceContainerHigh = Color(0xFF232120),
    surfaceContainerHighest = Color(0xFF2B2826),
    outline = SilverDim,
    outlineVariant = Color(0xFF3A3835),
    error = RecordRed,
    onError = OnRecordRed,
    errorContainer = ActiveContainer,
    onErrorContainer = TextPrimary
)

@Composable
fun CassetteCatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OwnedDeviceColorScheme,
        typography = Typography,
        content = content
    )
}
