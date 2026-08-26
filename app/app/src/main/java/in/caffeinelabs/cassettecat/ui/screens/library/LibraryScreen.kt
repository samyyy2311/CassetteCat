@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.ui.screens.library

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onNavigateToFolder: (String) -> Unit = {},
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit = {},
    viewModel: LibraryViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sortDirection by viewModel.sortDirection.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val loadedState = uiState as? LibraryUiState.Loaded
    val isRefreshing = uiState is LibraryUiState.Loading
    val context = LocalContext.current
    val appPreferencesRepository = remember { `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = `in`.caffeinelabs.cassettecat.data.settings.AppPreferences())

    var showRefineSheet by remember { mutableStateOf(false) }
    var showNewPlaylistSheet by remember { mutableStateOf(false) }
    val visibleModes = remember(preferences.libraryTabOrder, preferences.hiddenLibraryTabs) {
        preferences.libraryTabOrder
            .filterNot { it in preferences.hiddenLibraryTabs }
            .map { LibraryViewMode.valueOf(it.name) }
    }
    val defaultPage = remember(visibleModes, preferences.defaultLibraryTab) {
        visibleModes.indexOfFirst { it.name == preferences.defaultLibraryTab.name }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = defaultPage) { visibleModes.size }
    var hasSyncedDefaultPage by remember { mutableStateOf(false) }
    LaunchedEffect(defaultPage) {
        if (!hasSyncedDefaultPage) {
            pagerState.scrollToPage(defaultPage)
            hasSyncedDefaultPage = true
        }
    }
    val pagerScope = rememberCoroutineScope()
    val songListState = rememberLazyListState()
    val songGridState = rememberLazyGridState()
    val artistGridState = rememberLazyGridState()
    val albumGridState = rememberLazyGridState()
    val genreGridState = rememberLazyGridState()
    val folderGridState = rememberLazyGridState()
    val artistListState = rememberLazyListState()
    val albumListState = rememberLazyListState()
    val genreListState = rememberLazyListState()
    val folderListState = rememberLazyListState()
    val playlistListState = rememberLazyListState()
    val viewMode = visibleModes.getOrElse(pagerState.currentPage) { visibleModes.first() }
    val collectionLayout by viewModel.collectionLayout.collectAsStateWithLifecycle()
    val artistSortOrder by viewModel.artistSortOrder.collectAsStateWithLifecycle()
    val artistSortDirection by viewModel.artistSortDirection.collectAsStateWithLifecycle()
    val albumSortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
    val albumSortDirection by viewModel.albumSortDirection.collectAsStateWithLifecycle()
    val genreSortOrder by viewModel.genreSortOrder.collectAsStateWithLifecycle()
    val genreSortDirection by viewModel.genreSortDirection.collectAsStateWithLifecycle()
    val folderSortOrder by viewModel.folderSortOrder.collectAsStateWithLifecycle()
    val folderSortDirection by viewModel.folderSortDirection.collectAsStateWithLifecycle()
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val songFilter by viewModel.songFilter.collectAsStateWithLifecycle()
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var songForMenu by remember { mutableStateOf<Song?>(null) }
    var importSummary by remember { mutableStateOf<M3uImportSummary?>(null) }
    val selectionMode = selectedIds.isNotEmpty()
    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }
    val favoritesRepository = remember { FavoritesRepository(context) }
    val favoriteIds by favoritesRepository.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val downloads by downloadRepository.downloads.collectAsStateWithLifecycle()
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

    fun selectedSongs(): List<Song> = when (viewMode) {
        LibraryViewMode.SONGS -> filteredSongs.filter { it.id in selectedIds }
        LibraryViewMode.ARTISTS -> filteredSongs.groupedByArtist().filter { it.artist in selectedIds }.flatMap { it.songs }
        LibraryViewMode.ALBUMS -> filteredSongs.groupedByAlbum().filter { it.albumId in selectedIds }.flatMap { it.songs }
        LibraryViewMode.GENRES -> filteredSongs.groupedByGenre().filter { it.genre in selectedIds }.flatMap { it.songs }
        LibraryViewMode.FOLDERS -> filteredSongs.groupedByFolder().filter { it.folderPath in selectedIds }.flatMap { it.songs }
        LibraryViewMode.PLAYLISTS -> {
            val allSongs = loadedState?.songs.orEmpty()
            playlists.filter { it.id in selectedIds }.flatMap { it.songIds }.distinct()
                .mapNotNull { id -> allSongs.find { it.id == id } }
        }
    }.distinctBy { it.id }

    fun allIdsForCurrentTab(): Set<String> = when (viewMode) {
        LibraryViewMode.SONGS -> filteredSongs.map { it.id }.toSet()
        LibraryViewMode.ARTISTS -> filteredSongs.groupedByArtist().map { it.artist }.toSet()
        LibraryViewMode.ALBUMS -> filteredSongs.groupedByAlbum().map { it.albumId }.toSet()
        LibraryViewMode.GENRES -> filteredSongs.groupedByGenre().map { it.genre }.toSet()
        LibraryViewMode.PLAYLISTS -> playlists.map { it.id }.toSet()
        LibraryViewMode.FOLDERS -> filteredSongs.groupedByFolder().map { it.folderPath }.toSet()
    }

    fun shareSelected(songs: List<Song>) {
        shareSongs(context, songs)
        selectedIds = emptySet()
    }

    fun downloadSelected(songs: List<Song>) {
        songs.filter { it.source != MusicSource.Local }.forEach(downloadRepository::download)
        selectedIds = emptySet()
    }

    fun playGroup(songs: List<Song>) {
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        playbackViewModel.playQueue(songs, 0)
        if (wasIdle) onNavigateToNowPlaying()
    }

    fun playSong(song: Song) {
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        val index = filteredSongs.indexOfFirst { it.id == song.id }
        playbackViewModel.playQueue(filteredSongs, index)
        if (wasIdle) onNavigateToNowPlaying()
    }

    fun moveSortedListToStart(target: LibraryViewMode) {
        pagerScope.launch {
            delay(260)
            when (target) {
                LibraryViewMode.SONGS -> if (collectionLayout == CollectionLayout.GRID) songGridState.scrollToItem(0) else songListState.scrollToItem(0)
                LibraryViewMode.ARTISTS -> if (collectionLayout == CollectionLayout.GRID) artistGridState.scrollToItem(0) else artistListState.scrollToItem(0)
                LibraryViewMode.ALBUMS -> if (collectionLayout == CollectionLayout.GRID) albumGridState.scrollToItem(0) else albumListState.scrollToItem(0)
                LibraryViewMode.GENRES -> if (collectionLayout == CollectionLayout.GRID) genreGridState.scrollToItem(0) else genreListState.scrollToItem(0)
                LibraryViewMode.FOLDERS -> if (collectionLayout == CollectionLayout.GRID) folderGridState.scrollToItem(0) else folderListState.scrollToItem(0)
                LibraryViewMode.PLAYLISTS -> Unit
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_x,
                        contentDescription = "Cancel selection",
                        onClick = { selectedIds = emptySet() }
                    )
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_check_check,
                        contentDescription = "Select all",
                        onClick = { selectedIds = allIdsForCurrentTab() }
                    )
                    Text(
                        "${selectedIds.size} selected",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_list_plus,
                        contentDescription = "Add to playlist",
                        onClick = { showPlaylistPicker = true }
                    )
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_download,
                        contentDescription = "Download",
                        onClick = { downloadSelected(selectedSongs()) }
                    )
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_share_2,
                        contentDescription = "Share",
                        onClick = { shareSelected(selectedSongs()) }
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
                                LibraryViewMode.FOLDERS -> filteredSongs.groupedByFolder().size
                                LibraryViewMode.PLAYLISTS -> playlists.size + 1 + SmartPlaylistType.entries.size
                            }
                            val noun = when (viewMode) {
                                LibraryViewMode.SONGS -> "song"
                                LibraryViewMode.ARTISTS -> "artist"
                                LibraryViewMode.ALBUMS -> "album"
                                LibraryViewMode.GENRES -> "genre"
                                LibraryViewMode.FOLDERS -> "folder"
                                LibraryViewMode.PLAYLISTS -> "collection"
                            }
                            val baseCountText = if (count == 1) "1 $noun" else "$count ${noun}s"
                            val isOffline by viewModel.isOfflineMode.collectAsStateWithLifecycle()
                            Text(
                                if (isOffline) "$baseCountText · Offline" else baseCountText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOffline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                                contentDescription = "Refine library",
                                tint = if (songFilter == SongFilter.ALL) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                                onClick = { showRefineSheet = true }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            LibraryViewModeTabs(
                modes = visibleModes,
                selected = viewMode,
                onSelect = { mode ->
                    if (!selectionMode) pagerScope.launch { pagerState.animateScrollToPage(visibleModes.indexOf(mode)) }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !selectionMode
            ) { page ->
                val pageMode = visibleModes.getOrElse(page) { visibleModes.first() }
                when (pageMode) {
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
                            onOpenSmartPlaylist = onNavigateToSmartPlaylist,
                            selectedIds = selectedIds,
                            selectionMode = selectionMode,
                            onToggleSelect = ::toggleSelected
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
                            onOpenSmartPlaylist = onNavigateToSmartPlaylist,
                            selectedIds = selectedIds,
                            selectionMode = selectionMode,
                            onToggleSelect = ::toggleSelected
                        )
                    }

                    else -> when (val state = uiState) {
                        is LibraryUiState.Loading -> {
                            if (pageMode == LibraryViewMode.SONGS && collectionLayout == CollectionLayout.LIST) {
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
                                when (pageMode) {
                                    LibraryViewMode.SONGS -> SongsTabContent(
                                        filteredSongs = filteredSongs,
                                        songFilter = songFilter,
                                        collectionLayout = collectionLayout,
                                        selectedIds = selectedIds,
                                        selectionMode = selectionMode,
                                        songGridState = songGridState,
                                        songListState = songListState,
                                        listBottomPadding = listBottomPadding,
                                        onPlaySong = ::playSong,
                                        onToggleSelect = ::toggleSelected,
                                        onSongMore = { songForMenu = it },
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
                                        selectedIds = selectedIds,
                                        selectionMode = selectionMode,
                                        onToggleSelect = ::toggleSelected,
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
                                        selectedIds = selectedIds,
                                        selectionMode = selectionMode,
                                        onToggleSelect = ::toggleSelected,
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
                                        selectedIds = selectedIds,
                                        selectionMode = selectionMode,
                                        onToggleSelect = ::toggleSelected,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LibraryViewMode.FOLDERS -> {
                                        val folders = filteredSongs.groupedByFolder().let { list ->
                                            val sorted = list.sortedWith(folderSortOrder.comparator())
                                            if (folderSortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
                                        }
                                        FoldersTab(
                                            folders = folders,
                                            collectionLayout = collectionLayout,
                                            gridState = folderGridState,
                                            listState = folderListState,
                                            listBottomPadding = listBottomPadding,
                                            onNavigateToFolder = onNavigateToFolder,
                                            onPlayGroup = ::playGroup,
                                            selectedIds = selectedIds,
                                            selectionMode = selectionMode,
                                            onToggleSelect = ::toggleSelected,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    LibraryViewMode.PLAYLISTS -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRefineSheet) {
        when (viewMode) {
            LibraryViewMode.SONGS -> LibraryRefineSheet(
                filter = songFilter,
                onFilterSelect = viewModel::setSongFilter,
                sortOptions = SongSortOrder.entries,
                sortLabelOf = { it.label },
                selectedSort = sortOrder,
                sortDirection = sortDirection,
                onSortSelect = { order ->
                    viewModel.setSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.SONGS)
                },
                onDismiss = { showRefineSheet = false }
            )

            LibraryViewMode.ARTISTS -> LibraryRefineSheet(
                filter = songFilter,
                onFilterSelect = viewModel::setSongFilter,
                sortOptions = ArtistSortOrder.entries,
                sortLabelOf = { it.label },
                selectedSort = artistSortOrder,
                sortDirection = artistSortDirection,
                onSortSelect = { order ->
                    viewModel.setArtistSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.ARTISTS)
                },
                onDismiss = { showRefineSheet = false }
            )

            LibraryViewMode.ALBUMS -> LibraryRefineSheet(
                filter = songFilter,
                onFilterSelect = viewModel::setSongFilter,
                sortOptions = AlbumSortOrder.entries,
                sortLabelOf = { it.label },
                selectedSort = albumSortOrder,
                sortDirection = albumSortDirection,
                onSortSelect = { order ->
                    viewModel.setAlbumSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.ALBUMS)
                },
                onDismiss = { showRefineSheet = false }
            )

            LibraryViewMode.GENRES -> LibraryRefineSheet(
                filter = songFilter,
                onFilterSelect = viewModel::setSongFilter,
                sortOptions = GenreSortOrder.entries,
                sortLabelOf = { it.label },
                selectedSort = genreSortOrder,
                sortDirection = genreSortDirection,
                onSortSelect = { order ->
                    viewModel.setGenreSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.GENRES)
                },
                onDismiss = { showRefineSheet = false }
            )

            LibraryViewMode.FOLDERS -> LibraryRefineSheet(
                filter = songFilter,
                onFilterSelect = viewModel::setSongFilter,
                sortOptions = FolderSortOrder.entries,
                sortLabelOf = { it.label },
                selectedSort = folderSortOrder,
                sortDirection = folderSortDirection,
                onSortSelect = { order ->
                    viewModel.setFolderSortOrder(order)
                    moveSortedListToStart(LibraryViewMode.FOLDERS)
                },
                onDismiss = { showRefineSheet = false }
            )

            LibraryViewMode.PLAYLISTS -> Unit
        }
    }

    if (showNewPlaylistSheet) {
        PlaylistNameSheet(
            title = "New Playlist",
            initialName = "",
            onConfirm = { name ->
                showNewPlaylistSheet = false
                playlistViewModel.create(name) { playlist -> onNavigateToPlaylist(playlist.id) }
            },
            onConfirmSmart = { name, criteria ->
                showNewPlaylistSheet = false
                playlistViewModel.createSmartPlaylist(name, criteria) { playlist -> onNavigateToPlaylist(playlist.id) }
            },
            onDismiss = { showNewPlaylistSheet = false }
        )
    }

    if (showPlaylistPicker) {
        PlaylistPickerSheet(
            playlists = playlists,
            onSelect = { playlist ->
                val idsToAdd = if (songForMenu != null) listOf(songForMenu!!.id) else selectedSongs().map { it.id }
                playlistViewModel.addSongs(playlist.id, idsToAdd)
                selectedIds = emptySet()
                showPlaylistPicker = false
                songForMenu = null
            },
            onDismiss = {
                showPlaylistPicker = false
                songForMenu = null
            }
        )
    }

    if (!showPlaylistPicker) songForMenu?.let { song ->
        SongOptionsSheet(
            song = song,
            isFavorite = song.isFavorite || song.id in favoriteIds,
            onPlayNext = {
                playbackViewModel.addToUpNext(listOf(song))
                songForMenu = null
            },
            onAddToQueue = {
                playbackViewModel.addToEndOfQueue(listOf(song))
                songForMenu = null
            },
            onAddToPlaylist = {
                showPlaylistPicker = true
            },
            onToggleFavorite = {
                val isFav = song.isFavorite || song.id in favoriteIds
                pagerScope.launch {
                    favoritesRepository.setFavorite(song.id, !isFav)
                }
                songForMenu = null
            },
            onShare = {
                shareSongs(context, listOf(song))
                songForMenu = null
            },
            onDismiss = { songForMenu = null }
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
}
