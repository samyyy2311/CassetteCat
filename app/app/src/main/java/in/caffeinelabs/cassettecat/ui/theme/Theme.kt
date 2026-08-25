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
    appFontFamily: `in`.caffeinelabs.cassettecat.data.settings.AppFontFamily = `in`.caffeinelabs.cassettecat.data.settings.AppFontFamily.SPACE_GROTESK,
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

    val typography = androidx.compose.runtime.remember(appFontFamily) {
        createAppTypography(appFontFamily)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
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
    return normalizeArtworkAccent(r, g, b)
}

internal fun normalizeArtworkAccent(initialRed: Double, initialGreen: Double, initialBlue: Double): Long {
    var r = initialRed
    var g = initialGreen
    var b = initialBlue
    val high = max(r, max(g, b))
    val low = min(r, min(g, b))
    val saturation = if (high > 0.0) (high - low) / high else 0.0
    if (!saturation.isFinite() || saturation <= 0.0) return ThemeAccent.RECORD_RED.colorValue
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

data class ArtworkAtmospherePalette(
    val topColor: Color,
    val centerColor: Color,
    val accentColor: Color,
    val darkBase: Color = Color(0xFF09080B),
    val isMonochrome: Boolean = false
)

private fun adjustAtmosphereColor(r: Double, g: Double, b: Double, targetLuminance: Double): Color {
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val sat = if (maxC > 0.0) delta / maxC else 0.0

    // Enhance natural saturation slightly for rich dark tones without distorting hue
    val targetSat = if (sat > 0.05) (sat * 1.30).coerceIn(0.28, 0.72) else 0.0
    var adjR = r
    var adjG = g
    var adjB = b

    if (sat > 0.001 && targetSat > 0.001) {
        adjR = maxC - (maxC - r) * (targetSat / sat)
        adjG = maxC - (maxC - g) * (targetSat / sat)
        adjB = maxC - (maxC - b) * (targetSat / sat)
    }

    // Scale to target luminance for atmospheric depth
    val curLum = 0.2126 * adjR + 0.7152 * adjG + 0.0722 * adjB
    val lumScale = if (curLum > 0.001) (targetLuminance / curLum).coerceIn(0.35, 2.2) else 1.0

    val finalR = (adjR * lumScale).coerceIn(0.0, 1.0).toFloat()
    val finalG = (adjG * lumScale).coerceIn(0.0, 1.0).toFloat()
    val finalB = (adjB * lumScale).coerceIn(0.0, 1.0).toFloat()

    return Color(finalR, finalG, finalB)
}

fun extractArtworkAtmospherePalette(bitmap: Bitmap): ArtworkAtmospherePalette {
    val sampleW = 24
    val sampleH = 24
    var totalR = 0.0
    var totalG = 0.0
    var totalB = 0.0
    var totalSat = 0.0
    var validCount = 0

    var topR = 0.0
    var topG = 0.0
    var topB = 0.0
    var topWeight = 0.0

    var bottomR = 0.0
    var bottomG = 0.0
    var bottomB = 0.0
    var bottomWeight = 0.0

    var maxSat = 0.0
    var accentR = 0.0
    var accentG = 0.0
    var accentB = 0.0

    for (y in 0 until sampleH) {
        val srcY = (y * (bitmap.height - 1) / (sampleH - 1)).coerceIn(0, bitmap.height - 1)
        val isTop = y < sampleH * 0.45
        for (x in 0 until sampleW) {
            val srcX = (x * (bitmap.width - 1) / (sampleW - 1)).coerceIn(0, bitmap.width - 1)
            val argb = bitmap.getPixel(srcX, srcY)
            val alpha = (argb ushr 24) and 0xFF
            if (alpha < 128) continue

            val r = ((argb ushr 16) and 0xFF) / 255.0
            val g = ((argb ushr 8) and 0xFF) / 255.0
            val b = (argb and 0xFF) / 255.0

            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val delta = maxC - minC
            val sat = if (maxC > 0.0) delta / maxC else 0.0
            val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b

            totalR += r
            totalG += g
            totalB += b
            totalSat += sat
            validCount++

            val weight = (1.0 + sat * 2.5) * (1.0 - abs(lum - 0.5) * 0.7).coerceAtLeast(0.1)

            if (isTop) {
                topR += r * weight
                topG += g * weight
                topB += b * weight
                topWeight += weight
            } else {
                bottomR += r * weight
                bottomG += g * weight
                bottomB += b * weight
                bottomWeight += weight
            }

            if (sat > maxSat && lum in 0.15..0.85) {
                maxSat = sat
                accentR = r
                accentG = g
                accentB = b
            }
        }
    }

    if (validCount == 0) {
        return ArtworkAtmospherePalette(
            topColor = Color(0xFF1E1E24),
            centerColor = Color(0xFF141418),
            accentColor = Color(0xFF282832),
            darkBase = Color(0xFF09080B),
            isMonochrome = true
        )
    }

    val avgSat = totalSat / validCount
    val isMonochrome = avgSat < 0.12 && maxSat < 0.20

    if (isMonochrome) {
        val avgLum = (0.2126 * (totalR / validCount) + 0.7152 * (totalG / validCount) + 0.0722 * (totalB / validCount)).coerceIn(0.12, 0.45)
        val topL = (avgLum * 0.75).coerceIn(0.08, 0.24).toFloat()
        val centerL = (avgLum * 0.45).coerceIn(0.05, 0.15).toFloat()
        return ArtworkAtmospherePalette(
            topColor = Color(topL, (topL * 1.01f).coerceAtMost(1f), (topL * 1.03f).coerceAtMost(1f)),
            centerColor = Color(centerL, (centerL * 1.01f).coerceAtMost(1f), (centerL * 1.02f).coerceAtMost(1f)),
            accentColor = Color(0xFF26262E),
            darkBase = Color(0xFF08080A),
            isMonochrome = true
        )
    }

    val rawTopR = if (topWeight > 0) topR / topWeight else totalR / validCount
    val rawTopG = if (topWeight > 0) topG / topWeight else totalG / validCount
    val rawTopB = if (topWeight > 0) topB / topWeight else totalB / validCount
    val topColor = adjustAtmosphereColor(rawTopR, rawTopG, rawTopB, targetLuminance = 0.25)

    val rawBottomR = if (bottomWeight > 0) bottomR / bottomWeight else totalR / validCount
    val rawBottomG = if (bottomWeight > 0) bottomG / bottomWeight else totalG / validCount
    val rawBottomB = if (bottomWeight > 0) bottomB / bottomWeight else totalB / validCount
    val centerColor = adjustAtmosphereColor(rawBottomR, rawBottomG, rawBottomB, targetLuminance = 0.16)

    val rawAccR = if (maxSat > 0) accentR else rawTopR
    val rawAccG = if (maxSat > 0) accentG else rawTopG
    val rawAccB = if (maxSat > 0) accentB else rawTopB
    val accentColor = adjustAtmosphereColor(rawAccR, rawAccG, rawAccB, targetLuminance = 0.32)

    return ArtworkAtmospherePalette(
        topColor = topColor,
        centerColor = centerColor,
        accentColor = accentColor,
        darkBase = Color(0xFF09080B),
        isMonochrome = false
    )
}
