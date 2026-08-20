package `in`.caffeinelabs.cassettecat.ui.screens.library

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.matchM3uEntries
import `in`.caffeinelabs.cassettecat.data.library.parseM3u
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.shareSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LibraryScreen(
    playbackViewModel: PlaybackViewModel,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToGenre: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToLikedSongs: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp,
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit = {},
    viewModel: LibraryViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val loadedState = uiState as? LibraryUiState.Loaded
    val isRefreshing = uiState is LibraryUiState.Loading
    val context = LocalContext.current
    val appPreferencesRepository = remember { `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = `in`.caffeinelabs.cassettecat.data.settings.AppPreferences())

    var showSortSheet by remember { mutableStateOf(false) }
    var sortTarget by remember { mutableStateOf(LibraryViewMode.SONGS) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showNewPlaylistSheet by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = preferences.defaultLibraryTab.pageIndex) { LibraryViewMode.entries.size }
    val pagerScope = rememberCoroutineScope()
    val songListState = rememberLazyListState()
    val songGridState = rememberLazyGridState()
    val artistGridState = rememberLazyGridState()
    val albumGridState = rememberLazyGridState()
    val genreGridState = rememberLazyGridState()
    val artistListState = rememberLazyListState()
    val albumListState = rememberLazyListState()
    val genreListState = rememberLazyListState()
    val playlistListState = rememberLazyListState()
    val viewMode = LibraryViewMode.entries[pagerState.currentPage]
    val collectionLayout by viewModel.collectionLayout.collectAsState()
    val artistSortOrder by viewModel.artistSortOrder.collectAsState()
    val artistSortDirection by viewModel.artistSortDirection.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val albumSortDirection by viewModel.albumSortDirection.collectAsState()
    val genreSortOrder by viewModel.genreSortOrder.collectAsState()
    val genreSortDirection by viewModel.genreSortDirection.collectAsState()
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val songFilter by viewModel.songFilter.collectAsState()
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var editingSong by remember { mutableStateOf<Song?>(null) }
    var importSummary by remember { mutableStateOf<M3uImportSummary?>(null) }
    val selectionMode = selectedIds.isNotEmpty()
    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }
    val favoritesRepository = remember { FavoritesRepository(context) }
    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val downloads by downloadRepository.downloads.collectAsState()
    val favoriteSongs = remember(loadedState?.songs, favoriteIds) {
        loadedState?.songs.orEmpty().filter { it.isFavorite || it.id in favoriteIds }
    }
    val filteredSongs = remember(loadedState?.songs, songFilter, downloads, favoriteIds) {
        when (songFilter) {
            SongFilter.ALL -> loadedState?.songs.orEmpty()
            SongFilter.FAVORITES -> favoriteSongs
            SongFilter.DOWNLOADED -> loadedState?.songs.orEmpty().filter { song ->
                song.source == MusicSource.Local || downloads[song.id]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
            }
            SongFilter.RECENTLY_ADDED -> loadedState?.songs.orEmpty().sortedByDescending { it.dateAddedMs }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val librarySongs = loadedState?.songs.orEmpty()
            pagerScope.launch {
                val resolver = context.contentResolver
                val result = withContext(Dispatchers.IO) {
                    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }?.substringBeforeLast(".") ?: "Imported Playlist"
                    val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    text?.let { parseM3u(it) }?.let { entries -> Triple(name, librarySongs.matchM3uEntries(entries), entries.size) }
                }
                if (result != null) {
                    val (name, matchedIds, entryCount) = result
                    playlistViewModel.create(name) { playlist -> playlistViewModel.addSongs(playlist.id, matchedIds) }
                    importSummary = M3uImportSummary(name, matchedIds.size, entryCount)
                }
            }
        }
    }

    fun toggleSelected(songId: String) {
        selectedIds = if (songId in selectedIds) selectedIds - songId else selectedIds + songId
    }

    fun shareSelected(songs: List<Song>) {
        shareSongs(context, songs.filter { it.id in selectedIds })
        selectedIds = emptySet()
    }

    fun downloadSelected(songs: List<Song>) {
        songs.filter { it.id in selectedIds && it.source != MusicSource.Local }.forEach(downloadRepository::download)
        selectedIds = emptySet()
    }

    fun playGroup(songs: List<Song>) {
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        playbackViewModel.playQueue(songs, 0)
        if (wasIdle) onNavigateToNowPlaying()
    }

    fun playOrSelectSong(song: Song) {
        if (selectionMode) {
            toggleSelected(song.id)
        } else {
            val wasIdle = playbackViewModel.playbackState.value.currentSong == null
            val index = filteredSongs.indexOfFirst { it.id == song.id }
            playbackViewModel.playQueue(filteredSongs, index)
            if (wasIdle) onNavigateToNowPlaying()
        }
    }

    fun moveSortedListToStart(target: LibraryViewMode) {
        pagerScope.launch {
            delay(260)
            when (target) {
                LibraryViewMode.SONGS -> if (collectionLayout == CollectionLayout.GRID) songGridState.scrollToItem(0) else songListState.scrollToItem(0)
                LibraryViewMode.ARTISTS -> if (collectionLayout == CollectionLayout.GRID) artistGridState.scrollToItem(0) else artistListState.scrollToItem(0)
                LibraryViewMode.ALBUMS -> if (collectionLayout == CollectionLayout.GRID) albumGridState.scrollToItem(0) else albumListState.scrollToItem(0)
                LibraryViewMode.GENRES -> if (collectionLayout == CollectionLayout.GRID) genreGridState.scrollToItem(0) else genreListState.scrollToItem(0)
                LibraryViewMode.PLAYLISTS -> Unit
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_x,
                        contentDescription = "Cancel selection",
                        onClick = { selectedIds = emptySet() }
                    )
                    Text(
                        "${selectedIds.size} selected",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    val singleLocalSong = if (selectedIds.size == 1) {
                        loadedState?.songs?.find { it.id == selectedIds.first() && it.source == MusicSource.Local }
                    } else null

                    if (singleLocalSong != null) {
                        PressDepthIconButton(
                            iconRes = R.drawable.lucide_ic_sliders_horizontal,
                            contentDescription = "Edit metadata",
                            onClick = { editingSong = singleLocalSong }
                        )
                    }
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_list_plus,
                        contentDescription = "Add to playlist",
                        onClick = { showPlaylistPicker = true }
                    )
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_download,
                        contentDescription = "Download",
                        onClick = { downloadSelected(loadedState?.songs.orEmpty()) }
                    )
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_share_2,
                        contentDescription = "Share",
                        onClick = { shareSelected(loadedState?.songs.orEmpty()) }
                    )
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Library", style = MaterialTheme.typography.headlineSmall)
                        if (loadedState != null || viewMode == LibraryViewMode.PLAYLISTS) {
                            val count = when (viewMode) {
                                LibraryViewMode.SONGS -> filteredSongs.size
                                LibraryViewMode.ARTISTS -> filteredSongs.groupedByArtist().size
                                LibraryViewMode.ALBUMS -> filteredSongs.groupedByAlbum().size
                                LibraryViewMode.GENRES -> filteredSongs.groupedByGenre().size
                                LibraryViewMode.PLAYLISTS -> playlists.size + 1 + SmartPlaylistType.entries.size
                            }
                            val noun = when (viewMode) {
                                LibraryViewMode.SONGS -> "song"
                                LibraryViewMode.ARTISTS -> "artist"
                                LibraryViewMode.ALBUMS -> "album"
                                LibraryViewMode.GENRES -> "genre"
                                LibraryViewMode.PLAYLISTS -> "collection"
                            }
                            val baseCountText = if (count == 1) "1 $noun" else "$count ${noun}s"
                            val isOffline by viewModel.isOfflineMode.collectAsState()
                            Text(
                                if (isOffline) "$baseCountText . Offline" else baseCountText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                                color = if (isOffline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (viewMode == LibraryViewMode.PLAYLISTS) {
                        PressDepthIconButton(
                            iconRes = R.drawable.lucide_ic_import,
                            contentDescription = "Import playlist",
                            onClick = { importLauncher.launch(arrayOf("*/*")) }
                        )
                        PressDepthIconButton(
                            iconRes = R.drawable.lucide_ic_plus,
                            contentDescription = "New playlist",
                            onClick = { showNewPlaylistSheet = true }
                        )
                    }
                    PressDepthIconButton(
                        iconRes = if (collectionLayout == CollectionLayout.GRID) R.drawable.lucide_ic_layout_list else R.drawable.lucide_ic_layout_grid,
                        contentDescription = if (collectionLayout == CollectionLayout.GRID) "Use list layout" else "Use artwork layout",
                        onClick = {
                            viewModel.setCollectionLayout(if (collectionLayout == CollectionLayout.GRID) CollectionLayout.LIST else CollectionLayout.GRID)
                        }
                    )
                    if (viewMode != LibraryViewMode.PLAYLISTS && loadedState != null) {
                        PressDepthIconButton(
                            iconRes = R.drawable.lucide_ic_sliders_horizontal,
                            contentDescription = "Filter library",
                            tint = if (songFilter == SongFilter.ALL) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                            onClick = { showFilterSheet = true }
                        )
                        PressDepthIconButton(
                            iconRes = R.drawable.lucide_ic_arrow_up_down,
                            contentDescription = "Sort by",
                            onClick = {
                                sortTarget = viewMode
                                showSortSheet = true
                            }
                        )
                    }
                }
            }

            LibraryViewModeTabs(
                selected = viewMode,
                onSelect = { mode -> pagerScope.launch { pagerState.animateScrollToPage(mode.ordinal) } }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !selectionMode
            ) { page ->
                when (LibraryViewMode.entries[page]) {
                    LibraryViewMode.PLAYLISTS -> if (collectionLayout == CollectionLayout.GRID) {
                        PlaylistGrid(
                            playlists = playlists,
                            allSongs = loadedState?.songs.orEmpty(),
                            likedSongs = favoriteSongs,
                            modifier = Modifier.fillMaxSize(),
                            listBottomPadding = listBottomPadding,
                            onClick = onNavigateToPlaylist,
                            onPlay = ::playGroup,
                            onOpenLikedSongs = onNavigateToLikedSongs,
                            onOpenSmartPlaylist = onNavigateToSmartPlaylist
                        )
                    } else {
                        PlaylistList(
                            playlists = playlists,
                            allSongs = loadedState?.songs.orEmpty(),
                            likedSongs = favoriteSongs,
                            state = playlistListState,
                            modifier = Modifier.fillMaxSize(),
                            listBottomPadding = listBottomPadding,
                            onClick = onNavigateToPlaylist,
                            onPlay = ::playGroup,
                            onOpenLikedSongs = onNavigateToLikedSongs,
                            onOpenSmartPlaylist = onNavigateToSmartPlaylist
                        )
                    }

                    else -> when (val state = uiState) {
                        is LibraryUiState.Loading -> {
                            if (LibraryViewMode.entries[page] == LibraryViewMode.SONGS && collectionLayout == CollectionLayout.LIST) {
                                Column(modifier = Modifier.fillMaxSize()) { repeat(8) { SongRowSkeleton() } }
                            } else if (collectionLayout == CollectionLayout.GRID) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding + 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    items(8) { GridCardSkeleton() }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = listBottomPadding)
                                ) {
                                    items(8) { SongRowSkeleton() }
                                }
                            }
                        }

                        is LibraryUiState.Empty -> {
                            EmptyState(
                                iconRes = R.drawable.lucide_ic_music,
                                title = "No music found",
                                message = "Pull down to rescan, or check your library folder settings."
                            )
                        }

                        is LibraryUiState.Loaded -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (state.sourceWarnings.isNotEmpty()) {
                                    SourceWarningBanner(warnings = state.sourceWarnings)
                                }
                                when (LibraryViewMode.entries[page]) {
                                    LibraryViewMode.SONGS -> SongsTabContent(
                                        filteredSongs = filteredSongs,
                                        songFilter = songFilter,
                                        collectionLayout = collectionLayout,
                                        selectedIds = selectedIds,
                                        selectionMode = selectionMode,
                                        songGridState = songGridState,
                                        songListState = songListState,
                                        listBottomPadding = listBottomPadding,
                                        onPlayOrSelect = ::playOrSelectSong,
                                        onToggleSelect = ::toggleSelected,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LibraryViewMode.ARTISTS -> ArtistsTabContent(
                                        filteredSongs = filteredSongs,
                                        sortOrder = artistSortOrder,
                                        sortDirection = artistSortDirection,
                                        collectionLayout = collectionLayout,
                                        gridState = artistGridState,
                                        listState = artistListState,
                                        listBottomPadding = listBottomPadding,
                                        onNavigateToArtist = onNavigateToArtist,
                                        onPlayGroup = ::playGroup,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LibraryViewMode.ALBUMS -> AlbumsTabContent(
                                        filteredSongs = filteredSongs,
                                        sortOrder = albumSortOrder,
                                        sortDirection = albumSortDirection,
                                        collectionLayout = collectionLayout,
                                        gridState = albumGridState,
                                        listState = albumListState,
                                        listBottomPadding = listBottomPadding,
                                        onNavigateToAlbum = onNavigateToAlbum,
                                        onPlayGroup = ::playGroup,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LibraryViewMode.GENRES -> GenresTabContent(
                                        filteredSongs = filteredSongs,
                                        sortOrder = genreSortOrder,
                                        sortDirection = genreSortDirection,
                                        collectionLayout = collectionLayout,
                                        gridState = genreGridState,
                                        listState = genreListState,
                                        listBottomPadding = listBottomPadding,
                                        onNavigateToGenre = onNavigateToGenre,
                                        onPlayGroup = ::playGroup,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LibraryViewMode.PLAYLISTS -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        when (sortTarget) {
            LibraryViewMode.SONGS -> SortOptionsSheet(
                options = SongSortOrder.entries,
                labelOf = { it.label },
                selected = sortOrder,
                direction = sortDirection,
                onSelect = { order ->
                    viewModel.setSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.SONGS)
                },
                onDismiss = { showSortSheet = false }
            )

            LibraryViewMode.ARTISTS -> SortOptionsSheet(
                options = ArtistSortOrder.entries,
                labelOf = { it.label },
                selected = artistSortOrder,
                direction = artistSortDirection,
                onSelect = { order ->
                    viewModel.setArtistSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.ARTISTS)
                },
                onDismiss = { showSortSheet = false }
            )

            LibraryViewMode.ALBUMS -> SortOptionsSheet(
                options = AlbumSortOrder.entries,
                labelOf = { it.label },
                selected = albumSortOrder,
                direction = albumSortDirection,
                onSelect = { order ->
                    viewModel.setAlbumSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.ALBUMS)
                },
                onDismiss = { showSortSheet = false }
            )

            LibraryViewMode.GENRES -> SortOptionsSheet(
                options = GenreSortOrder.entries,
                labelOf = { it.label },
                selected = genreSortOrder,
                direction = genreSortDirection,
                onSelect = { order ->
                    viewModel.setGenreSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.GENRES)
                },
                onDismiss = { showSortSheet = false }
            )

            LibraryViewMode.PLAYLISTS -> Unit
        }
    }

    if (showFilterSheet) {
        SongFilterSheet(
            selected = songFilter,
            onSelect = {
                viewModel.setSongFilter(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showNewPlaylistSheet) {
        PlaylistNameSheet(
            title = "New Playlist",
            initialName = "",
            onConfirm = { name ->
                showNewPlaylistSheet = false
                playlistViewModel.create(name) { playlist -> onNavigateToPlaylist(playlist.id) }
            },
            onDismiss = { showNewPlaylistSheet = false }
        )
    }

    if (showPlaylistPicker) {
        PlaylistPickerSheet(
            playlists = playlists,
            onSelect = { playlist ->
                selectedIds.forEach { songId -> playlistViewModel.addSong(playlist.id, songId) }
                selectedIds = emptySet()
                showPlaylistPicker = false
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    val summary = importSummary
    if (summary != null) {
        AlertDialog(
            onDismissRequest = { importSummary = null },
            title = { Text("Playlist imported") },
            text = { Text("Imported \"${summary.name}\": ${summary.matched} of ${summary.total} songs matched.") },
            confirmButton = {
                TextButton(onClick = { importSummary = null }) { Text("OK") }
            }
        )
    }

    editingSong?.let { songToEdit ->
        TagEditorSheet(
            song = songToEdit,
            onDismiss = { editingSong = null },
            onSaved = {
                viewModel.refresh()
                selectedIds = emptySet()
            }
        )
    }
}
