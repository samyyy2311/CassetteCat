package `in`.caffeinelabs.cassettecat.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import `in`.caffeinelabs.cassettecat.data.settings.ThemeAccent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

fun dominantArtworkAccent(bitmap: Bitmap): Long? {
    val pixels = IntArray(16 * 16)
    for (y in 0 until 16) {
        for (x in 0 until 16) {
            pixels[y * 16 + x] = bitmap.getPixel(
                x * (bitmap.width - 1).coerceAtLeast(0) / 15,
                y * (bitmap.height - 1).coerceAtLeast(0) / 15
            )
        }
    }
    return artworkAccentFromPixels(pixels)
}

internal fun artworkAccentFromPixels(pixels: IntArray): Long? {
    val weights = DoubleArray(12)
    val red = DoubleArray(12)
    val green = DoubleArray(12)
    val blue = DoubleArray(12)

    pixels.forEach { argb ->
        if ((argb ushr 24) < 128) return@forEach
        val r = (argb ushr 16 and 0xFF) / 255.0
        val g = (argb ushr 8 and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        val high = max(r, max(g, b))
        val low = min(r, min(g, b))
        val delta = high - low
        val saturation = if (high == 0.0) 0.0 else delta / high
        if (saturation < 0.2 || high < 0.18) return@forEach
        val hue = when (high) {
            r -> 60.0 * (((g - b) / delta) % 6.0)
            g -> 60.0 * ((b - r) / delta + 2.0)
            else -> 60.0 * ((r - g) / delta + 4.0)
        }.let { if (it < 0) it + 360.0 else it }
        val bin = (hue / 30.0).toInt().coerceIn(0, 11)
        val weight = saturation * saturation * (1.0 - abs(high - 0.68) * 0.35)
        weights[bin] += weight
        red[bin] += r * weight
        green[bin] += g * weight
        blue[bin] += b * weight
    }

    val bin = weights.indices.maxByOrNull(weights::get)?.takeIf { weights[it] > 0.0 } ?: return null
    var r = red[bin] / weights[bin]
    var g = green[bin] / weights[bin]
    var b = blue[bin] / weights[bin]
    val high = max(r, max(g, b))
    val low = min(r, min(g, b))
    val saturation = (high - low) / high
    val targetSaturation = max(0.58, saturation)
    r = high - (high - r) * targetSaturation / saturation
    g = high - (high - g) * targetSaturation / saturation
    b = high - (high - b) * targetSaturation / saturation
    val valueScale = 0.72 / max(r, max(g, b))
    r *= valueScale
    g *= valueScale
    b *= valueScale
    val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    val contrastScale = if (luminance > 0.46) 0.46 / luminance else 1.0

    fun channel(value: Double) = (value * contrastScale * 255.0).toInt().coerceIn(0, 255)
    return 0xFF000000L or (channel(r).toLong() shl 16) or (channel(g).toLong() shl 8) or channel(b).toLong()
}
