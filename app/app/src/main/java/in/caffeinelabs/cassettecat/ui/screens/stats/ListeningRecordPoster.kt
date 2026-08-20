package `in`.caffeinelabs.cassettecat.ui.screens.stats

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.ShareActionPill
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.shareImageWithApp
import android.content.Context
import java.util.Locale

internal fun buildListeningRecordPoster(
    context: Context,
    monthLabel: String,
    yearLabel: String,
    listeningMinutes: Long,
    totalPlays: Int,
    uniqueSongs: Int,
    topArtists: List<String>,
    topSongs: List<Triple<String, String, Int>> = emptyList(),
    isRewind: Boolean = false
): Bitmap {
    val size = 1080
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val ibmPlexSansBold = runCatching {
        val font = ResourcesCompat.getFont(context, AppR.font.ibm_plex_sans_variable)
        Typeface.create(font ?: Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }.getOrNull() ?: Typeface.DEFAULT_BOLD
    val ibmPlexMono = runCatching {
        ResourcesCompat.getFont(context, AppR.font.ibm_plex_mono_regular)
    }.getOrNull() ?: Typeface.MONOSPACE
    val ibmPlexSansRegular = runCatching {
        val font = ResourcesCompat.getFont(context, AppR.font.ibm_plex_sans_variable)
        Typeface.create(font ?: Typeface.DEFAULT, Typeface.NORMAL)
    }.getOrNull() ?: Typeface.DEFAULT

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(
                android.graphics.Color.parseColor("#C23B30"),
                android.graphics.Color.parseColor("#321319"),
                android.graphics.Color.parseColor("#09090B")
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(28, 255, 244, 237)
    }
    canvas.drawCircle(960f, 100f, 320f, accentPaint)
    canvas.drawCircle(1000f, 260f, 150f, Paint(accentPaint).apply { alpha = 20 })
    canvas.drawCircle(60f, 1000f, 240f, accentPaint)
    canvas.drawCircle(-40f, 780f, 130f, Paint(accentPaint).apply { alpha = 16 })

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F5F0EC")
        textSize = 64f
        typeface = ibmPlexSansBold
    }

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F5F0EC")
        textSize = 30f
        typeface = ibmPlexMono
        letterSpacing = 0.14f
    }
    canvas.drawText("CASSETTECAT", 64f, 108f, headerPaint)

    val recapSubtitle = if (isRewind) "Your annual rewind" else "Your listening recap"
    canvas.drawText(recapSubtitle, 64f, 178f, titlePaint)

    val heroText = if (isRewind) yearLabel else monthLabel.take(3).uppercase(Locale.US)
    val heroPaint = Paint(titlePaint).apply { textSize = 178f }
    canvas.drawText(heroText, 64f, 370f, heroPaint)

    val subHeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        color = android.graphics.Color.parseColor("#A8A29A")
        typeface = ibmPlexSansRegular
    }
    val subHeroText = if (isRewind) "ANNUAL RECAP" else yearLabel
    canvas.drawText(subHeroText, 70f, 438f, subHeroPaint)

    val statValuePaint = Paint(titlePaint).apply { textSize = 56f }
    val statLabelPaint = Paint(subHeroPaint).apply { textSize = 36f }

    canvas.drawText(formatListeningTime(listeningMinutes), 64f, 620f, statValuePaint)
    canvas.drawText("listened", 64f, 675f, statLabelPaint)

    val playsText = "$totalPlays plays  ·  $uniqueSongs tracks"
    canvas.drawText(playsText, 64f, 770f, Paint(titlePaint).apply { textSize = 38f })

    if (topArtists.isNotEmpty()) {
        val sectionLabelPaint = Paint(subHeroPaint).apply {
            textSize = 26f
            letterSpacing = 0.12f
            typeface = ibmPlexMono
        }
        canvas.drawText("ON REPEAT", 64f, 870f, sectionLabelPaint)

        val artistsPaint = Paint(subHeroPaint).apply { textSize = 34f }
        val artistsStr = topArtists.take(3).joinToString("  •  ")
        val truncatedArtists = truncateText(artistsPaint, artistsStr, size - 128f)
        canvas.drawText(truncatedArtists, 64f, 930f, artistsPaint)
    }

    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListeningRecordShareSheet(bitmap: Bitmap, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Share", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PressDepthIconButton(iconRes = R.drawable.lucide_ic_x, contentDescription = "Close", onClick = onDismiss)
            }

            Spacer(Modifier.height(16.dp))

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShareActionPill(
                    iconRes = AppR.drawable.ic_logo_whatsapp,
                    label = "WhatsApp",
                    packageNames = listOf("com.whatsapp", "com.whatsapp.w4b"),
                    backgroundColor = Color(0xFF25D366),
                    iconTint = Color.White,
                    onClick = {
                        val targetPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
                            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
                        } ?: "com.whatsapp"
                        shareImageWithApp(context, bitmap, title, targetPkg)
                    }
                )
                ShareActionPill(
                    iconRes = AppR.drawable.ic_logo_instagram,
                    label = "Stories",
                    packageNames = listOf("com.instagram.android"),
                    backgroundBrush = Brush.linearGradient(
                        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFF77737))
                    ),
                    iconTint = Color.White,
                    onClick = { shareImageWithApp(context, bitmap, title, "com.instagram.android") }
                )
                ShareActionPill(
                    iconRes = R.drawable.lucide_ic_share_2,
                    label = "More",
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    onClick = { shareImageWithApp(context, bitmap, title, null) }
                )
            }
        }
    }
}

private fun truncateText(paint: Paint, text: String, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var truncated = text
    while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
        truncated = truncated.dropLast(1)
    }
    return if (truncated.isEmpty()) text else "$truncated..."
}
