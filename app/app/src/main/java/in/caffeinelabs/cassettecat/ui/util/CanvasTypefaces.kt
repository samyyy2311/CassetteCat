package `in`.caffeinelabs.cassettecat.ui.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import `in`.caffeinelabs.cassettecat.R as AppR

data class CanvasTypefaces(
    val spaceGroteskBold: Typeface,
    val spaceGroteskSemiBold: Typeface,
    val ibmPlexMono: Typeface,
    val ibmPlexSans: Typeface
)

// Shared by the share-card canvases (Now Playing screenshot, lyric card, Rewind poster):
// same fonts, same runCatching fallback-to-system-typeface behavior.
fun loadCanvasTypefaces(context: Context): CanvasTypefaces {
    val spaceGroteskBold = runCatching {
        val font = ResourcesCompat.getFont(context, AppR.font.space_grotesk_variable)
        Typeface.create(font ?: Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }.getOrNull() ?: Typeface.DEFAULT_BOLD

    val spaceGroteskSemiBold = runCatching {
        val font = ResourcesCompat.getFont(context, AppR.font.space_grotesk_variable)
        Typeface.create(font ?: Typeface.DEFAULT, Typeface.BOLD)
    }.getOrNull() ?: Typeface.DEFAULT

    val ibmPlexMono = runCatching {
        ResourcesCompat.getFont(context, AppR.font.ibm_plex_mono_regular)
    }.getOrNull() ?: Typeface.MONOSPACE

    val ibmPlexSans = runCatching {
        val font = ResourcesCompat.getFont(context, AppR.font.ibm_plex_sans_variable)
        Typeface.create(font ?: Typeface.DEFAULT, Typeface.NORMAL)
    }.getOrNull() ?: Typeface.DEFAULT

    return CanvasTypefaces(spaceGroteskBold, spaceGroteskSemiBold, ibmPlexMono, ibmPlexSans)
}
