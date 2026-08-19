package `in`.caffeinelabs.cassettecat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import `in`.caffeinelabs.cassettecat.data.settings.ThemeAccent

@Composable
fun CassetteCatTheme(
    accent: ThemeAccent = ThemeAccent.RECORD_RED,
    customAccentColor: Long = ThemeAccent.RECORD_RED.colorValue,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val accentColor = if (accent == ThemeAccent.CUSTOM) Color(customAccentColor) else Color(accent.colorValue)
    val activeContainer = if (accent == ThemeAccent.CUSTOM) {
        Color(
            red = accentColor.red * 0.32f,
            green = accentColor.green * 0.32f,
            blue = accentColor.blue * 0.32f
        )
    } else {
        Color(accent.containerValue)
    }

    val backgroundColor = if (isAmoled) Color(0xFF000000) else Background
    val surfaceColor = if (isAmoled) Color(0xFF080706) else Surface
    val surfaceVariantColor = if (isAmoled) Color(0xFF12100E) else SurfaceVariant

    val colorScheme = darkColorScheme(
        primary = Silver,
        onPrimary = backgroundColor,
        primaryContainer = surfaceVariantColor,
        onPrimaryContainer = TextPrimary,
        secondary = SilverDim,
        onSecondary = TextPrimary,
        secondaryContainer = surfaceVariantColor,
        onSecondaryContainer = TextPrimary,
        tertiary = accentColor,
        onTertiary = OnRecordRed,
        tertiaryContainer = activeContainer,
        onTertiaryContainer = TextPrimary,
        background = backgroundColor,
        onBackground = TextPrimary,
        surface = surfaceColor,
        onSurface = TextPrimary,
        surfaceVariant = surfaceVariantColor,
        onSurfaceVariant = TextSecondary,
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = if (isAmoled) Color(0xFF040404) else Color(0xFF141210),
        surfaceContainer = surfaceColor,
        surfaceContainerHigh = if (isAmoled) Color(0xFF161412) else Color(0xFF232120),
        surfaceContainerHighest = if (isAmoled) Color(0xFF1E1C1A) else Color(0xFF2B2826),
        outline = SilverDim,
        outlineVariant = Color(0xFF3A3835),
        error = accentColor,
        onError = OnRecordRed,
        errorContainer = activeContainer,
        onErrorContainer = TextPrimary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
