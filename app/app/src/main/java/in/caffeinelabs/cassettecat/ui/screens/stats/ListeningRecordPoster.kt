package `in`.caffeinelabs.cassettecat.ui.screens.stats

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.ShareActionPill
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.shareImageWithApp
import java.util.Locale

internal fun buildListeningRecordPoster(
    context: Context,
    monthAbbreviation: String,
    yearLabel: String,
    listeningMinutes: Long,
    totalPlays: Int,
    uniqueSongs: Int,
    topArtists: List<String>,
    topSongs: List<Triple<String, String, Int>> = emptyList(),
    isRewind: Boolean = false
): Bitmap {
    val size = 1080
    val bitmap = createBitmap(size, size)
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

    val cream = "#FFF7EE".toColorInt()
    val muted = "#B8AAA5".toColorInt()
    val accent = "#FF5A49".toColorInt()
    val panel = "#24FFFFFF".toColorInt()

    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            intArrayOf("#09090C".toColorInt(), "#201015".toColorInt(), "#72231F".toColorInt()),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP
        )
    })
    canvas.drawCircle(980f, 920f, 420f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            980f,
            920f,
            420f,
            intArrayOf("#50FF5A49".toColorInt(), android.graphics.Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
    })
    canvas.drawCircle(60f, 80f, 280f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            60f,
            80f,
            280f,
            intArrayOf("#2EFFB19F".toColorInt(), android.graphics.Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
    })

    val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(11, 255, 255, 255)
        strokeWidth = 1f
    }
    for (offset in -size until size * 2 step 48) {
        canvas.drawLine(offset.toFloat(), 0f, (offset - size).toFloat(), size.toFloat(), texturePaint)
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cream
        typeface = ibmPlexSansBold
    }
    val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        typeface = ibmPlexMono
        letterSpacing = 0.12f
    }

    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRoundRect(64f, 58f, 124f, 100f, 9f, 9f, iconPaint)
    canvas.drawCircle(82f, 79f, 7f, iconPaint)
    canvas.drawCircle(106f, 79f, 7f, iconPaint)
    canvas.drawLine(82f, 92f, 106f, 92f, iconPaint)

    canvas.drawText(context.getString(AppR.string.app_name).uppercase(Locale.getDefault()), 148f, 88f, Paint(monoPaint).apply {
        color = cream
        textSize = 27f
    })
    val localizedMonth = monthAbbreviation.uppercase(Locale.getDefault())
    val periodLabel = if (isRewind) {
        context.getString(AppR.string.poster_period_annual, yearLabel)
    } else {
        context.getString(AppR.string.poster_period_month, localizedMonth, yearLabel)
    }
    canvas.drawText(periodLabel, 1016f, 88f, Paint(monoPaint).apply {
        color = cream
        textSize = 25f
        textAlign = Paint.Align.RIGHT
    })
    canvas.drawLine(64f, 136f, 1016f, 136f, Paint(texturePaint).apply { alpha = 44 })

    canvas.drawText(
        context.getString(if (isRewind) AppR.string.poster_year_in_music else AppR.string.poster_month_in_music),
        64f,
        202f,
        Paint(monoPaint).apply { color = accent; textSize = 25f }
    )

    val heroText = if (isRewind) yearLabel else localizedMonth
    canvas.drawText(heroText, 56f, 372f, Paint(titlePaint).apply { textSize = if (isRewind) 150f else 184f })
    if (!isRewind) {
        canvas.drawText(yearLabel, 1016f, 354f, Paint(monoPaint).apply {
            color = cream
            textSize = 43f
            textAlign = Paint.Align.RIGHT
        })
    }

    canvas.drawText(context.getString(AppR.string.poster_total_listening_time), 64f, 434f, Paint(monoPaint).apply { textSize = 23f })
    val listeningTime = formatListeningTime(listeningMinutes)
    val listeningTimePaint = Paint(titlePaint).apply {
        textSize = 112f
        val width = measureText(listeningTime)
        if (width > 952f) textSize *= 952f / width
    }
    canvas.drawText(listeningTime, 64f, 555f, listeningTimePaint)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = panel }
    val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(42, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    fun drawStatCard(left: Float, right: Float, label: String, value: String) {
        canvas.drawRoundRect(left, 610f, right, 748f, 24f, 24f, cardPaint)
        canvas.drawRoundRect(left, 610f, right, 748f, 24f, 24f, cardStroke)
        canvas.drawText(label, left + 28f, 652f, Paint(monoPaint).apply { textSize = 21f })
        canvas.drawText(value, left + 28f, 714f, Paint(titlePaint).apply { textSize = 50f })
    }
    drawStatCard(64f, 306f, context.getString(AppR.string.poster_plays), totalPlays.toString())
    drawStatCard(326f, 568f, context.getString(AppR.string.poster_tracks), uniqueSongs.toString())

    canvas.drawRoundRect(588f, 610f, 1016f, 748f, 24f, 24f, cardPaint)
    canvas.drawRoundRect(588f, 610f, 1016f, 748f, 24f, 24f, cardStroke)
    canvas.drawText(context.getString(AppR.string.poster_play_frequency), 616f, 652f, Paint(monoPaint).apply { textSize = 21f })
    val maxPlayCount = topSongs.maxOfOrNull { it.third }?.coerceAtLeast(1) ?: 1
    repeat(5) { index ->
        val playCount = topSongs.getOrNull(index)?.third ?: 0
        val height = 18f + 56f * playCount / maxPlayCount
        val left = 620f + index * 70f
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (index == 0) accent else android.graphics.Color.argb(115, 255, 247, 238)
        }
        canvas.drawRoundRect(left, 722f - height, left + 42f, 722f, 12f, 12f, barPaint)
    }

    canvas.drawRoundRect(64f, 786f, 1016f, 1016f, 30f, 30f, Paint(cardPaint).apply { alpha = 205 })
    canvas.drawRoundRect(64f, 786f, 1016f, 1016f, 30f, 30f, cardStroke)
    canvas.drawText(context.getString(AppR.string.poster_on_repeat), 96f, 836f, Paint(monoPaint).apply { color = accent; textSize = 22f })

    val leadArtistPaint = Paint(titlePaint).apply { textSize = 54f }
    val leadArtist = topArtists.firstOrNull() ?: context.getString(AppR.string.poster_no_repeats)
    canvas.drawText(truncateText(leadArtistPaint, leadArtist, 720f), 96f, 890f, leadArtistPaint)
    canvas.drawText("01", 982f, 888f, Paint(monoPaint).apply {
        color = android.graphics.Color.argb(90, 255, 247, 238)
        textSize = 54f
        textAlign = Paint.Align.RIGHT
    })

    val otherArtists = topArtists.drop(1).take(2).joinToString(" / ")
    if (otherArtists.isNotEmpty()) {
        val otherArtistsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 23f
            typeface = ibmPlexSansRegular
        }
        canvas.drawText(truncateText(otherArtistsPaint, otherArtists, 888f), 96f, 922f, otherArtistsPaint)
    }

    canvas.drawLine(96f, 944f, 984f, 944f, Paint(texturePaint).apply { alpha = 42 })
    val topSong = topSongs.firstOrNull()
    if (topSong != null) {
        canvas.drawText(context.getString(AppR.string.poster_top_track), 96f, 988f, Paint(monoPaint).apply { textSize = 19f })
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 29f
            typeface = ibmPlexSansRegular
        }
        canvas.drawText(truncateText(trackPaint, "${topSong.first} / ${topSong.second}", 570f), 270f, 990f, trackPaint)
        val playCount = context.resources.getQuantityString(AppR.plurals.poster_play_count, topSong.third, topSong.third)
        canvas.drawText(playCount, 984f, 988f, Paint(monoPaint).apply {
            color = cream
            textSize = 20f
            textAlign = Paint.Align.RIGHT
        })
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
                Text(text = stringResource(AppR.string.share_listening_record), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_x,
                    contentDescription = stringResource(AppR.string.close),
                    onClick = onDismiss
                )
            }

            Spacer(Modifier.height(16.dp))

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .shadow(18.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShareActionPill(
                    iconRes = AppR.drawable.ic_logo_whatsapp,
                    label = stringResource(AppR.string.share_whatsapp),
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
                    label = stringResource(AppR.string.share_stories),
                    packageNames = listOf("com.instagram.android"),
                    backgroundBrush = Brush.linearGradient(
                        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFF77737))
                    ),
                    iconTint = Color.White,
                    onClick = { shareImageWithApp(context, bitmap, title, "com.instagram.android") }
                )
                ShareActionPill(
                    iconRes = R.drawable.lucide_ic_share_2,
                    label = stringResource(AppR.string.share_more),
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
