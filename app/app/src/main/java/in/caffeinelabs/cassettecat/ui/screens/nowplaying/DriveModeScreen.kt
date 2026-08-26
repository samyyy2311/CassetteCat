package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel

@Composable
fun DriveModeScreen(
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val isFavorite by playbackViewModel.isCurrentSongFavorite.collectAsStateWithLifecycle()
    val positionMs by playbackViewModel.positionMs.collectAsStateWithLifecycle()

    val song = playbackState.currentSong
    val isPlaying = playbackState.isPlaying
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val gestureModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = { playbackViewModel.togglePlayPause() },
            onDoubleTap = { playbackViewModel.toggleFavoriteForCurrentSong() }
        )
    }.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { dragAccumulator = 0f },
            onDragEnd = {
                if (dragAccumulator < -60f) {
                    playbackViewModel.skipNext()
                } else if (dragAccumulator > 60f) {
                    playbackViewModel.skipPrevious()
                }
                dragAccumulator = 0f
            },
            onHorizontalDrag = { _, dragAmount ->
                dragAccumulator += dragAmount
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Bar matching CassetteCat style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_chevron_left,
                    contentDescription = "Exit Drive Mode",
                    onClick = onBack
                )

                Text(
                    "Drive Mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.size(40.dp))
            }

            if (song == null) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        iconRes = R.drawable.lucide_ic_music,
                        title = "No track playing",
                        message = "Play a song from your library to start listening in Drive Mode"
                    )
                }
            } else if (isLandscape) {
                // Landscape layout matching NowPlayingPlayerView
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.88f)
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .then(gestureModifier)
                    ) {
                        AlbumArtCard(
                            song = song,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(28.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        TitleRow(
                            song = song,
                            isFavorite = isFavorite,
                            showThumbnail = false,
                            audioFormat = playbackState.audioFormat,
                            onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                            onShowMenu = {},
                            onArtistClick = {}
                        )
                        Spacer(Modifier.height(28.dp))
                        PlaybackControlsRow(
                            positionMs = positionMs,
                            durationMs = playbackState.durationMs,
                            onSeek = { playbackViewModel.seekTo(it) },
                            isShuffleEnabled = playbackState.isShuffleEnabled,
                            onToggleShuffle = { playbackViewModel.toggleShuffle() },
                            onSkipPrevious = { playbackViewModel.skipPrevious() },
                            isPlaying = playbackState.isPlaying,
                            playWhenReady = playbackState.playWhenReady,
                            onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                            onSkipNext = { playbackViewModel.skipNext() },
                            repeatMode = playbackState.repeatMode,
                            onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() }
                        )
                    }
                }
            } else {
                // Portrait layout matching NowPlayingPlayerView
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(Modifier.weight(3f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .align(Alignment.CenterHorizontally)
                            .then(gestureModifier)
                    ) {
                        AlbumArtCard(
                            song = song,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.weight(2.5f))
                    TitleRow(
                        song = song,
                        isFavorite = isFavorite,
                        showThumbnail = false,
                        audioFormat = playbackState.audioFormat,
                        onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                        onShowMenu = {},
                        onArtistClick = {}
                    )
                    Spacer(Modifier.height(24.dp))
                    PlaybackControlsRow(
                        positionMs = positionMs,
                        durationMs = playbackState.durationMs,
                        onSeek = { playbackViewModel.seekTo(it) },
                        isShuffleEnabled = playbackState.isShuffleEnabled,
                        onToggleShuffle = { playbackViewModel.toggleShuffle() },
                        onSkipPrevious = { playbackViewModel.skipPrevious() },
                        isPlaying = playbackState.isPlaying,
                        playWhenReady = playbackState.playWhenReady,
                        onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                        onSkipNext = { playbackViewModel.skipNext() },
                        repeatMode = playbackState.repeatMode,
                        onCycleRepeatMode = { playbackViewModel.cycleRepeatMode() }
                    )
                    Spacer(Modifier.weight(1.5f))
                }
            }
        }
    }
}
