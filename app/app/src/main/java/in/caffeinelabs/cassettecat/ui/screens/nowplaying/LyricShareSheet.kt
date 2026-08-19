package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.io.File
import kotlinx.coroutines.launch

enum class LyricCardTheme(val label: String) {
    ATMOSPHERE("Atmosphere"),
    OBSIDIAN("Obsidian")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricShareSheet(
    song: Song,
    selectedLines: List<String>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTheme by remember { mutableStateOf(LyricCardTheme.ATMOSPHERE) }

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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share Lyric Quote",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                )
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_x,
                    contentDescription = "Close",
                    onClick = onDismiss
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                LyricQuoteCard(
                    song = song,
                    lines = selectedLines,
                    theme = selectedTheme
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LyricCardTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { selectedTheme = theme }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = theme.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Quick Target Action Icons Tray
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShareActionPill(
                    iconRes = R.drawable.lucide_ic_copy,
                    label = "Copy text",
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        val fullText = selectedLines.joinToString("\n") + "\n\n${song.title} - ${song.artist}"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Lyrics", fullText))
                        Toast.makeText(context, "Lyrics copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                ShareActionPill(
                    iconRes = `in`.caffeinelabs.cassettecat.R.drawable.ic_logo_whatsapp,
                    label = "WhatsApp",
                    packageNames = listOf("com.whatsapp", "com.whatsapp.w4b"),
                    backgroundColor = Color(0xFF25D366),
                    iconTint = Color.White,
                    onClick = {
                        scope.launch {
                            val artBitmap = loadSongArtwork(context, song)
                            val bitmap = buildLyricCardPoster(context, song, selectedLines, selectedTheme, artBitmap)
                            val targetPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
                                runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
                            } ?: "com.whatsapp"
                            shareLyricCardBitmap(context, bitmap, "${song.title} - ${song.artist}", targetPkg)
                        }
                    }
                )

                ShareActionPill(
                    iconRes = `in`.caffeinelabs.cassettecat.R.drawable.ic_logo_instagram,
                    label = "Stories",
                    packageNames = listOf("com.instagram.android"),
                    backgroundBrush = Brush.linearGradient(
                        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFF77737))
                    ),
                    iconTint = Color.White,
                    onClick = {
                        scope.launch {
                            val artBitmap = loadSongArtwork(context, song)
                            val bitmap = buildLyricCardPoster(context, song, selectedLines, selectedTheme, artBitmap)
                            shareLyricCardBitmap(context, bitmap, "${song.title} - ${song.artist}", "com.instagram.android")
                        }
                    }
                )

                ShareActionPill(
                    iconRes = R.drawable.lucide_ic_share_2,
                    label = "More",
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        scope.launch {
                            val artBitmap = loadSongArtwork(context, song)
                            val bitmap = buildLyricCardPoster(context, song, selectedLines, selectedTheme, artBitmap)
                            shareLyricCardBitmap(context, bitmap, "${song.title} - ${song.artist}", null)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LyricQuoteCard(
    song: Song,
    lines: List<String>,
    theme: LyricCardTheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                when (theme) {
                    LyricCardTheme.ATMOSPHERE -> Modifier.background(Color(0xFF141210))
                    LyricCardTheme.OBSIDIAN -> Modifier
                        .background(Color(0xFF0F0E0D))
                        .border(1.dp, Color(0xFF2E2A27), RoundedCornerShape(20.dp))
                }
            )
    ) {
        if (theme == LyricCardTheme.ATMOSPHERE) {
            Box(modifier = Modifier.fillMaxSize()) {
                AlbumArt(
                    song = song,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_quote),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
                lines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (lines.size <= 2) 22.sp else 18.sp,
                            lineHeight = if (lines.size <= 2) 30.sp else 26.sp,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CassetteCat",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = Color.White.copy(alpha = 0.45f)
                )
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_cassette_tape),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun buildLyricCardPoster(
    context: Context,
    song: Song,
    lines: List<String>,
    theme: LyricCardTheme,
    artBitmap: Bitmap?
): Bitmap {
    val width = 2160
    val height = 2700
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val spaceGroteskBold = runCatching {
        val font = ResourcesCompat.getFont(context, AppR.font.space_grotesk_variable)
        if (font != null) Typeface.create(font, 700, false) else Typeface.DEFAULT_BOLD
    }.getOrNull() ?: Typeface.DEFAULT_BOLD
    val ibmPlexMono = runCatching { ResourcesCompat.getFont(context, AppR.font.ibm_plex_mono_regular) }.getOrNull() ?: Typeface.MONOSPACE
    val tapeDrawable = ContextCompat.getDrawable(context, R.drawable.lucide_ic_cassette_tape)?.mutate()

    // Background
    when (theme) {
        LyricCardTheme.ATMOSPHERE -> {
            if (artBitmap != null) {
                val blurred = createFastBlurredBitmap(artBitmap)
                val filterPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    isFilterBitmap = true
                    isDither = true
                }
                canvas.drawBitmap(blurred, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), filterPaint)

                // Dark gradient scrim matching in-app Atmosphere theme
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
                            android.graphics.Color.parseColor("#261E1A"),
                            android.graphics.Color.parseColor("#14110F"),
                            android.graphics.Color.parseColor("#0A0908")
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
                color = android.graphics.Color.parseColor("#0F0E0D")
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#2E2A27")
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawRoundRect(RectF(60f, 60f, width - 60f, height - 60f), 64f, 64f, borderPaint)
        }
    }

    // Top Header Thumbnail (220x220)
    val thumbSize = 220f
    val thumbLeft = 180f
    val thumbTop = 180f
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
        canvas.drawRoundRect(thumbRect, 40f, 40f, thumbPaint)
    } else {
        val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#221F1D")
        }
        canvas.drawRoundRect(thumbRect, 40f, 40f, placeholderPaint)
    }

    // Header Title & Artist
    val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 76f
        typeface = spaceGroteskBold
        fontVariationSettings = "'wght' 700"
    }
    val safeHeaderTitle = if (song.title.length > 28) song.title.take(26) + "…" else song.title
    canvas.drawText(safeHeaderTitle, 450f, thumbTop + 96f, headerTitlePaint)

    val headerArtistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.argb(180, 255, 255, 255)
        textSize = 56f
        typeface = Typeface.DEFAULT
    }
    val safeHeaderArtist = if (song.artist.length > 34) song.artist.take(32) + "…" else song.artist
    canvas.drawText(safeHeaderArtist, 450f, thumbTop + 184f, headerArtistPaint)

    // Quote Icon
    val quoteDrawable = ContextCompat.getDrawable(context, R.drawable.lucide_ic_quote)?.mutate()
    if (quoteDrawable != null) {
        quoteDrawable.setTint(android.graphics.Color.argb(102, 255, 255, 255))
        val qSize = 96
        quoteDrawable.setBounds(180, 480, 180 + qSize, 480 + qSize)
        quoteDrawable.draw(canvas)
    }

    // Lyrics Text Layout
    val validLines = lines.filter { it.isNotBlank() }
    val fontSize = when {
        validLines.size <= 1 -> 116f
        validLines.size <= 2 -> 104f
        validLines.size <= 4 -> 90f
        else -> 72f
    }
    val lineSpacingMult = when {
        validLines.size <= 2 -> 1.30f
        validLines.size <= 4 -> 1.25f
        else -> 1.18f
    }

    val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = fontSize
        typeface = spaceGroteskBold
        fontVariationSettings = "'wght' 700"
    }

    val textToDraw = validLines.joinToString("\n")
    val textWidth = width - 360
    val staticLayout = StaticLayout.Builder.obtain(textToDraw, 0, textToDraw.length, lyricPaint, textWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(24f, lineSpacingMult)
        .build()

    canvas.save()
    canvas.translate(180f, 620f)
    staticLayout.draw(canvas)
    canvas.restore()

    // Footer
    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = android.graphics.Color.argb(128, 255, 255, 255)
        textSize = 56f
        typeface = ibmPlexMono
    }
    canvas.drawText("CassetteCat", 180f, height - 180f, footerPaint)

    val tapeSize = 72
    if (tapeDrawable != null) {
        tapeDrawable.setTint(android.graphics.Color.argb(128, 255, 255, 255))
        tapeDrawable.setBounds(
            (width - 180 - tapeSize),
            (height - 232),
            (width - 180),
            (height - 232 + tapeSize)
        )
        tapeDrawable.draw(canvas)
    }

    return bitmap
}

private fun createFastBlurredBitmap(src: Bitmap): Bitmap {
    val downW = 128
    val downH = 160
    val small = Bitmap.createScaledBitmap(src, downW, downH, true)
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
            var r = 0; var g = 0; var b = 0; var count = 0
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
            var r = 0; var g = 0; var b = 0; var count = 0
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

private fun shareLyricCardBitmap(
    context: Context,
    bitmap: Bitmap,
    title: String,
    targetPackage: String? = null
) {
    val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(cacheDir, "lyric_quote_${System.currentTimeMillis()}.png")
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (targetPackage != null) {
            setPackage(targetPackage)
        }
    }
    runCatching {
        if (targetPackage != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent.createChooser(intent, "Share Lyric Card"))
        }
    }.onFailure {
        context.startActivity(Intent.createChooser(intent, "Share Lyric Card"))
    }
}
