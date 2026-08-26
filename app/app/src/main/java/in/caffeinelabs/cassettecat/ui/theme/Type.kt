package `in`.caffeinelabs.cassettecat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.cassettecat.data.settings.AppFontFamily

fun resolveFontFamily(family: AppFontFamily): FontFamily {
    return when (family) {
        AppFontFamily.SPACE_GROTESK -> SpaceGroteskFontFamily
        AppFontFamily.IBM_PLEX_SANS -> IbmPlexSansFontFamily
        AppFontFamily.IBM_PLEX_MONO -> IbmPlexMonoFontFamily
        AppFontFamily.SILKSCREEN -> SilkscreenFontFamily
        AppFontFamily.VT323 -> VT323FontFamily
        AppFontFamily.MONOCRAFT -> MonocraftFontFamily
        AppFontFamily.SYSTEM_DEFAULT -> FontFamily.SansSerif
        AppFontFamily.SYSTEM_SERIF -> FontFamily.Serif
        AppFontFamily.SYSTEM_MONO -> FontFamily.Monospace
    }
}

fun createAppTypography(appFontFamily: AppFontFamily): Typography {
    val font = resolveFontFamily(appFontFamily)
    val (scale, tracking) = when (appFontFamily) {
        AppFontFamily.SILKSCREEN, AppFontFamily.MONOCRAFT -> 0.94f to (-0.2).sp
        AppFontFamily.VT323 -> 0.96f to (-0.1).sp
        AppFontFamily.IBM_PLEX_MONO, AppFontFamily.SYSTEM_MONO -> 0.94f to (-0.1).sp
        else -> 1.0f to 0.sp
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = (57 * scale).sp,
            lineHeight = (64 * scale).sp,
            letterSpacing = tracking
        ),
        displayMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = (45 * scale).sp,
            lineHeight = (52 * scale).sp,
            letterSpacing = tracking
        ),
        displaySmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = (36 * scale).sp,
            lineHeight = (44 * scale).sp,
            letterSpacing = tracking
        ),
        headlineLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = (32 * scale).sp,
            lineHeight = (40 * scale).sp,
            letterSpacing = tracking
        ),
        headlineMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = (28 * scale).sp,
            lineHeight = (36 * scale).sp,
            letterSpacing = tracking
        ),
        headlineSmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = (24 * scale).sp,
            lineHeight = (32 * scale).sp,
            letterSpacing = tracking
        ),
        titleLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = tracking
        ),
        titleMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = tracking
        ),
        titleSmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = tracking
        ),
        bodyLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = tracking
        ),
        bodyMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = tracking
        ),
        bodySmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = tracking
        ),
        labelLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = tracking
        ),
        labelMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = tracking
        ),
        labelSmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = tracking
        )
    )
}

val Typography = createAppTypography(AppFontFamily.SPACE_GROTESK)
