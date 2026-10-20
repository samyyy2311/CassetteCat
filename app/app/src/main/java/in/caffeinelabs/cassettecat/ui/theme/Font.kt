package `in`.caffeinelabs.cassettecat.ui.theme

import `in`.caffeinelabs.cassettecat.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// One file per weight: the variable fonts were drawn at their default weight, Light for Space Grotesk, so bold
// text came out thin.
val SpaceGroteskFontFamily = FontFamily(
    Font(resId = R.font.space_grotesk_light, weight = FontWeight.Light),
    Font(resId = R.font.space_grotesk_regular, weight = FontWeight.Normal),
    Font(resId = R.font.space_grotesk_medium, weight = FontWeight.Medium),
    Font(resId = R.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.space_grotesk_bold, weight = FontWeight.Bold)
)

val IbmPlexSansFontFamily = FontFamily(
    Font(resId = R.font.ibm_plex_sans_regular, weight = FontWeight.Normal),
    Font(resId = R.font.ibm_plex_sans_medium, weight = FontWeight.Medium),
    Font(resId = R.font.ibm_plex_sans_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.ibm_plex_sans_bold, weight = FontWeight.Bold)
)

val IbmPlexMonoFontFamily = FontFamily(
    Font(resId = R.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.ibm_plex_mono_regular, weight = FontWeight.Medium),
    Font(resId = R.font.ibm_plex_mono_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.ibm_plex_mono_semibold, weight = FontWeight.Bold)
)

val SilkscreenFontFamily = FontFamily(
    Font(resId = R.font.silkscreen_regular, weight = FontWeight.Normal),
    Font(resId = R.font.silkscreen_regular, weight = FontWeight.Medium),
    Font(resId = R.font.silkscreen_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.silkscreen_bold, weight = FontWeight.Bold)
)

val VT323FontFamily = FontFamily(
    Font(resId = R.font.vt323_regular, weight = FontWeight.Normal),
    Font(resId = R.font.vt323_regular, weight = FontWeight.Medium),
    Font(resId = R.font.vt323_regular, weight = FontWeight.SemiBold),
    Font(resId = R.font.vt323_regular, weight = FontWeight.Bold)
)

val MonocraftFontFamily = FontFamily(
    Font(resId = R.font.monocraft, weight = FontWeight.Normal),
    Font(resId = R.font.monocraft, weight = FontWeight.Medium),
    Font(resId = R.font.monocraft, weight = FontWeight.SemiBold),
    Font(resId = R.font.monocraft, weight = FontWeight.Bold)
)
