package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomRole
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackUiState
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton

@Composable
internal fun RadioNowPlayingView(
    song: Song,
    state: PlaybackUiState,
    isFavorite: Boolean,
    listeningRoomState: ListeningRoomState,
    expandFraction: Float,
    collapsedArtRect: State<Rect?>?,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val art: @Composable (Modifier) -> Unit = { modifier ->
        AlbumArtCard(
            song = song,
            collapsedArtRect = collapsedArtRect,
            expandFraction = expandFraction,
            onSwipeUp = null,
            isPlaying = state.isPlaying,
            modifier = modifier
        )
    }

    // A station has no artist/album page and no lyrics/queue to navigate to,
    // so the header stays informational rather than a set of dead-end taps.
    val info: @Composable () -> Unit = {
        TitleRow(
            song = song,
            isFavorite = isFavorite,
            showThumbnail = false,
            onToggleFavorite = onToggleFavorite,
            onShowMenu = onShowMenu,
            onArtistClick = {}
        )
        val metaParts = buildList {
            if (song.bitrateKbps > 0) add("${song.bitrateKbps} kbps")
            addAll(song.genres.take(4))
        }
        if (metaParts.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                metaParts.joinToString(" · "),
                style = readoutStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (listeningRoomState.role != ListeningRoomRole.NONE) {
            Spacer(Modifier.height(8.dp))
            ListeningRoomStatusPill(listeningRoomState)
        }
        Spacer(Modifier.height(32.dp))
        RadioControlsRow(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            playWhenReady = state.playWhenReady,
            onTogglePlayPause = onTogglePlayPause
        )
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            art(
                Modifier
                    .fillMaxHeight(0.85f)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
            )
            Spacer(Modifier.width(28.dp))
            Column(modifier = Modifier.weight(1f)) { info() }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.weight(2.5f))
            art(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .aspectRatio(1f)
            )
            Spacer(Modifier.weight(2f))
            info()
            Spacer(Modifier.weight(1.5f))
        }
    }
}

@Composable
internal fun RadioControlsRow(
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    playWhenReady: Boolean,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        LiveIndicator(isPlaying = isPlaying, isBuffering = isBuffering)
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Crossfade(
                targetState = playWhenReady,
                animationSpec = tween(durationMillis = 150, easing = SmoothEasing),
                label = "radioPlayPause"
            ) { playing ->
                TransportButton(
                    iconRes = if (playing) R.drawable.lucide_ic_pause else R.drawable.lucide_ic_play,
                    size = 90.dp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    onClick = onTogglePlayPause,
                    accented = playing
                )
            }
        }
    }
}
