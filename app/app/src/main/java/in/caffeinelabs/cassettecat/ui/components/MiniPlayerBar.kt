package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

private const val MINI_PLAYER_SNAP_MS = 220
private val SmoothEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

// Collapsed content for MainShell's drag sheet; no bg/border of its own, flush with the
// sheet's surface. Tapping outside the transport buttons expands the sheet (onExpand).
@Composable
fun MiniPlayerRow(
    playbackViewModel: PlaybackViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    // Reports thumbnail bounds so NowPlayingContent can morph art into this spot on
    // collapse instead of cross-fading. No-op by default.
    onThumbnailBoundsChange: (Rect) -> Unit = {}
) {
    val state by playbackViewModel.playbackState.collectAsState()
    val song = state.currentSong ?: return
    val previousSong = state.previousInQueue
    val nextSong = state.upNext.firstOrNull()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            // The parent sheet is transparent while collapsed so the app navigation below can
            // behave as glass. Keep the mini player readable as its own distinct surface.
            .background(
                // The mini player carries primary playback controls, so unlike the nav glass
                // it must completely block content scrolling underneath it.
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .clickable(onClick = hapticClick(onExpand))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            MiniPlayerArtRow(
                currentSong = song,
                previousSong = previousSong,
                nextSong = nextSong,
                onSwipeNext = { playbackViewModel.skipNext() },
                onSwipePrevious = { playbackViewModel.skipPrevious() },
                onThumbnailBoundsChange = onThumbnailBoundsChange
            )
        }
        Spacer(Modifier.width(12.dp))
        TransportButton(
            iconRes = if (state.isPlaying) R.drawable.lucide_ic_pause else R.drawable.lucide_ic_play,
            size = 44.dp,
            tint = MaterialTheme.colorScheme.tertiary,
            onClick = { playbackViewModel.togglePlayPause() },
            accented = state.isPlaying
        )
        Spacer(Modifier.width(12.dp))
        TransportButton(
            iconRes = R.drawable.lucide_ic_skip_forward,
            size = 40.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { playbackViewModel.skipNext() }
        )
    }
}

// Same pattern as NowPlayingScreen's AlbumArtCarousel: [previousSong, currentSong, nextSong]
// form a sliding window in a real HorizontalPager, not hand-rolled drag/position math.
@Composable
private fun MiniPlayerArtRow(
    currentSong: Song,
    previousSong: Song?,
    nextSong: Song?,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onThumbnailBoundsChange: (Rect) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    key(currentSong.id, previousSong?.id, nextSong?.id) {
        val windowSongs = remember(currentSong.id, previousSong?.id, nextSong?.id) {
            buildList {
                if (previousSong != null) add(previousSong)
                add(currentSong)
                if (nextSong != null) add(nextSong)
            }
        }
        val currentIndex = if (previousSong != null) 1 else 0
        val pagerState = rememberPagerState(initialPage = currentIndex) { windowSongs.size }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { settled ->
                if (settled != currentIndex) {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    if (settled > currentIndex) onSwipeNext() else onSwipePrevious()
                    pagerState.animateScrollToPage(currentIndex)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = tween(MINI_PLAYER_SNAP_MS, easing = SmoothEasing)
            )
        ) { page ->
            MiniPlayerSongRow(
                song = windowSongs[page],
                onThumbnailBoundsChange = if (page == currentIndex) onThumbnailBoundsChange else { _ -> }
            )
        }
    }
}

@Composable
private fun MiniPlayerSongRow(
    song: Song,
    modifier: Modifier = Modifier,
    onThumbnailBoundsChange: (Rect) -> Unit = {}
) {
    Row(modifier = modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .onGloballyPositioned { onThumbnailBoundsChange(it.boundsInWindow()) }
        ) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
