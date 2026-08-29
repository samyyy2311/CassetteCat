package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.playback.AudioTrackFormat
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.BluetoothOutputLabel
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

@Composable
internal fun AudioQualityBadge(audioFormat: AudioTrackFormat, modifier: Modifier = Modifier) {
    var showSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(
                if (audioFormat.isLossless) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                0.5.dp,
                if (audioFormat.isLossless) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(3.dp)
            )
            .clickable(onClick = hapticClick { showSheet = true })
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
    ) {
        Text(
            text = audioFormat.badgeLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = if (audioFormat.isLossless) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.3.sp
            ),
            color = if (audioFormat.isLossless) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }

    if (showSheet) {
        AudioDetailsSheet(audioFormat = audioFormat, onDismiss = { showSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioDetailsSheet(
    audioFormat: AudioTrackFormat,
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (audioFormat.isHiRes) "Hi-Res Lossless Audio" else if (audioFormat.isLossless) "Lossless Audio" else "Audio Quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = audioFormat.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(16.dp))

            AudioDetailItem("Format", audioFormat.codecName)
            if (audioFormat.sampleRateHz > 0) {
                AudioDetailItem("Sample Rate", "${audioFormat.sampleRateHz} Hz (${audioFormat.sampleRateHz / 1000f} kHz)")
            }
            if (audioFormat.bitDepth > 0) {
                AudioDetailItem("Bit Depth", "${audioFormat.bitDepth}-bit")
            }
            if (audioFormat.bitrateKbps > 0) {
                AudioDetailItem("Bitrate", "${audioFormat.bitrateKbps} kbps")
            }
            AudioDetailItem("Encoding", if (audioFormat.isLossless) "Lossless Uncompressed" else "Lossy Compressed")

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AudioDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun TitleRow(
    song: Song,
    isFavorite: Boolean,
    showThumbnail: Boolean,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    modifier: Modifier = Modifier,
    audioFormat: AudioTrackFormat? = null,
    onArtistClick: () -> Unit = {},
    onThumbnailClick: () -> Unit = {},
    onCollapseRequest: () -> Unit = {},
    onHeaderDrag: (Float) -> Unit = {},
    onHeaderSpringBack: () -> Unit = {},
    enableHeaderDrag: Boolean = false
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val currentOnCollapseRequest by rememberUpdatedState(onCollapseRequest)
    val currentOnHeaderDrag by rememberUpdatedState(onHeaderDrag)
    val currentOnHeaderSpringBack by rememberUpdatedState(onHeaderSpringBack)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enableHeaderDrag) {
                    Modifier.pointerInput(Unit) {
                        val thresholdPx = with(density) { HEADER_COLLAPSE_DRAG_THRESHOLD.toPx() }
                        val flingThresholdPx = with(density) { HEADER_COLLAPSE_FLING_VELOCITY.toPx() }
                        val velocityTracker = VelocityTracker()
                        var totalDragPx = 0f
                        detectVerticalDragGestures(
                            onDragStart = {
                                totalDragPx = 0f
                                velocityTracker.resetTracking()
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0f || totalDragPx > 0f) {
                                    change.consume()
                                    totalDragPx += dragAmount
                                    currentOnHeaderDrag(dragAmount)
                                    velocityTracker.addPointerInputChange(change)
                                }
                            },
                            onDragEnd = {
                                val flingVelocity = velocityTracker.calculateVelocity().y
                                if (totalDragPx > thresholdPx || flingVelocity > flingThresholdPx) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentOnCollapseRequest()
                                } else {
                                    currentOnHeaderSpringBack()
                                }
                            },
                            onDragCancel = { currentOnHeaderSpringBack() }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showThumbnail) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = hapticClick(onThumbnailClick))
            ) {
                AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = if (showThumbnail) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.tapScale(onArtistClick)
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                audioFormat?.let {
                    AudioQualityBadge(audioFormat = it)
                }
                BluetoothOutputLabel()
            }
        }
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_heart,
            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
            tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onToggleFavorite
        )
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_ellipsis_vertical,
            contentDescription = "More",
            onClick = onShowMenu
        )
    }
}
