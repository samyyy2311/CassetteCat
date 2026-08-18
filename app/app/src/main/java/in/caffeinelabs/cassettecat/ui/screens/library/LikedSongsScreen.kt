package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily

/** A first-class collection, not a redirect to a filtered version of Library → Songs. */
@Composable
fun LikedSongsScreen(
    libraryViewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val favoritesRepository = remember { FavoritesRepository(context) }
    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val allSongs = (uiState as? LibraryUiState.Loaded)?.songs.orEmpty()
    val songs = remember(allSongs, favoriteIds) {
        allSongs.filter { it.isFavorite || it.id in favoriteIds }
    }

    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val downloadableSongs = remember(songs) { songs.filter { it.source != MusicSource.Local } }

    fun playAll(shuffle: Boolean) {
        if (songs.isEmpty()) return
        val queue = if (shuffle) songs.shuffled() else songs
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        playbackViewModel.playQueue(queue, 0)
        if (wasIdle) onNavigateToNowPlaying()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Liked Songs", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (songs.size == 1) "1 song" else "${songs.size} songs",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (downloadableSongs.isNotEmpty()) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_download,
                    contentDescription = "Download liked songs",
                    onClick = { downloadableSongs.forEach(downloadRepository::download) }
                )
                Spacer(Modifier.width(4.dp))
            }
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 42.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = { playAll(shuffle = false) }
            )
            Spacer(Modifier.width(8.dp))
            TransportButton(
                iconRes = R.drawable.lucide_ic_shuffle,
                size = 42.dp,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = { playAll(shuffle = true) }
            )
        }

        if (songs.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_heart,
                title = "No liked songs yet",
                message = "Tap the heart on any track to keep it here.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = listBottomPadding)
            ) {
                items(songs, key = { it.id }) { song ->
                    LibrarySongRow(
                        song = song,
                        onClick = {
                            val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                            val index = songs.indexOfFirst { it.id == song.id }
                            playbackViewModel.playQueue(songs, index)
                            if (wasIdle) onNavigateToNowPlaying()
                        }
                    )
                }
            }
        }
    }
}
