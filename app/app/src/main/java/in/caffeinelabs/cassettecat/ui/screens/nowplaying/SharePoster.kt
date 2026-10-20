package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.util.CanvasTypefaces
import `in`.caffeinelabs.cassettecat.ui.util.loadCanvasTypefaces

internal fun generateSharePoster(
    context: Context,
    song: Song,
    mode: ShareCardMode,
    lines: List<String>,
    theme: LyricCardTheme,
    artBitmap: Bitmap?
): Bitmap {
    val width = 2160
    val height = 2700
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val cardRadius = 96f
    val cardPath = Path().apply {
        addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), cardRadius, cardRadius, Path.Direction.CW)
    }
    canvas.clipPath(cardPath)

    val typefaces = loadCanvasTypefaces(context)
    val tapeDrawable = ContextCompat.getDrawable(context, R.drawable.lucide_ic_cassette_tape)?.mutate()

    when (theme) {
        LyricCardTheme.ATMOSPHERE -> {
            if (artBitmap != null) {
                val blurred = createFastBlurredBitmap(artBitmap)
                val filterPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    isFilterBitmap = true
                    isDither = true
                }
                canvas.drawBitmap(blurred, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), filterPaint)

                val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(
                            android.graphics.Color.argb(110, 0, 0, 0),
                            android.graphics.Color.argb(225, 10, 9, 8)
                        ),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
            } else {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(
                            "#261E1A".toColorInt(),
                            "#14110F".toColorInt(),
                            "#0A0908".toColorInt()
                        ),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
        }
        LyricCardTheme.OBSIDIAN -> {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
            }
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), cardRadius, cardRadius, bgPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = "#1E1E1E".toColorInt()
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawRoundRect(RectF(3f, 3f, width - 3f, height - 3f), cardRadius, cardRadius, borderPaint)
        }
    }

    if (mode == ShareCardMode.SONG) {
        drawSongCard(canvas, context, song, artBitmap, typefaces, tapeDrawable)
    } else {
        drawLyricCard(canvas, context, song, lines, artBitmap, typefaces, tapeDrawable)
    }

    return bitmap
}

private fun createFastBlurredBitmap(src: Bitmap): Bitmap {
    val downW = 128
    val downH = 160
    val small = src.scale(downW, downH)
    val pixels = IntArray(downW * downH)
    small.getPixels(pixels, 0, downW, 0, 0, downW, downH)
    fastBoxBlur(pixels, downW, downH, 8)
    fastBoxBlur(pixels, downW, downH, 8)
    fastBoxBlur(pixels, downW, downH, 8)
    small.setPixels(pixels, 0, downW, 0, 0, downW, downH)
    return small
}

private fun fastBoxBlur(pixels: IntArray, w: Int, h: Int, radius: Int) {
    val temp = IntArray(pixels.size)
    for (y in 0 until h) {
        val rowOffset = y * w
        for (x in 0 until w) {
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            for (dx in -radius..radius) {
                val nx = (x + dx).coerceIn(0, w - 1)
                val c = pixels[rowOffset + nx]
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
                count++
            }
            temp[rowOffset + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }
    for (x in 0 until w) {
        for (y in 0 until h) {
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            for (dy in -radius..radius) {
                val ny = (y + dy).coerceIn(0, h - 1)
                val c = temp[ny * w + x]
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
                count++
            }
            pixels[y * w + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }
}

private fun drawSongCard(
    canvas: Canvas,
    context: Context,
    song: Song,
    artBitmap: Bitmap?,
    typefaces: CanvasTypefaces,
    tapeDrawable: Drawable?
) {
    val width = canvas.width
    val height = canvas.height
    val (spaceGroteskBold, _, ibmPlexMono, _) = typefaces
    val artSize = 1280f
    val artLeft = (width - artSize) / 2f
    val artTop = 360f
    val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

    if (artBitmap != null) {
        val shader = BitmapShader(artBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix()
        val scale = artSize / minOf(artBitmap.width, artBitmap.height)
        val dx = artLeft - (artBitmap.width * scale - artSize) / 2f
        val dy = artTop - (artBitmap.height * scale - artSize) / 2f
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        shader.setLocalMatrix(matrix)

        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.shader = shader
            isFilterBitmap = true
            isDither = true
        }
        canvas.drawRoundRect(artRect, 72f, 72f, artPaint)
    } else {
        val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#221F1D".toColorInt()
        }
        canvas.drawRoundRect(artRect, 72f, 72f, placeholderPaint)
    }

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 104f
        typeface = spaceGroteskBold
        textAlign = Paint.Align.CENTER
    }
    val safeTitle = if (song.title.length > 32) song.title.take(30) + "…" else song.title
    canvas.drawText(safeTitle, width / 2f, artTop + artSize + 220f, titlePaint)

    val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.argb(190, 255, 255, 255)
        textSize = 68f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }
    val safeArtist = if (song.artist.length > 40) song.artist.take(38) + "…" else song.artist
    canvas.drawText(safeArtist, width / 2f, artTop + artSize + 340f, artistPaint)

    val footerText = context.getString(AppR.string.app_name)
    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.argb(128, 255, 255, 255)
        textSize = 56f
        typeface = ibmPlexMono
    }
    val tapeSize = 72
    val tapeSpacing = 24
    val textWidth = footerPaint.measureText(footerText)
    val totalFooterWidth = tapeSize + tapeSpacing + textWidth
    val footerStartX = (width - totalFooterWidth) / 2f
    val footerY = height - 200f

    if (tapeDrawable != null) {
        tapeDrawable.setTint(android.graphics.Color.argb(128, 255, 255, 255))
        tapeDrawable.setBounds(
            footerStartX.toInt(),
            (footerY - 52).toInt(),
            (footerStartX + tapeSize).toInt(),
            (footerY - 52 + tapeSize).toInt()
        )
        tapeDrawable.draw(canvas)
    }
    canvas.drawText(footerText, footerStartX + tapeSize + tapeSpacing, footerY, footerPaint)
}

private fun drawLyricCard(
    canvas: Canvas,
    context: Context,
    song: Song,
    lines: List<String>,
    artBitmap: Bitmap?,
    typefaces: CanvasTypefaces,
    tapeDrawable: Drawable?
) {
    val width = canvas.width
    val height = canvas.height
    val (_, spaceGroteskSemiBold, ibmPlexMono, _, spaceGroteskMedium) = typefaces
    val thumbSize = 240f
    val thumbLeft = 180f
    val thumbTop = 160f
    val thumbRect = RectF(thumbLeft, thumbTop, thumbLeft + thumbSize, thumbTop + thumbSize)

    if (artBitmap != null) {
        val shader = BitmapShader(artBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix()
        val scale = thumbSize / minOf(artBitmap.width, artBitmap.height)
        val dx = thumbLeft - (artBitmap.width * scale - thumbSize) / 2f
        val dy = thumbTop - (artBitmap.height * scale - thumbSize) / 2f
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        shader.setLocalMatrix(matrix)

        val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.shader = shader
            isFilterBitmap = true
            isDither = true
        }
        canvas.drawRoundRect(thumbRect, 44f, 44f, thumbPaint)
    } else {
        val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#221F1D".toColorInt()
        }
        canvas.drawRoundRect(thumbRect, 44f, 44f, placeholderPaint)
    }

    val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 84f
        typeface = spaceGroteskSemiBold
    }
    val safeHeaderTitle = if (song.title.length > 26) song.title.take(24) + "…" else song.title
    canvas.drawText(safeHeaderTitle, thumbLeft + thumbSize + 48f, thumbTop + 104f, headerTitlePaint)

    val headerArtistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.argb(180, 255, 255, 255)
        textSize = 58f
        typeface = Typeface.DEFAULT
    }
    val safeHeaderArtist = if (song.artist.length > 32) song.artist.take(30) + "…" else song.artist
    canvas.drawText(safeHeaderArtist, thumbLeft + thumbSize + 48f, thumbTop + 192f, headerArtistPaint)

    val validLines = lines.filter { it.isNotBlank() }
    val (fontSize, lineSpacingMult, extraSpacing, qSize, spacingBetween) = when {
        validLines.size <= 2 -> listOf(124f, 1.26f, 20f, 120f, 32f)
        validLines.size <= 5 -> listOf(104f, 1.22f, 16f, 106f, 28f)
        else -> listOf(92f, 1.20f, 12f, 96f, 24f)
    }

    val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = fontSize
        typeface = spaceGroteskMedium
    }

    val textToDraw = validLines.joinToString("\n")
    val textMargin = 180f
    val textWidth = (width - (textMargin * 2f)).toInt()
    val staticLayout = StaticLayout.Builder.obtain(textToDraw, 0, textToDraw.length, lyricPaint, textWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(extraSpacing, lineSpacingMult)
        .build()

    val availableTop = thumbTop + thumbSize + 60f
    val availableBottom = height - 220f
    val availableHeight = availableBottom - availableTop
    val totalBlockHeight = qSize + spacingBetween + staticLayout.height
    val blockStartY = maxOf(availableTop, availableTop + ((availableHeight - totalBlockHeight) / 2f).coerceAtLeast(0f))

    val quoteDrawable = ContextCompat.getDrawable(context, R.drawable.lucide_ic_quote)?.mutate()
    if (quoteDrawable != null) {
        quoteDrawable.setTint(android.graphics.Color.argb(102, 255, 255, 255))
        quoteDrawable.setBounds(textMargin.toInt(), blockStartY.toInt(), (textMargin + qSize).toInt(), (blockStartY + qSize).toInt())
        quoteDrawable.draw(canvas)
    }

    val textStartY = blockStartY + qSize + spacingBetween
    canvas.withTranslation(textMargin, textStartY) {
        staticLayout.draw(this)
    }

    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.argb(128, 255, 255, 255)
        textSize = 58f
        typeface = ibmPlexMono
    }
    canvas.drawText(context.getString(AppR.string.app_name), 180f, height - 160f, footerPaint)

    val tapeSize = 78
    if (tapeDrawable != null) {
        tapeDrawable.setTint(android.graphics.Color.argb(128, 255, 255, 255))
        tapeDrawable.setBounds(
            width - 180 - tapeSize,
            height - 216,
            width - 180,
            height - 216 + tapeSize
        )
        tapeDrawable.draw(canvas)
    }
}
