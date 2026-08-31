package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
internal fun LibraryViewModeTabs(
    modes: List<LibraryViewMode>,
    selected: LibraryViewMode,
    onSelect: (LibraryViewMode) -> Unit
) {
    val scrollState = rememberScrollState()
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)

    LaunchedEffect(selectedIndex) {
        if (modes.size > 4) {
            // Smoothly scroll active tab into view with nice padding
            val targetOffset = (selectedIndex * 180 - 100).coerceAtLeast(0)
            scrollState.animateScrollTo(targetOffset)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selected
            val tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .tapScale { onSelect(mode) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = tint
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 22.dp else 0.dp)
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
    onPlaySong: (Song) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSongMore: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val gridColumns = preferences.gridColumnCount

    if (filteredSongs.isEmpty()) {
        EmptyState(
            iconRes = R.drawable.lucide_ic_music,
            title = "No ${songFilter.label.lowercase()} found",
            message = "Try a different filter.",
            modifier = modifier.fillMaxSize()
        )
    } else {
        val firstVisibleIndex by remember(collectionLayout) {
            derivedStateOf {
                if (collectionLayout == CollectionLayout.GRID) songGridState.firstVisibleItemIndex else songListState.firstVisibleItemIndex
            }
        }
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
                            onClick = {
                                if (selectionMode) {
                                    onToggleSelect(song.id)
                                } else {
                                    onPlaySong(song)
                                }
                            },
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
                            onMoreClick = if (onSongMore != null && !selectionMode) { { onSongMore(song) } } else null,
                            onClick = {
                                if (selectionMode) {
                                    onToggleSelect(song.id)
                                } else {
                                    onPlaySong(song)
                                }
                            },
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
                firstVisibleIndex = firstVisibleIndex,
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
    selectedIds: Set<String> = emptySet(),
    selectionMode: Boolean = false,
    onToggleSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val gridColumns = preferences.gridColumnCount

    val artists = remember(filteredSongs, sortOrder, sortDirection) {
        val comparator = sortOrder.comparator()
            .let { if (sortDirection == SortDirection.DESCENDING) it.reversed() else it }
        filteredSongs.groupedByArtist().sortedWith(comparator)
    }

    val firstVisibleIndex by remember(collectionLayout) {
        derivedStateOf {
            if (collectionLayout == CollectionLayout.GRID) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
        }
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
                        selected = group.artist in selectedIds,
                        onClick = { if (selectionMode) onToggleSelect(group.artist) else onNavigateToArtist(group.artist) },
                        onLongClick = { onToggleSelect(group.artist) }
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
                        selected = group.artist in selectedIds,
                        selectionMode = selectionMode,
                        onClick = { if (selectionMode) onToggleSelect(group.artist) else onNavigateToArtist(group.artist) },
                        onLongClick = { onToggleSelect(group.artist) }
                    )
                }
            }
        }

        FastScrollIndexRail(
            items = artists,
            labelExtractor = { it.artist },
            itemNoun = "artist",
            bottomPadding = listBottomPadding,
            firstVisibleIndex = firstVisibleIndex,
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
    selectedIds: Set<String> = emptySet(),
    selectionMode: Boolean = false,
    onToggleSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val gridColumns = preferences.gridColumnCount

    val albums = remember(filteredSongs, sortOrder, sortDirection) {
        val comparator = sortOrder.comparator()
            .let { if (sortDirection == SortDirection.DESCENDING) it.reversed() else it }
        filteredSongs.groupedByAlbum().sortedWith(comparator)
    }

    val firstVisibleIndex by remember(collectionLayout) {
        derivedStateOf {
            if (collectionLayout == CollectionLayout.GRID) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
        }
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
                        selected = group.albumId in selectedIds,
                        onClick = { if (selectionMode) onToggleSelect(group.albumId) else onNavigateToAlbum(group.albumId) },
                        onLongClick = { onToggleSelect(group.albumId) }
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
                        selected = group.albumId in selectedIds,
                        selectionMode = selectionMode,
                        onClick = { if (selectionMode) onToggleSelect(group.albumId) else onNavigateToAlbum(group.albumId) },
                        onLongClick = { onToggleSelect(group.albumId) }
                    )
                }
            }
        }

        FastScrollIndexRail(
            items = albums,
            labelExtractor = { it.album },
            itemNoun = "album",
            bottomPadding = listBottomPadding,
            firstVisibleIndex = firstVisibleIndex,
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
    selectedIds: Set<String> = emptySet(),
    selectionMode: Boolean = false,
    onToggleSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val gridColumns = preferences.gridColumnCount

    val genres = remember(filteredSongs, sortOrder, sortDirection) {
        val comparator = sortOrder.comparator()
            .let { if (sortDirection == SortDirection.DESCENDING) it.reversed() else it }
        filteredSongs.groupedByGenre().sortedWith(comparator)
    }

    val firstVisibleIndex by remember(collectionLayout) {
        derivedStateOf {
            if (collectionLayout == CollectionLayout.GRID) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
        }
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
                        selected = group.genre in selectedIds,
                        onClick = { if (selectionMode) onToggleSelect(group.genre) else onNavigateToGenre(group.genre) },
                        onLongClick = { onToggleSelect(group.genre) },
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
                        selected = group.genre in selectedIds,
                        selectionMode = selectionMode,
                        onClick = { if (selectionMode) onToggleSelect(group.genre) else onNavigateToGenre(group.genre) },
                        onLongClick = { onToggleSelect(group.genre) },
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
            firstVisibleIndex = firstVisibleIndex,
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
internal fun FoldersTab(
    folders: List<FolderGroup>,
    collectionLayout: CollectionLayout,
    gridColumns: Int = 2,
    gridState: LazyGridState,
    listState: LazyListState,
    listBottomPadding: Dp,
    onNavigateToFolder: (String) -> Unit,
    onPlayGroup: (List<Song>) -> Unit,
    onChangeCover: (FolderGroup) -> Unit,
    selectedIds: Set<String> = emptySet(),
    selectionMode: Boolean = false,
    onToggleSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val firstVisibleIndex by remember {
        derivedStateOf {
            if (collectionLayout == CollectionLayout.GRID) {
                gridState.firstVisibleItemIndex
            } else {
                listState.firstVisibleItemIndex
            }
        }
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
                items(folders, key = { it.folderPath }) { group ->
                    FolderCard(
                        group = group,
                        selected = group.folderPath in selectedIds,
                        onClick = { if (selectionMode) onToggleSelect(group.folderPath) else onNavigateToFolder(group.folderPath) },
                        onLongClick = { if (selectionMode) onToggleSelect(group.folderPath) else onChangeCover(group) },
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
                items(folders, key = { it.folderPath }) { group ->
                    FolderListRow(
                        group = group,
                        selected = group.folderPath in selectedIds,
                        selectionMode = selectionMode,
                        onClick = { if (selectionMode) onToggleSelect(group.folderPath) else onNavigateToFolder(group.folderPath) },
                        onLongClick = { if (selectionMode) onToggleSelect(group.folderPath) else onChangeCover(group) },
                        onPlay = { onPlayGroup(group.songs) }
                    )
                }
            }
        }

        FastScrollIndexRail(
            items = folders,
            labelExtractor = { it.folderName },
            itemNoun = "folder",
            bottomPadding = listBottomPadding,
            firstVisibleIndex = firstVisibleIndex,
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
