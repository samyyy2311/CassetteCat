package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel

@Composable
fun LikedSongsScreen(
    libraryViewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val favoritesRepository = remember { FavoritesRepository(context) }
    val favoriteIds by favoritesRepository.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val allSongs = (uiState as? LibraryUiState.Loaded)?.songs.orEmpty()
    val songs = remember(allSongs, favoriteIds) {
        allSongs.filter { it.isFavorite || it.id in favoriteIds }
    }

    fun playAll(shuffle: Boolean) {
        playbackViewModel.play(songs, shuffle = shuffle, onFirstPlay = onNavigateToNowPlaying)
    }

    val isRefreshing by libraryViewModel.isRefreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { libraryViewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SongCollectionHeader(
                title = stringResource(AppR.string.library_liked_songs_title),
                songs = songs,
                downloadDescription = stringResource(AppR.string.desc_download_liked_songs),
                onBack = onBack,
                onPlay = ::playAll
            )

            if (songs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    EmptyState(
                        catRes = AppR.drawable.cat_gray_dancing,
                        title = stringResource(AppR.string.liked_empty_title),
                        message = stringResource(AppR.string.liked_empty_message),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 12.dp, bottom = listBottomPadding)
                ) {
                    items(songs, key = { it.id }) { song ->
                        LibrarySongRow(
                            song = song,
                            onClick = {
                                playbackViewModel.play(songs, songs.indexOfFirst { it.id == song.id }, onFirstPlay = onNavigateToNowPlaying)
                            }
                        )
                    }
                }
            }
        }
    }
}
