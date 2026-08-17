package `in`.caffeinelabs.cassettecat.ui.theme

import `in`.caffeinelabs.cassettecat.R
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

// Space Grotesk: general UI text (headings, body, labels). Variable font (wght
// axis), pinned to Regular and SemiBold rather than every named cut.
@OptIn(ExperimentalTextApi::class)
val SpaceGroteskFontFamily = FontFamily(
    Font(
        resId = R.font.space_grotesk_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.space_grotesk_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    )
)


// IBM Plex Sans/Mono: alternate pairing, kept alongside Space Grotesk/Mono for a
// live side-by-side comparison. Not the active choice yet, see Type.kt.
@OptIn(ExperimentalTextApi::class)
val IbmPlexSansFontFamily = FontFamily(
    Font(
        resId = R.font.ibm_plex_sans_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.ibm_plex_sans_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    )
)

val IbmPlexMonoFontFamily = FontFamily(
    Font(resId = R.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.ibm_plex_mono_semibold, weight = FontWeight.Bold)
)
