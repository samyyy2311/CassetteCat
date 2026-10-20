package `in`.caffeinelabs.cassettecat.ui.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import `in`.caffeinelabs.cassettecat.R as AppR

data class CanvasTypefaces(
    val spaceGroteskBold: Typeface,
    val spaceGroteskSemiBold: Typeface,
    val ibmPlexMono: Typeface,
    val ibmPlexSans: Typeface,
    val spaceGroteskMedium: Typeface
)

fun loadCanvasTypefaces(context: Context): CanvasTypefaces {
    val spaceGroteskBold = runCatching {
        ResourcesCompat.getFont(context, AppR.font.space_grotesk_bold)
    }.getOrNull() ?: Typeface.DEFAULT_BOLD

    val spaceGroteskSemiBold = runCatching {
        ResourcesCompat.getFont(context, AppR.font.space_grotesk_semibold)
    }.getOrNull() ?: Typeface.DEFAULT

    val ibmPlexMono = runCatching {
        ResourcesCompat.getFont(context, AppR.font.ibm_plex_mono_regular)
    }.getOrNull() ?: Typeface.MONOSPACE

    val ibmPlexSans = runCatching {
        ResourcesCompat.getFont(context, AppR.font.ibm_plex_sans_regular)
    }.getOrNull() ?: Typeface.DEFAULT

    // The fonts are static, one file per weight, so each weight is loaded rather than set as a variation.
    val spaceGroteskMedium = runCatching {
        ResourcesCompat.getFont(context, AppR.font.space_grotesk_medium)
    }.getOrNull() ?: Typeface.DEFAULT

    return CanvasTypefaces(spaceGroteskBold, spaceGroteskSemiBold, ibmPlexMono, ibmPlexSans, spaceGroteskMedium)
}
