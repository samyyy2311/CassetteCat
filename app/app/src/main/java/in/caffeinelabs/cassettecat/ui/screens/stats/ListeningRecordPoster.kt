package `in`.caffeinelabs.cassettecat.ui.screens.stats

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.ShareActionPill
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.shareImageToInstagramStories
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.shareImageWithApp
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexSansFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.loadCanvasTypefaces
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ListeningRecordTheme(val label: String) {
    ATMOSPHERE("Atmosphere"),
    OBSIDIAN("Obsidian")
}

internal fun buildListeningRecordPoster(
    context: Context,
    monthAbbreviation: String,
    yearLabel: String,
    listeningMinutes: Long,
    totalPlays: Int,
    uniqueSongs: Int,
    topArtists: List<ArtistStat>,
    topSongs: List<SongStat>,
    topAlbums: List<AlbumStat> = emptyList(),
    topArtistArtwork: Bitmap? = null,
    topSongArtwork: Bitmap? = null,
    topAlbumArtwork: Bitmap? = null,
    theme: ListeningRecordTheme = ListeningRecordTheme.ATMOSPHERE,
    isRewind: Boolean = false
): Bitmap {
    val width = 2160
    val height = 2700
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val (spaceGroteskBold, spaceGroteskSemiBold, ibmPlexMono, ibmPlexSans) = loadCanvasTypefaces(context)

    val tapeDrawable = ContextCompat.getDrawable(context, R.drawable.lucide_ic_cassette_tape)?.mutate()

    val (color1, color2, _) = extractAuroraColors(topSongArtwork)

    // 1. Vibrant Ambient Background
    when (theme) {
        ListeningRecordTheme.ATMOSPHERE -> {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = "#07060A".toColorInt()
            })

            // Top-left/center vibrant glowing artwork flare
            canvas.drawCircle(width * 0.20f, height * 0.10f, 1200f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.20f, height * 0.10f, 1200f,
                    intArrayOf(color1, android.graphics.Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            })

            // Top-right radiant bloom
            canvas.drawCircle(width * 0.85f, height * 0.20f, 1250f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.85f, height * 0.20f, 1250f,
                    intArrayOf(color2, android.graphics.Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            })

            // Right-side ambient artwork reflection
            canvas.drawCircle(width * 0.90f, height * 0.65f, 1100f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.90f, height * 0.65f, 1100f,
                    intArrayOf(color1, android.graphics.Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            })

            // Soft dark contrast overlay
            val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        android.graphics.Color.argb(20, 0, 0, 0),
                        android.graphics.Color.argb(100, 5, 4, 8),
                        android.graphics.Color.argb(190, 5, 4, 8)
                    ),
                    floatArrayOf(0f, 0.40f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        }

        ListeningRecordTheme.OBSIDIAN -> {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = "#0B0A0D".toColorInt()
            })

            val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = "#2E2A27".toColorInt()
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawRoundRect(RectF(60f, 60f, width - 60f, height - 60f), 56f, 56f, outerBorder)
        }
    }

    val leftMargin = 110f
    val rightMargin = width - 100f

    // Bold, Clean Text Paints with Enhanced Sizes
    val sectionHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = spaceGroteskBold
        textSize = 68f
    }
    val rankNumPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(160, 255, 255, 255)
        typeface = spaceGroteskBold
        textSize = 50f
    }
    val itemTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = spaceGroteskSemiBold
        textSize = 56f
    }
    val itemSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(180, 255, 255, 255)
        typeface = ibmPlexSans
        textSize = 44f
    }

    // 2. Top Header ("Rewind" on left, "CassetteCat" on right)
    val headerY = 175f
    canvas.drawText(
        "Rewind",
        leftMargin,
        headerY,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = spaceGroteskBold
            textSize = 72f
        }
    )

    if (tapeDrawable != null) {
        tapeDrawable.setTint(android.graphics.Color.WHITE)
        tapeDrawable.setBounds((rightMargin - 360f).toInt(), (headerY - 48f).toInt(), (rightMargin - 300f).toInt(), (headerY + 12f).toInt())
        tapeDrawable.draw(canvas)
    }

    canvas.drawText(
        "CassetteCat",
        rightMargin - 280f,
        headerY,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = spaceGroteskSemiBold
            textSize = 50f
        }
    )

    // 3. Hero Date & Minutes (Big, Bold, Centered)
    val heroY = 360f
    val localizedMonth = monthAbbreviation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val heroTitle = if (isRewind) yearLabel else "$localizedMonth $yearLabel"

    val heroYearPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = spaceGroteskBold
        textSize = 125f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(heroTitle, width / 2f, heroY, heroYearPaint)

    val formattedMinutes = NumberFormat.getNumberInstance(Locale.US).format(listeningMinutes)
    val minutesText = "$formattedMinutes minutes"
    val minutesPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(200, 255, 255, 255)
        typeface = ibmPlexSans
        textSize = 54f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(minutesText, width / 2f, heroY + 80f, minutesPaint)

    // 4. Two-Column Asymmetric Section (Left: Text, Right: Large Artwork Stack)
    val splitLeftWidth = 1220f
    val rightArtSize = 580f
    val rightArtLeft = rightMargin - rightArtSize

    fun drawArtwork(bitmap: Bitmap?, left: Float, top: Float, size: Float, isCircle: Boolean) {
        val rect = RectF(left, top, left + size, top + size)

        // Drop shadow
        canvas.drawRoundRect(
            RectF(left - 12f, top + 12f, left + size + 12f, top + size + 28f),
            if (isCircle) size / 2f else 32f,
            if (isCircle) size / 2f else 32f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(110, 0, 0, 0) }
        )

        if (bitmap != null) {
            val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val matrix = Matrix()
            val scale = size / minOf(bitmap.width, bitmap.height)
            val dx = left - (bitmap.width * scale - size) / 2f
            val dy = top - (bitmap.height * scale - size) / 2f
            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)
            shader.setLocalMatrix(matrix)

            val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.shader = shader
                isFilterBitmap = true
                isDither = true
            }

            if (isCircle) {
                canvas.drawCircle(left + size / 2f, top + size / 2f, size / 2f, artPaint)
                canvas.drawCircle(left + size / 2f, top + size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(60, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                })
            } else {
                canvas.drawRoundRect(rect, 32f, 32f, artPaint)
                canvas.drawRoundRect(rect, 32f, 32f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(60, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                })
            }
        } else {
            val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#1E1828".toColorInt() }
            if (isCircle) {
                canvas.drawCircle(left + size / 2f, top + size / 2f, size / 2f, pPaint)
            } else {
                canvas.drawRoundRect(rect, 32f, 32f, pPaint)
            }
            if (tapeDrawable != null) {
                tapeDrawable.setTint(android.graphics.Color.argb(160, 255, 255, 255))
                val iconSize = 160f
                tapeDrawable.setBounds(
                    (left + (size - iconSize) / 2).toInt(),
                    (top + (size - iconSize) / 2).toInt(),
                    (left + (size + iconSize) / 2).toInt(),
                    (top + (size + iconSize) / 2).toInt()
                )
                tapeDrawable.draw(canvas)
            }
        }
    }

    val art1Top = 550f
    val art2Top = 1190f
    val art3Top = 1830f

    // 1: Top Artist Circle
    drawArtwork(topArtistArtwork ?: topSongArtwork, rightArtLeft, art1Top, rightArtSize, isCircle = true)

    // 2: Top Song Square
    drawArtwork(topSongArtwork, rightArtLeft, art2Top, rightArtSize, isCircle = false)

    // 3: Top Album Square
    drawArtwork(topAlbumArtwork ?: topSongArtwork, rightArtLeft, art3Top, rightArtSize, isCircle = false)

    // --- LEFT COLUMN TEXT SECTIONS ---

    // SECTION 1: Top Artists
    val sec1Top = 580f
    canvas.drawText("Top Artists", leftMargin, sec1Top, sectionHeaderPaint)

    val artistsList = topArtists.take(3)
    val artRowH = 125f
    val artRowStart = sec1Top + 105f

    artistsList.forEachIndexed { i, stat ->
        val y = artRowStart + i * artRowH
        canvas.drawText("${i + 1}", leftMargin, y, rankNumPaint)
        canvas.drawText(
            truncateText(itemTitlePaint, stat.artist, splitLeftWidth - 90f),
            leftMargin + 80f,
            y,
            itemTitlePaint
        )
    }

    // SECTION 2: Top Songs
    val sec2Top = 1200f
    canvas.drawText("Top Songs", leftMargin, sec2Top, sectionHeaderPaint)

    val songsList = topSongs.take(3)
    val songRowH = 145f
    val songRowStart = sec2Top + 105f

    songsList.forEachIndexed { i, stat ->
        val y = songRowStart + i * songRowH
        canvas.drawText("${i + 1}", leftMargin, y, rankNumPaint)
        canvas.drawText(
            truncateText(itemTitlePaint, stat.song.title, splitLeftWidth - 90f),
            leftMargin + 80f,
            y,
            itemTitlePaint
        )
        canvas.drawText(
            truncateText(itemSubPaint, stat.song.artist, splitLeftWidth - 90f),
            leftMargin + 80f,
            y + 48f,
            itemSubPaint
        )
    }

    // SECTION 3: Top Albums
    val sec3Top = 1840f
    canvas.drawText("Top Albums", leftMargin, sec3Top, sectionHeaderPaint)

    val albumsList = topAlbums.take(3)
    val albumRowH = 145f
    val albumRowStart = sec3Top + 105f

    if (albumsList.isNotEmpty()) {
        albumsList.forEachIndexed { i, stat ->
            val y = albumRowStart + i * albumRowH
            canvas.drawText("${i + 1}", leftMargin, y, rankNumPaint)
            canvas.drawText(
                truncateText(itemTitlePaint, stat.album, splitLeftWidth - 90f),
                leftMargin + 80f,
                y,
                itemTitlePaint
            )
            canvas.drawText(
                truncateText(itemSubPaint, stat.artSong.artist, splitLeftWidth - 90f),
                leftMargin + 80f,
                y + 48f,
                itemSubPaint
            )
        }
    } else {
        topSongs.drop(3).take(3).forEachIndexed { i, stat ->
            val y = albumRowStart + i * albumRowH
            canvas.drawText("${i + 1}", leftMargin, y, rankNumPaint)
            canvas.drawText(
                truncateText(itemTitlePaint, stat.song.album.ifEmpty { stat.song.title }, splitLeftWidth - 90f),
                leftMargin + 80f,
                y,
                itemTitlePaint
            )
            canvas.drawText(
                truncateText(itemSubPaint, stat.song.artist, splitLeftWidth - 90f),
                leftMargin + 80f,
                y + 48f,
                itemSubPaint
            )
        }
    }

    return bitmap
}

@Composable
internal fun ListeningRecordPreviewCard(
    monthAbbreviation: String,
    yearLabel: String,
    listeningMinutes: Long,
    totalPlays: Int,
    uniqueSongs: Int,
    topArtists: List<ArtistStat>,
    topSongs: List<SongStat>,
    topAlbums: List<AlbumStat>,
    topArtistArtwork: Bitmap?,
    topSongArtwork: Bitmap?,
    topAlbumArtwork: Bitmap?,
    theme: ListeningRecordTheme,
    isRewind: Boolean,
    modifier: Modifier = Modifier
) {
    val localizedMonth = monthAbbreviation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val heroTitle = if (isRewind) yearLabel else "$localizedMonth $yearLabel"
    val formattedMinutes = NumberFormat.getNumberInstance(Locale.US).format(listeningMinutes)

    val (color1, color2, _) = remember(topSongArtwork) {
        val (i1, i2, i3) = extractAuroraColors(topSongArtwork)
        Triple(Color(i1), Color(i2), Color(i3))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                when (theme) {
                    ListeningRecordTheme.ATMOSPHERE -> Modifier.background(Color(0xFF07060A))
                    ListeningRecordTheme.OBSIDIAN -> Modifier
                        .background(Color(0xFF0B0A0D))
                        .border(1.dp, Color(0xFF2E2A27), RoundedCornerShape(20.dp))
                }
            )
    ) {
        if (theme == ListeningRecordTheme.ATMOSPHERE) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.TopStart)
                    .background(color1.copy(alpha = 0.60f), CircleShape)
                    .blur(50.dp)
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.TopEnd)
                    .background(color2.copy(alpha = 0.55f), CircleShape)
                    .blur(55.dp)
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.BottomEnd)
                    .background(color1.copy(alpha = 0.45f), CircleShape)
                    .blur(50.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.70f))
                        )
                    )
            )
        }

        // Card Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rewind",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_cassette_tape),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "CassetteCat",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = SpaceGroteskFontFamily, fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Milestone
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = heroTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$formattedMinutes minutes",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            // 2-Column Split
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column (Text List with Large Fonts)
                Column(
                    modifier = Modifier.weight(1.4f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Top Artists
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Top Artists",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        topArtists.take(3).forEachIndexed { i, stat ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${i + 1}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                                Text(
                                    text = stat.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Top Songs
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Top Songs",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        topSongs.take(3).forEachIndexed { i, stat ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${i + 1}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                                Column {
                                    Text(
                                        text = stat.song.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stat.song.artist,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Top Albums
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Top Albums",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val albumsToShow = if (topAlbums.isNotEmpty()) topAlbums.take(3).map { it.album to it.artSong.artist }
                        else topSongs.drop(3).take(3).map { it.song.album.ifEmpty { it.song.title } to it.song.artist }

                        albumsToShow.forEachIndexed { i, (alb, art) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${i + 1}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                                Column {
                                    Text(
                                        text = alb,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = art,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Column (Stacked Visual Artworks)
                Column(
                    modifier = Modifier.weight(1.0f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Artist Circle
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                    ) {
                        val art = topArtistArtwork ?: topSongArtwork
                        if (art != null) {
                            Image(bitmap = art.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1828)))
                        }
                    }

                    // Song Square
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(10.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                    ) {
                        if (topSongArtwork != null) {
                            Image(bitmap = topSongArtwork.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1828)))
                        }
                    }

                    // Album Square
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(10.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                    ) {
                        val albArt = topAlbumArtwork ?: topSongArtwork
                        if (albArt != null) {
                            Image(bitmap = albArt.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1828)))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListeningRecordShareSheet(
    monthAbbreviation: String,
    yearLabel: String,
    periodTitle: String,
    listeningMinutes: Long,
    totalPlays: Int,
    uniqueSongs: Int,
    topArtists: List<ArtistStat>,
    topSongs: List<SongStat>,
    topAlbums: List<AlbumStat> = emptyList(),
    isRewind: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTheme by remember { mutableStateOf(ListeningRecordTheme.ATMOSPHERE) }
    var isGeneratingShare by remember { mutableStateOf(false) }

    val topSong = topSongs.firstOrNull()?.song
    val topAlbumSong = topAlbums.firstOrNull()?.artSong ?: topSongs.getOrNull(1)?.song
    val topArtistSong = topSongs.firstOrNull { it.song.artist == topArtists.firstOrNull()?.artist }?.song ?: topSong

    var topSongArtwork by remember(topSong?.id) { mutableStateOf<Bitmap?>(null) }
    var topAlbumArtwork by remember(topAlbumSong?.id) { mutableStateOf<Bitmap?>(null) }
    var topArtistArtwork by remember(topArtistSong?.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(topSong?.id, topAlbumSong?.id, topArtistSong?.id) {
        if (topSong != null) topSongArtwork = loadSongArtwork(context, topSong)
        if (topAlbumSong != null) topAlbumArtwork = loadSongArtwork(context, topAlbumSong)
        if (topArtistSong != null) topArtistArtwork = loadSongArtwork(context, topArtistSong)
    }

    fun sharePoster(share: suspend (Bitmap) -> Unit) {
        isGeneratingShare = true
        scope.launch(Dispatchers.Default) {
            try {
                val bitmap = buildListeningRecordPoster(
                    context = context,
                    monthAbbreviation = monthAbbreviation,
                    yearLabel = yearLabel,
                    listeningMinutes = listeningMinutes,
                    totalPlays = totalPlays,
                    uniqueSongs = uniqueSongs,
                    topArtists = topArtists,
                    topSongs = topSongs,
                    topAlbums = topAlbums,
                    topArtistArtwork = topArtistArtwork,
                    topSongArtwork = topSongArtwork,
                    topAlbumArtwork = topAlbumArtwork,
                    theme = selectedTheme,
                    isRewind = isRewind
                )
                withContext(Dispatchers.Main) { share(bitmap) }
            } finally {
                withContext(Dispatchers.Main) { isGeneratingShare = false }
            }
        }
    }

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
                    text = "Share Rewind",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                )
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_x,
                    contentDescription = stringResource(AppR.string.close),
                    onClick = onDismiss
                )
            }

            Spacer(Modifier.height(16.dp))

            // Fixed 4:5 Aspect Ratio Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                ListeningRecordPreviewCard(
                    monthAbbreviation = monthAbbreviation,
                    yearLabel = yearLabel,
                    listeningMinutes = listeningMinutes,
                    totalPlays = totalPlays,
                    uniqueSongs = uniqueSongs,
                    topArtists = topArtists,
                    topSongs = topSongs,
                    topAlbums = topAlbums,
                    topArtistArtwork = topArtistArtwork,
                    topSongArtwork = topSongArtwork,
                    topAlbumArtwork = topAlbumArtwork,
                    theme = selectedTheme,
                    isRewind = isRewind,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(20.dp))

            // Theme Selection Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListeningRecordTheme.entries.forEach { theme ->
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
            if (isGeneratingShare) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(36.dp)
                )
            } else {
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
                            sharePoster { bitmap ->
                                val targetPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
                                    runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
                                } ?: "com.whatsapp"
                                shareImageWithApp(context, bitmap, periodTitle, targetPkg)
                            }
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
                        onClick = {
                            sharePoster { bitmap -> shareImageToInstagramStories(context, bitmap, periodTitle) }
                        }
                    )
                    ShareActionPill(
                        iconRes = R.drawable.lucide_ic_share_2,
                        label = stringResource(AppR.string.share_more),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            sharePoster { bitmap -> shareImageWithApp(context, bitmap, periodTitle, null) }
                        }
                    )
                }
            }
        }
    }
}

private fun extractAuroraColors(bitmap: Bitmap?): Triple<Int, Int, Int> {
    if (bitmap == null) {
        return Triple("#FF9F43".toColorInt(), "#00C9A7".toColorInt(), "#FF5252".toColorInt())
    }
    return try {
        val w = bitmap.width
        val h = bitmap.height
        val p1 = bitmap.getPixel((w * 0.25f).toInt().coerceIn(0, w - 1), (h * 0.25f).toInt().coerceIn(0, h - 1))
        val p2 = bitmap.getPixel((w * 0.75f).toInt().coerceIn(0, w - 1), (h * 0.35f).toInt().coerceIn(0, h - 1))
        val p3 = bitmap.getPixel((w * 0.50f).toInt().coerceIn(0, w - 1), (h * 0.80f).toInt().coerceIn(0, h - 1))

        Triple(boostSaturation(p1, "#FF9F43"), boostSaturation(p2, "#00C9A7"), boostSaturation(p3, "#FF5252"))
    } catch (e: Exception) {
        Triple("#FF9F43".toColorInt(), "#00C9A7".toColorInt(), "#FF5252".toColorInt())
    }
}

private fun boostSaturation(argb: Int, fallbackHex: String): Int {
    val r = (argb ushr 16 and 0xFF) / 255f
    val g = (argb ushr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), hsv)

    if (hsv[1] < 0.20f) {
        return fallbackHex.toColorInt()
    }
    hsv[1] = hsv[1].coerceAtLeast(0.70f)
    hsv[2] = hsv[2].coerceIn(0.75f, 0.98f)

    return android.graphics.Color.HSVToColor(hsv)
}

private fun truncateText(paint: Paint, text: String, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var truncated = text
    while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
        truncated = truncated.dropLast(1)
    }
    return if (truncated.isEmpty()) text else "$truncated..."
}
