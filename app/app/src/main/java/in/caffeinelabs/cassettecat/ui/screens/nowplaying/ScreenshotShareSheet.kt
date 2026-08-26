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
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.loadCanvasTypefaces
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ShareCardMode(val label: String) {
    SONG("Song"),
    LYRICS("Lyrics")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScreenshotShareSheet(
    song: Song,
    syncedLyrics: List<LyricLine>?,
    fallbackLyrics: String?,
    currentPositionMs: Long,
    onDismiss: () -> Unit,
    onOpenFullLyricEditor: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val availableLyrics = remember(syncedLyrics, fallbackLyrics, currentPositionMs) {
        if (!syncedLyrics.isNullOrEmpty()) {
            val activeIdx = syncedLyrics.indexOfLast { it.timestampMs <= currentPositionMs }.coerceAtLeast(0)
            val endIdx = (activeIdx + 3).coerceAtMost(syncedLyrics.size)
            syncedLyrics.subList(activeIdx, endIdx).map { it.text }.filter { it.isNotBlank() }
        } else if (!fallbackLyrics.isNullOrBlank()) {
            fallbackLyrics.lines().filter { it.isNotBlank() }.take(4)
        } else {
            emptyList()
        }
    }

    val hasLyrics = availableLyrics.isNotEmpty()
    var mode by remember { mutableStateOf(ShareCardMode.SONG) }
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
                    text = "Share Card",
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

            Spacer(Modifier.height(12.dp))

            // Mode Selector Pill (Song vs Lyrics)
            if (hasLyrics) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShareCardMode.entries.forEach { entry ->
                        val isSelected = mode == entry
                        val bgColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            animationSpec = tween(200),
                            label = "tabBg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(bgColor)
                                .clickable { mode = entry }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(4.dp))
            }

            // Fixed 4:5 Aspect Ratio In-App Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (mode == ShareCardMode.SONG) {
                    SongSharePreviewCard(
                        song = song,
                        theme = selectedTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LyricSharePreviewCard(
                        song = song,
                        lines = availableLyrics,
                        theme = selectedTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (mode == ShareCardMode.LYRICS) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
                        .tapScale {
                            onDismiss()
                            onOpenFullLyricEditor()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_pencil),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Select different lyrics",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(20.dp))
            }

            // Theme Selection Pills (Atmosphere vs Obsidian)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LyricCardTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    val textColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(if (isSelected) 1.dp else 0.5.dp, borderColor, CircleShape)
                            .clickable { selectedTheme = theme }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = theme.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor
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
                        val text = if (mode == ShareCardMode.SONG) {
                            "${song.title} - ${song.artist}"
                        } else {
                            "${availableLyrics.joinToString("\n")}\n\n${song.title} - ${song.artist}"
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("CassetteCat", text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                ShareActionPill(
                    iconRes = AppR.drawable.ic_logo_whatsapp,
                    label = "WhatsApp",
                    packageNames = listOf("com.whatsapp", "com.whatsapp.w4b"),
                    backgroundColor = Color(0xFF25D366),
                    iconTint = Color.White,
                    onClick = {
                        scope.launch {
                            val artBitmap = loadSongArtwork(context, song)
                            val bitmap = withContext(Dispatchers.Default) {
                                generateSharePoster(context, song, mode, availableLyrics, selectedTheme, artBitmap)
                            }
                            val targetPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
                                runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
                            } ?: "com.whatsapp"
                            shareImageWithApp(context, bitmap, "${song.title} - ${song.artist}", targetPkg)
                        }
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
                    onClick = {
                        scope.launch {
                            val artBitmap = loadSongArtwork(context, song)
                            val bitmap = withContext(Dispatchers.Default) {
                                generateSharePoster(context, song, mode, availableLyrics, selectedTheme, artBitmap)
                            }
                            shareImageToInstagramStories(context, bitmap, "${song.title} - ${song.artist}", selectedTheme)
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
                            val bitmap = withContext(Dispatchers.Default) {
                                generateSharePoster(context, song, mode, availableLyrics, selectedTheme, artBitmap)
                            }
                            shareImageWithApp(context, bitmap, "${song.title} - ${song.artist}", null)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SongSharePreviewCard(
    song: Song,
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
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
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
                                listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_cassette_tape),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "CassetteCat",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun LyricSharePreviewCard(
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
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
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
                                listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.85f))
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
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_quote),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(24.dp)
                )
                lines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (lines.size <= 2) 21.sp else 17.5.sp,
                            lineHeight = if (lines.size <= 2) 28.sp else 24.sp,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold
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

@Composable
internal fun ShareActionPill(
    iconRes: Int,
    label: String,
    packageNames: List<String> = emptyList(),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    backgroundBrush: Brush? = null,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val installedAppIcon = remember(packageNames) {
        packageNames.firstNotNullOfOrNull { pkg ->
            try {
                val drawable = context.packageManager.getApplicationIcon(pkg)
                drawable.toBitmap(width = 128, height = 128).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .tapScale(onClick)
            .padding(4.dp)
    ) {
        if (installedAppIcon != null) {
            Image(
                bitmap = installedAppIcon,
                contentDescription = label,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else if (backgroundBrush != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(backgroundBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun generateSharePoster(
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

    val (spaceGroteskBold, spaceGroteskSemiBold, ibmPlexMono, _) = loadCanvasTypefaces(context)
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
        // High-res Centered Artwork
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

        // Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 104f
            typeface = spaceGroteskBold
            fontVariationSettings = "'wght' 700"
            textAlign = Paint.Align.CENTER
        }
        val safeTitle = if (song.title.length > 32) song.title.take(30) + "…" else song.title
        canvas.drawText(safeTitle, width / 2f, artTop + artSize + 220f, titlePaint)

        // Artist
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.argb(190, 255, 255, 255)
            textSize = 68f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        val safeArtist = if (song.artist.length > 40) song.artist.take(38) + "…" else song.artist
        canvas.drawText(safeArtist, width / 2f, artTop + artSize + 340f, artistPaint)

        // Centered CassetteCat Branding
        val footerText = "CassetteCat"
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

    } else {
        // Top Header Thumbnail (240x240)
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

        // Header Title & Artist
        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 84f
            typeface = spaceGroteskBold
            fontVariationSettings = "'wght' 600"
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

        // Lyrics Text Layout (Perfect proportion and spacing without overlap)
        val validLines = lines.filter { it.isNotBlank() }
        val (fontSize, lineSpacingMult, extraSpacing, qSize, spacingBetween) = when {
            validLines.size <= 2 -> listOf(124f, 1.26f, 20f, 120f, 32f)
            validLines.size <= 5 -> listOf(104f, 1.22f, 16f, 106f, 28f)
            else -> listOf(92f, 1.20f, 12f, 96f, 24f)
        }

        val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = fontSize
            typeface = spaceGroteskSemiBold
            fontVariationSettings = "'wght' 500"
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

        // Quote Icon
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

        // Footer
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.argb(128, 255, 255, 255)
            textSize = 58f
            typeface = ibmPlexMono
        }
        canvas.drawText("CassetteCat", 180f, height - 160f, footerPaint)

        val tapeSize = 78
        if (tapeDrawable != null) {
            tapeDrawable.setTint(android.graphics.Color.argb(128, 255, 255, 255))
            tapeDrawable.setBounds(
                (width - 180 - tapeSize),
                (height - 216),
                (width - 180),
                (height - 216 + tapeSize)
            )
            tapeDrawable.draw(canvas)
        }
    }

    return bitmap
}

private suspend fun cacheBitmapAndGetUri(context: Context, bitmap: Bitmap, filenamePrefix: String): Uri =
    withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(cacheDir, "${filenamePrefix}_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

internal suspend fun shareImageToInstagramStories(
    context: Context,
    bitmap: Bitmap,
    title: String,
    theme: LyricCardTheme = LyricCardTheme.ATMOSPHERE
) {
    val uri = cacheBitmapAndGetUri(context, bitmap, "story")

    // Grant permission to Instagram package specifically
    context.grantUriPermission(
        "com.instagram.android",
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    )

    val topBg = if (theme == LyricCardTheme.OBSIDIAN) "#000000" else "#1A1715"
    val bottomBg = if (theme == LyricCardTheme.OBSIDIAN) "#000000" else "#0A0908"

    // 1. Direct Instagram Stories Intent API (Sticker card on gradient background - standard music share format)
    val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        type = "image/png"
        putExtra("interactive_asset_uri", uri)
        putExtra("top_background_color", topBg)
        putExtra("bottom_background_color", bottomBg)
        putExtra("source_application", context.packageName)
        setPackage("com.instagram.android")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val canHandleStories = runCatching {
        context.packageManager.resolveActivity(storiesIntent, 0) != null
    }.getOrDefault(false)

    if (canHandleStories) {
        val launched = runCatching {
            context.startActivity(storiesIntent)
            true
        }.getOrDefault(false)
        if (launched) return
    }

    // 2. Direct component intent for Instagram Stories camera
    val directStoriesIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        setClassName("com.instagram.android", "com.instagram.share.handler.ShareHandlerActivity")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val canHandleDirect = runCatching {
        context.packageManager.resolveActivity(directStoriesIntent, 0) != null
    }.getOrDefault(false)

    if (canHandleDirect) {
        val launched = runCatching {
            context.startActivity(directStoriesIntent)
            true
        }.getOrDefault(false)
        if (launched) return
    }

    // 3. Fallback to package send
    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, title)
        setPackage("com.instagram.android")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(fallbackIntent)
    }.onFailure {
        val chooserIntent = Intent(fallbackIntent).apply { setPackage(null) }
        runCatching { context.startActivity(Intent.createChooser(chooserIntent, "Share to Stories")) }
    }
}

internal suspend fun shareImageWithApp(
    context: Context,
    bitmap: Bitmap,
    title: String,
    targetPackage: String?
) {
    val uri = cacheBitmapAndGetUri(context, bitmap, "share_card")
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
            context.startActivity(Intent.createChooser(intent, "Share Card"))
        }
    }.onFailure {
        val chooserIntent = Intent(intent).apply { setPackage(null) }
        runCatching { context.startActivity(Intent.createChooser(chooserIntent, "Share Card")) }
    }
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
