package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ShareCardMode {
    SONG,
    LYRICS
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
    val trackCredit = stringResource(AppR.string.share_track_credit, song.title, song.artist)
    val copiedMessage = stringResource(AppR.string.share_copied)
    val appName = stringResource(AppR.string.app_name)

    ShareSheet(title = stringResource(AppR.string.share_card_title), onDismiss = onDismiss) {

        Spacer(Modifier.height(12.dp))

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
                            text = shareCardModeLabel(entry),
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
                    text = stringResource(AppR.string.share_select_different_lyrics),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(20.dp))
        }

        LyricCardThemePicker(selected = selectedTheme, onSelect = { selectedTheme = it })

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShareActionPill(
                iconRes = R.drawable.lucide_ic_copy,
                label = stringResource(AppR.string.share_copy_text),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                iconTint = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    val text = if (mode == ShareCardMode.SONG) {
                        trackCredit
                    } else {
                        "${availableLyrics.joinToString("\n")}\n\n$trackCredit"
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(appName, text))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }
            )

            ShareImageActions { target ->
                scope.launch {
                    val artBitmap = loadSongArtwork(context, song)
                    val bitmap = withContext(Dispatchers.Default) {
                        generateSharePoster(context, song, mode, availableLyrics, selectedTheme, artBitmap)
                    }
                    shareImage(context, bitmap, trackCredit, target, selectedTheme)
                }
            }
        }
    }
}

@Composable
private fun shareCardModeLabel(mode: ShareCardMode): String = stringResource(
    when (mode) {
        ShareCardMode.SONG -> AppR.string.share_mode_song
        ShareCardMode.LYRICS -> AppR.string.share_mode_lyrics
    }
)

/** What every share preview sits on: the theme's background, with the cover blurred behind it for Atmosphere. */
@Composable
private fun ShareCardBackground(
    song: Song,
    theme: LyricCardTheme,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
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

        content()
    }
}

@Composable
private fun SongSharePreviewCard(
    song: Song,
    theme: LyricCardTheme,
    modifier: Modifier = Modifier
) {
    ShareCardBackground(song, theme, modifier) {
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
                AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
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
                    text = stringResource(AppR.string.app_name),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
internal fun LyricSharePreviewCard(
    song: Song,
    lines: List<String>,
    theme: LyricCardTheme,
    modifier: Modifier = Modifier
) {
    ShareCardBackground(song, theme, modifier) {
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
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
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
                    text = stringResource(AppR.string.app_name),
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
