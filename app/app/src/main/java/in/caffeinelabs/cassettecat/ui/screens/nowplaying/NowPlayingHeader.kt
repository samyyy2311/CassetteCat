package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

@Composable
internal fun TitleRow(
    song: Song,
    isFavorite: Boolean,
    showThumbnail: Boolean,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    onArtistClick: () -> Unit = {},
    onThumbnailClick: () -> Unit = {},
    onCollapseRequest: () -> Unit = {},
    onHeaderDrag: (Float) -> Unit = {},
    onHeaderSpringBack: () -> Unit = {},
    modifier: Modifier = Modifier
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
                if (showThumbnail) {
                    Modifier.pointerInput(Unit) {
                        val collapseThresholdPx = with(density) { HEADER_COLLAPSE_DRAG_THRESHOLD.toPx() }
                        val flingVelocityPx = with(density) { HEADER_COLLAPSE_FLING_VELOCITY.toPx() }
                        var netDragPx = 0f
                        var velocityTracker = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = {
                                netDragPx = 0f
                                velocityTracker = VelocityTracker()
                            },
                            onDragCancel = {
                                netDragPx = 0f
                                currentOnHeaderSpringBack()
                            },
                            onDragEnd = {
                                val flingVelocity = velocityTracker.calculateVelocity().y
                                if (netDragPx > collapseThresholdPx || flingVelocity > flingVelocityPx) {
                                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                    currentOnCollapseRequest()
                                } else {
                                    currentOnHeaderSpringBack()
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                netDragPx += dragAmount
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                currentOnHeaderDrag(dragAmount)
                            }
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
                AlbumArt(song = song, modifier = Modifier.fillMaxSize())
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
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.tapScale(onArtistClick)
            )
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
