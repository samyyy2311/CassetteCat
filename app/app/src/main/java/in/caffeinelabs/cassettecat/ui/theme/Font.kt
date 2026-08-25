package `in`.caffeinelabs.cassettecat.ui.theme

import `in`.caffeinelabs.cassettecat.R
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

// Google Fonts Downloadable Font Provider
val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Space Grotesk (Bundled offline variable font)
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

// IBM Plex Sans (Bundled offline variable font)
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

// IBM Plex Mono (Bundled offline font)
val IbmPlexMonoFontFamily = FontFamily(
    Font(resId = R.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.ibm_plex_mono_semibold, weight = FontWeight.Bold)
)

// Outfit: Modern, rounded geometric sans
val OutfitFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
)

// Inter: Clean, crisp standard digital UI font
val InterFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
)

// Plus Jakarta Sans: Contemporary neo-grotesque with subtle friendly curves
val PlusJakartaSansFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
)

// Silkscreen: 8-bit retro arcade / pixel font
val SilkscreenFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Silkscreen"), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Silkscreen"), fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
)
