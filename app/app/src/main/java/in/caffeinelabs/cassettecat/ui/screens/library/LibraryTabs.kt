package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.FastScrollIndexRail
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.launch

@Composable
internal fun LibraryViewModeTabs(selected: LibraryViewMode, onSelect: (LibraryViewMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LibraryViewMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .tapScale { onSelect(mode) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(mode.label, style = MaterialTheme.typography.titleMedium, color = tint)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 20.dp else 0.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(tint)
                )
            }
        }
    }
}

@Composable
internal fun SongsTabContent(
    filteredSongs: List<Song>,
    songFilter: SongFilter,
    collectionLayout: CollectionLayout,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    songGridState: LazyGridState,
    songListState: LazyListState,
    listBottomPadding: Dp,
    onPlayOrSelect: (Song) -> Unit,
    onToggleSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())
    val gridColumns = preferences.gridColumnCount

    if (filteredSongs.isEmpty()) {
        EmptyState(
            iconRes = R.drawable.lucide_ic_music,
            title = "No ${songFilter.label.lowercase()} found",
            message = "Try a different filter.",
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            if (collectionLayout == CollectionLayout.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    state = songGridState,
                    contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 8.dp, bottom = listBottomPadding + 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(filteredSongs, key = { it.id }) { song ->
                        SongGridCard(
                            song = song,
                            selected = song.id in selectedIds,
                            onClick = { onPlayOrSelect(song) },
                            onLongClick = { onToggleSelect(song.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = songListState,
                    contentPadding = PaddingValues(start = 0.dp, end = 24.dp, top = 8.dp, bottom = listBottomPadding)
                ) {
                    items(filteredSongs, key = { it.id }) { song ->
                        SelectableSongRow(
                            song = song,
                            selected = song.id in selectedIds,
                            selectionMode = selectionMode,
                            onClick = { onPlayOrSelect(song) },
                            onLongClick = { onToggleSelect(song.id) }
                        )
                    }
                }
            }

            FastScrollIndexRail(
                items = filteredSongs,
                labelExtractor = { it.title },
                itemNoun = "track",
                bottomPadding = listBottomPadding,
                onScrollToIndex = { index ->
                    coroutineScope.launch {
                        if (collectionLayout == CollectionLayout.GRID) {
                            songGridState.scrollToItem(index)
                        } else {
                            songListState.scrollToItem(index)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
internal fun ArtistsTabContent(
    filteredSongs: List<Song>,
    sortOrder: ArtistSortOrder,
    sortDirection: SortDirection,
    collectionLayout: CollectionLayout,
    gridState: LazyGridState,
    listState: LazyListState,
    listBottomPadding: Dp,
    onNavigateToArtist: (String) -> Unit,
    onPlayGroup: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())
    val gridColumns = preferences.gridColumnCount

    val artists = remember(filteredSongs, sortOrder, sortDirection) {
        val comparator = sortOrder.comparator()
            .let { if (sortDirection == SortDirection.DESCENDING) it.reversed() else it }
        filteredSongs.groupedByArtist().sortedWith(comparator)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (collectionLayout == CollectionLayout.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = listBottomPadding + 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(artists, key = { it.artist }) { group ->
                    ArtistCard(
                        group = group,
                        onClick = { onNavigateToArtist(group.artist) },
                        onPlay = { onPlayGroup(group.songs) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(start = 0.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding)
            ) {
                items(artists, key = { it.artist }) { group ->
                    ArtistListRow(
                        group = group,
                        onClick = { onNavigateToArtist(group.artist) },
                        onPlay = { onPlayGroup(group.songs) }
                    )
                }
            }
        }

        FastScrollIndexRail(
            items = artists,
            labelExtractor = { it.artist },
            itemNoun = "artist",
            bottomPadding = listBottomPadding,
            onScrollToIndex = { index ->
                coroutineScope.launch {
                    if (collectionLayout == CollectionLayout.GRID) {
                        gridState.scrollToItem(index)
                    } else {
                        listState.scrollToItem(index)
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
internal fun AlbumsTabContent(
    filteredSongs: List<Song>,
    sortOrder: AlbumSortOrder,
    sortDirection: SortDirection,
    collectionLayout: CollectionLayout,
    gridState: LazyGridState,
    listState: LazyListState,
    listBottomPadding: Dp,
    onNavigateToAlbum: (String) -> Unit,
    onPlayGroup: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())
    val gridColumns = preferences.gridColumnCount

    val albums = remember(filteredSongs, sortOrder, sortDirection) {
        val comparator = sortOrder.comparator()
            .let { if (sortDirection == SortDirection.DESCENDING) it.reversed() else it }
        filteredSongs.groupedByAlbum().sortedWith(comparator)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (collectionLayout == CollectionLayout.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = listBottomPadding + 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(albums, key = { it.albumId }) { group ->
                    AlbumCard(
                        group = group,
                        onClick = { onNavigateToAlbum(group.albumId) },
                        onPlay = { onPlayGroup(group.songs) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(start = 0.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding)
            ) {
                items(albums, key = { it.albumId }) { group ->
                    AlbumListRow(
                        group = group,
                        onClick = { onNavigateToAlbum(group.albumId) },
                        onPlay = { onPlayGroup(group.songs) }
                    )
                }
            }
        }

        FastScrollIndexRail(
            items = albums,
            labelExtractor = { it.album },
            itemNoun = "album",
            bottomPadding = listBottomPadding,
            onScrollToIndex = { index ->
                coroutineScope.launch {
                    if (collectionLayout == CollectionLayout.GRID) {
                        gridState.scrollToItem(index)
                    } else {
                        listState.scrollToItem(index)
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
internal fun GenresTabContent(
    filteredSongs: List<Song>,
    sortOrder: GenreSortOrder,
    sortDirection: SortDirection,
    collectionLayout: CollectionLayout,
    gridState: LazyGridState,
    listState: LazyListState,
    listBottomPadding: Dp,
    onNavigateToGenre: (String) -> Unit,
    onPlayGroup: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())
    val gridColumns = preferences.gridColumnCount

    val genres = remember(filteredSongs, sortOrder, sortDirection) {
        val comparator = sortOrder.comparator()
            .let { if (sortDirection == SortDirection.DESCENDING) it.reversed() else it }
        filteredSongs.groupedByGenre().sortedWith(comparator)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (collectionLayout == CollectionLayout.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = listBottomPadding + 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(genres, key = { it.genre }) { group ->
                    GenreCard(
                        group = group,
                        onClick = { onNavigateToGenre(group.genre) },
                        onPlay = { onPlayGroup(group.songs) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(start = 0.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding)
            ) {
                items(genres, key = { it.genre }) { group ->
                    GenreListRow(
                        group = group,
                        onClick = { onNavigateToGenre(group.genre) },
                        onPlay = { onPlayGroup(group.songs) }
                    )
                }
            }
        }

        FastScrollIndexRail(
            items = genres,
            labelExtractor = { it.genre },
            itemNoun = "genre",
            bottomPadding = listBottomPadding,
            onScrollToIndex = { index ->
                coroutineScope.launch {
                    if (collectionLayout == CollectionLayout.GRID) {
                        gridState.scrollToItem(index)
                    } else {
                        listState.scrollToItem(index)
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
