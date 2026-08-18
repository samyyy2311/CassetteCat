package `in`.caffeinelabs.cassettecat.ui.screens.stats

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

internal fun buildListeningRecordPoster(
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
        color = android.graphics.Color.argb(32, 255, 244, 237)
    }
    canvas.drawCircle(920f, 150f, 250f, accentPaint)
    canvas.drawCircle(120f, 940f, 180f, accentPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F5F0EC")
        textSize = 64f
        typeface = Typeface.DEFAULT_BOLD
    }

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F5F0EC")
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
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
        typeface = Typeface.DEFAULT
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
        }
        canvas.drawText("ON REPEAT", 64f, 870f, sectionLabelPaint)

        val artistsPaint = Paint(subHeroPaint).apply { textSize = 34f }
        val artistsStr = topArtists.take(3).joinToString("  •  ")
        val truncatedArtists = truncateText(artistsPaint, artistsStr, size - 128f)
        canvas.drawText(truncatedArtists, 64f, 930f, artistsPaint)
    }

    return bitmap
}

internal fun shareListeningRecordPoster(context: Context, bitmap: Bitmap, title: String) {
    val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(dir, "listening_record_${title.replace(' ', '_')}.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun truncateText(paint: Paint, text: String, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var truncated = text
    while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
        truncated = truncated.dropLast(1)
    }
    return if (truncated.isEmpty()) text else "$truncated..."
}
