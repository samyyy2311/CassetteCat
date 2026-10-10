package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LyricCardTheme {
    ATMOSPHERE,
    OBSIDIAN
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
    val trackCredit = stringResource(AppR.string.share_track_credit, song.title, song.artist)
    val lyricsClipboardLabel = stringResource(AppR.string.share_mode_lyrics)
    val lyricsCopiedMessage = stringResource(AppR.string.share_lyrics_copied)

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
                    text = stringResource(AppR.string.share_lyric_quote_title),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                LyricSharePreviewCard(
                    song = song,
                    lines = selectedLines,
                    theme = selectedTheme
                )
            }

            Spacer(Modifier.height(20.dp))

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
                        val fullText = selectedLines.joinToString("\n") + "\n\n$trackCredit"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(lyricsClipboardLabel, fullText))
                        Toast.makeText(context, lyricsCopiedMessage, Toast.LENGTH_SHORT).show()
                    }
                )

                ShareImageActions { target ->
                    scope.launch {
                        val artBitmap = loadSongArtwork(context, song)
                        val bitmap = withContext(Dispatchers.Default) {
                            generateSharePoster(context, song, ShareCardMode.LYRICS, selectedLines, selectedTheme, artBitmap)
                        }
                        shareImage(context, bitmap, trackCredit, target)
                    }
                }
            }
        }
    }
}

@Composable
internal fun LyricCardThemePicker(selected: LyricCardTheme, onSelect: (LyricCardTheme) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LyricCardTheme.entries.forEach { theme ->
            val isSelected = selected == theme
            val bgColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
            val borderColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            val textColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(if (isSelected) 1.dp else 0.5.dp, borderColor, CircleShape)
                    .clickable { onSelect(theme) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = lyricCardThemeLabel(theme),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
internal fun lyricCardThemeLabel(theme: LyricCardTheme): String = stringResource(
    when (theme) {
        LyricCardTheme.ATMOSPHERE -> AppR.string.share_theme_atmosphere
        LyricCardTheme.OBSIDIAN -> AppR.string.share_theme_obsidian
    }
)
