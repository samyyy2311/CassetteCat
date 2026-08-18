package `in`.caffeinelabs.cassettecat.ui.screens.library

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.matchM3uEntries
import `in`.caffeinelabs.cassettecat.data.library.parseM3u
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.DownloadStatusIcon
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PlaylistCoverArt
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.shareSongs
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import `in`.caffeinelabs.cassettecat.ui.util.tapScaleSelectable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LibraryViewMode(val label: String) {
    SONGS("Songs"), ARTISTS("Artists"), ALBUMS("Albums"), GENRES("Genres"), PLAYLISTS("Playlists")
}

// One layout preference keeps every library tab consistent: compact rows or artwork tiles.
private enum class CollectionLayout { GRID, LIST }

private enum class SongFilter(val label: String) {
    ALL("All songs"), FAVORITES("Favorites"), DOWNLOADED("Downloaded"), RECENTLY_ADDED("Recently added")
}

private enum class ArtistSortOrder(val label: String) { NAME("Name"), SONG_COUNT("Song Count") }
private enum class AlbumSortOrder(val label: String) { ALBUM("Album"), ARTIST("Artist"), SONG_COUNT("Song Count") }
private enum class GenreSortOrder(val label: String) { NAME("Name"), SONG_COUNT("Song Count") }

private data class M3uImportSummary(val name: String, val matched: Int, val total: Int)

private fun ArtistSortOrder.comparator(): Comparator<ArtistGroup> = when (this) {
    ArtistSortOrder.NAME -> compareBy { it.artist.lowercase() }
    ArtistSortOrder.SONG_COUNT -> compareBy { it.songs.size }
}

private fun AlbumSortOrder.comparator(): Comparator<AlbumGroup> = when (this) {
    AlbumSortOrder.ALBUM -> compareBy { it.album.lowercase() }
    AlbumSortOrder.ARTIST -> compareBy { it.artist.lowercase() }
    AlbumSortOrder.SONG_COUNT -> compareBy { it.songs.size }
}

private fun GenreSortOrder.comparator(): Comparator<GenreGroup> = when (this) {
    GenreSortOrder.NAME -> compareBy { it.genre.lowercase() }
    GenreSortOrder.SONG_COUNT -> compareBy { it.songs.size }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    // clears MainShell's drag sheet peek height
    listBottomPadding: Dp = 0.dp,
    viewModel: LibraryViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val loadedState = uiState as? LibraryUiState.Loaded
    val isRefreshing = uiState is LibraryUiState.Loading
    var showSortSheet by remember { mutableStateOf(false) }
    // Freeze the tab being sorted when the sheet opens. Pager state can be mid-settle while
    // a sheet is shown; deriving this from currentPage at selection time caused the sort action
    // to be applied against a different tab and made the page appear to jump.
    var sortTarget by remember { mutableStateOf(LibraryViewMode.SONGS) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showNewPlaylistSheet by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState { LibraryViewMode.entries.size }
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
    var collectionLayout by rememberSaveable { mutableStateOf(CollectionLayout.GRID) }
    var artistSortOrder by rememberSaveable { mutableStateOf(ArtistSortOrder.NAME) }
    var artistSortDirection by rememberSaveable { mutableStateOf(SortDirection.ASCENDING) }
    var albumSortOrder by rememberSaveable { mutableStateOf(AlbumSortOrder.ALBUM) }
    var albumSortDirection by rememberSaveable { mutableStateOf(SortDirection.ASCENDING) }
    var genreSortOrder by rememberSaveable { mutableStateOf(GenreSortOrder.NAME) }
    var genreSortDirection by rememberSaveable { mutableStateOf(SortDirection.ASCENDING) }
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var songFilter by rememberSaveable { mutableStateOf(SongFilter.ALL) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var importSummary by remember { mutableStateOf<M3uImportSummary?>(null) }
    val selectionMode = selectedIds.isNotEmpty()
    val context = LocalContext.current
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
            val resolver = context.contentResolver
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }?.substringBeforeLast(".") ?: "Imported Playlist"
            val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text != null) {
                val entries = parseM3u(text)
                val librarySongs = loadedState?.songs.orEmpty()
                val matchedIds = librarySongs.matchM3uEntries(entries)
                playlistViewModel.create(name) { playlist -> playlistViewModel.addSongs(playlist.id, matchedIds) }
                importSummary = M3uImportSummary(name, matchedIds.size, entries.size)
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
            // Let the selection sheet finish closing first. Scrolling while it owns the focus
            // was being lost by the nested pager, leaving the user partway down the new order.
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
                // Keep the title comfortably below the status area while preserving the
                // compact hand-off into the library tabs.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
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
                                // Liked Songs is a permanent library collection alongside user playlists.
                                LibraryViewMode.PLAYLISTS -> playlists.size + 1
                            }
                            val noun = when (viewMode) {
                                LibraryViewMode.SONGS -> "song"
                                LibraryViewMode.ARTISTS -> "artist"
                                LibraryViewMode.ALBUMS -> "album"
                                LibraryViewMode.GENRES -> "genre"
                                LibraryViewMode.PLAYLISTS -> "collection"
                            }
                            Text(
                                if (count == 1) "1 $noun" else "$count ${noun}s",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        iconRes = if (collectionLayout == CollectionLayout.GRID) {
                            R.drawable.lucide_ic_layout_list
                        } else {
                            R.drawable.lucide_ic_layout_grid
                        },
                        contentDescription = if (collectionLayout == CollectionLayout.GRID) {
                            "Use list layout"
                        } else {
                            "Use artwork layout"
                        },
                        onClick = {
                            collectionLayout = if (collectionLayout == CollectionLayout.GRID) {
                                CollectionLayout.LIST
                            } else {
                                CollectionLayout.GRID
                            }
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
                            onOpenLikedSongs = onNavigateToLikedSongs
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
                            onOpenLikedSongs = onNavigateToLikedSongs
                        )
                    }

                    else -> when (val state = uiState) {
                        is LibraryUiState.Loading -> {
                            if (LibraryViewMode.entries[page] == LibraryViewMode.SONGS && collectionLayout == CollectionLayout.LIST) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    repeat(8) { SongRowSkeleton() }
                                }
                            } else if (collectionLayout == CollectionLayout.GRID) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 24.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding + 16.dp
                                    ),
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
                                    LibraryViewMode.SONGS -> if (filteredSongs.isEmpty()) {
                                        EmptyState(
                                            iconRes = R.drawable.lucide_ic_music,
                                            title = "No ${songFilter.label.lowercase()} found",
                                            message = "Try a different filter.",
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else if (collectionLayout == CollectionLayout.GRID) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            modifier = Modifier.weight(1f),
                                            state = songGridState,
                                            contentPadding = PaddingValues(
                                                start = 24.dp, end = 24.dp, top = 8.dp, bottom = listBottomPadding + 16.dp
                                            ),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(20.dp)
                                        ) {
                                            items(filteredSongs, key = { it.id }) { song ->
                                                SongGridCard(
                                                    song = song,
                                                    selected = song.id in selectedIds,
                                                    onClick = { playOrSelectSong(song) },
                                                    onLongClick = { toggleSelected(song.id) }
                                                )
                                            }
                                        }
                                    } else LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        state = songListState,
                                        contentPadding = PaddingValues(top = 8.dp, bottom = listBottomPadding)
                                    ) {
                                        items(filteredSongs, key = { it.id }) { song ->
                                            SelectableSongRow(
                                                song = song,
                                                selected = song.id in selectedIds,
                                                selectionMode = selectionMode,
                                                onClick = { playOrSelectSong(song) },
                                                onLongClick = { toggleSelected(song.id) }
                                            )
                                        }
                                    }

                                    LibraryViewMode.ARTISTS -> {
                                        val artists = remember(filteredSongs, artistSortOrder, artistSortDirection) {
                                            val comparator = artistSortOrder.comparator()
                                                .let { if (artistSortDirection == SortDirection.DESCENDING) it.reversed() else it }
                                            filteredSongs.groupedByArtist().sortedWith(comparator)
                                        }
                                        if (collectionLayout == CollectionLayout.GRID) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(2),
                                                modifier = Modifier.weight(1f),
                                                state = artistGridState,
                                                contentPadding = PaddingValues(
                                                    start = 24.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding + 16.dp
                                                ),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                items(artists, key = { it.artist }) { group ->
                                                    ArtistCard(
                                                        group = group,
                                                        onClick = { onNavigateToArtist(group.artist) },
                                                        onPlay = { playGroup(group.songs) }
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.weight(1f),
                                                state = artistListState,
                                                contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
                                            ) {
                                                items(artists, key = { it.artist }) { group ->
                                                    ArtistListRow(
                                                        group = group,
                                                        onClick = { onNavigateToArtist(group.artist) },
                                                        onPlay = { playGroup(group.songs) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    LibraryViewMode.ALBUMS -> {
                                        val albums = remember(filteredSongs, albumSortOrder, albumSortDirection) {
                                            val comparator = albumSortOrder.comparator()
                                                .let { if (albumSortDirection == SortDirection.DESCENDING) it.reversed() else it }
                                            filteredSongs.groupedByAlbum().sortedWith(comparator)
                                        }
                                        if (collectionLayout == CollectionLayout.GRID) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(2),
                                                modifier = Modifier.weight(1f),
                                                state = albumGridState,
                                                contentPadding = PaddingValues(
                                                    start = 24.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding + 16.dp
                                                ),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                items(albums, key = { it.albumId }) { group ->
                                                    AlbumCard(
                                                        group = group,
                                                        onClick = { onNavigateToAlbum(group.albumId) },
                                                        onPlay = { playGroup(group.songs) }
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.weight(1f),
                                                state = albumListState,
                                                contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
                                            ) {
                                                items(albums, key = { it.albumId }) { group ->
                                                    AlbumListRow(
                                                        group = group,
                                                        onClick = { onNavigateToAlbum(group.albumId) },
                                                        onPlay = { playGroup(group.songs) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    LibraryViewMode.GENRES -> {
                                        val genres = remember(filteredSongs, genreSortOrder, genreSortDirection) {
                                            val comparator = genreSortOrder.comparator()
                                                .let { if (genreSortDirection == SortDirection.DESCENDING) it.reversed() else it }
                                            filteredSongs.groupedByGenre().sortedWith(comparator)
                                        }
                                        if (collectionLayout == CollectionLayout.GRID) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(2),
                                                modifier = Modifier.weight(1f),
                                                state = genreGridState,
                                                contentPadding = PaddingValues(
                                                    start = 24.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding + 16.dp
                                                ),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                items(genres, key = { it.genre }) { group ->
                                                    GenreCard(
                                                        group = group,
                                                        onClick = { onNavigateToGenre(group.genre) },
                                                        onPlay = { playGroup(group.songs) }
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.weight(1f),
                                                state = genreListState,
                                                contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
                                            ) {
                                                items(genres, key = { it.genre }) { group ->
                                                    GenreListRow(
                                                        group = group,
                                                        onClick = { onNavigateToGenre(group.genre) },
                                                        onPlay = { playGroup(group.songs) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // unreachable: PLAYLISTS is handled by the outer when above
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
                    if (order == artistSortOrder) {
                        artistSortDirection = artistSortDirection.flipped()
                    } else {
                        artistSortOrder = order
                        artistSortDirection = SortDirection.ASCENDING
                    }
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
                    if (order == albumSortOrder) {
                        albumSortDirection = albumSortDirection.flipped()
                    } else {
                        albumSortOrder = order
                        albumSortDirection = SortDirection.ASCENDING
                    }
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
                    if (order == genreSortOrder) {
                        genreSortDirection = genreSortDirection.flipped()
                    } else {
                        genreSortOrder = order
                        genreSortDirection = SortDirection.ASCENDING
                    }
                    moveSortedListToStart(LibraryViewMode.GENRES)
                },
                onDismiss = { showSortSheet = false }
            )

            // unreachable: the header shows "new playlist" instead of "sort" in this mode
            LibraryViewMode.PLAYLISTS -> Unit
        }
    }

    if (showFilterSheet) {
        SongFilterSheet(
            selected = songFilter,
            onSelect = {
                songFilter = it
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
}

// plain text tabs, not TabRow: avoids its filled-indicator/ripple styling
@Composable
private fun LibraryViewModeTabs(selected: LibraryViewMode, onSelect: (LibraryViewMode) -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongFilterSheet(selected: SongFilter, onSelect: (SongFilter) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Filter songs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            SongFilter.entries.forEach { filter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tapScale { onSelect(filter) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(filter.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (filter == selected) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_check),
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SortOptionsSheet(
    options: List<T>,
    labelOf: (T) -> String,
    selected: T,
    direction: SortDirection,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Sort By",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            options.forEach { option ->
                SortOptionRow(
                    label = labelOf(option),
                    selected = option == selected,
                    direction = direction,
                    onClick = {
                        onSelect(option)
                        // A selection always completes the interaction. Re-selecting the active
                        // field flips ascending/descending, so leaving the sheet open made it
                        // feel as if the whole Library page had shifted instead of updating.
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SortOptionRow(label: String, selected: Boolean, direction: SortDirection, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) tint else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_check),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(16.dp))
            Icon(
                painter = painterResource(
                    if (direction == SortDirection.ASCENDING) {
                        R.drawable.lucide_ic_chevron_up
                    } else {
                        R.drawable.lucide_ic_chevron_down
                    }
                ),
                contentDescription = if (direction == SortDirection.ASCENDING) "Ascending" else "Descending",
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// dismissal is local; reappears on next refresh() if still failing
@Composable
private fun SourceWarningBanner(warnings: List<String>) {
    var dismissed by remember(warnings) { mutableStateOf(false) }
    if (dismissed) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_triangle_alert),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            warnings.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_x),
            contentDescription = "Dismiss",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = hapticClick { dismissed = true })
        )
    }
}

// hand-rolled to match each layout's real geometry, no shimmer dependency.
// Not private: rememberSkeletonColor()/SongRowSkeleton() are reused by Home/Stats/Search
// for their own loading states.
@Composable
fun rememberSkeletonColor(): Color {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
}

@Composable
fun SongRowSkeleton() {
    val color = rememberSkeletonColor()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(color))
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Box(Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

// matches ArtistCard/AlbumCard/GenreCard's square-tile-plus-two-lines shape
@Composable
private fun GridCardSkeleton() {
    val color = rememberSkeletonColor()
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(color))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(70.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
private fun RowScope.SongListRowContent(song: Song) {
    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))) {
        AlbumArt(song = song, modifier = Modifier.fillMaxSize())
    }
    Spacer(Modifier.width(16.dp))
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    DownloadStatusIcon(song = song, modifier = Modifier.padding(start = 8.dp))
}

// Shared compact track treatment for Library collections and Search.
@Composable
fun LibrarySongRow(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(start = 24.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongListRowContent(song)
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 64.dp)
        )
    }
}

@Composable
fun SongRow(song: Song, onClick: () -> Unit) = LibrarySongRow(song, onClick)

@Composable
private fun SelectableSongRow(
    song: Song,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapScaleSelectable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 24.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Icon(
                    painter = painterResource(
                        if (selected) R.drawable.lucide_ic_square_check_big else R.drawable.lucide_ic_square
                    ),
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(16.dp))
            }
            SongListRowContent(song)
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = if (selectionMode) 96.dp else 64.dp)
        )
    }
}

@Composable
private fun SongGridCard(
    song: Song,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapScaleSelectable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f))
                )
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_square_check_big),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistPickerSheet(playlists: List<Playlist>, onSelect: (Playlist) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Add to Playlist",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            if (playlists.isEmpty()) {
                Text(
                    "No playlists yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            } else {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                playlists.forEach { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tapScale { onSelect(playlist) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_list_music),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (playlist.songIds.size == 1) "1 song" else "${playlist.songIds.size} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// grid card, not a row: art (or a neutral tile for artists) is the primary scan cue at this size
@Composable
private fun AlbumCard(group: AlbumGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            AlbumArt(song = group.songs.first(), modifier = Modifier.fillMaxSize())
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 36.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(group.album, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            group.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// The list alternative intentionally keeps the same information hierarchy as songs: one clear
// artwork cue, two lines of metadata, and a consistently placed play action.
@Composable
private fun CollectionListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    artwork: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tapScale(onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                artwork()
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 36.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = onPlay
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
            modifier = Modifier.padding(start = 96.dp)
        )
    }
}

@Composable
private fun ArtistListRow(group: ArtistGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    CollectionListRow(
        title = group.artist,
        subtitle = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
        onClick = onClick,
        onPlay = onPlay
    ) {
        ArtistImage(artist = group.artist, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun AlbumListRow(group: AlbumGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    CollectionListRow(
        title = group.album,
        subtitle = "${group.artist} · ${if (group.songs.size == 1) "1 song" else "${group.songs.size} songs"}",
        onClick = onClick,
        onPlay = onPlay
    ) {
        AlbumArt(song = group.songs.first(), modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun GenreListRow(group: GenreGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    val rule = genreRuleFor(group.genre)
    CollectionListRow(
        title = group.genre,
        subtitle = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
        onClick = onClick,
        onPlay = onPlay
    ) {
        Icon(
            painter = painterResource(rule?.iconRes ?: R.drawable.lucide_ic_cassette_tape),
            contentDescription = null,
            tint = rule?.color ?: MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ArtistCard(group: ArtistGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            ArtistImage(artist = group.artist, modifier = Modifier.fillMaxSize())
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 36.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(group.artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// keyword -> icon, checked in order; first match wins, falls back to a cassette icon
private data class GenreIconRule(val keywords: Set<String>, val iconRes: Int, val color: Color)

// muted, dark-mode-friendly tones, deliberately kept clear of Record Red (#B3483A)
// so that accent stays the app's one "real" highlight color
private val GENRE_ICON_RULES = listOf(
    GenreIconRule(setOf("rock", "punk", "grunge", "metal", "country", "blues"), R.drawable.lucide_ic_guitar, Color(0xFFA6784A)),
    GenreIconRule(setOf("hip", "rap", "trap", "r&b", "rnb", "soul"), R.drawable.lucide_ic_mic_vocal, Color(0xFF8B6BAE)),
    GenreIconRule(setOf("electro", "dance", "edm", "techno", "house", "trance"), R.drawable.lucide_ic_zap, Color(0xFF4FA8C9)),
    GenreIconRule(setOf("jazz", "classical", "orchestral", "instrumental"), R.drawable.lucide_ic_piano, Color(0xFFB99A4B)),
    GenreIconRule(setOf("reggae", "ska"), R.drawable.lucide_ic_tree_palm, Color(0xFF5C9B6C)),
    GenreIconRule(setOf("folk", "acoustic"), R.drawable.lucide_ic_leaf, Color(0xFF7A8B5C)),
    GenreIconRule(setOf("ambient", "chill", "lofi", "lo-fi"), R.drawable.lucide_ic_waves, Color(0xFF6B7F9E)),
    GenreIconRule(setOf("pop"), R.drawable.lucide_ic_sparkles, Color(0xFFB06B8A)),
    GenreIconRule(setOf("gospel", "christian", "worship"), R.drawable.lucide_ic_church, Color(0xFFC9B37E)),
    GenreIconRule(setOf("podcast"), R.drawable.lucide_ic_radio, Color(0xFF7C8A96))
)

private fun genreRuleFor(genre: String): GenreIconRule? {
    val normalized = genre.lowercase()
    return GENRE_ICON_RULES.firstOrNull { rule -> rule.keywords.any { normalized.contains(it) } }
}

@Composable
private fun GenreCard(group: GenreGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    // Keep genre tiles deliberately quiet: a four-cover mosaic makes a small grid visually
    // noisy and gives unrelated releases equal prominence. One tonal card per genre is easier
    // to scan and stays consistent even when a collection has only a few songs.
    val rule = genreRuleFor(group.genre)
    val accent = rule?.color ?: MaterialTheme.colorScheme.tertiary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        accent.copy(alpha = 0.22f)
                    )
                )
            )
            .tapScale(onClick)
    ) {
        Icon(
            painter = painterResource(rule?.iconRes ?: R.drawable.lucide_ic_cassette_tape),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.align(Alignment.TopStart).padding(18.dp).size(34.dp)
        )
        TransportButton(
            iconRes = R.drawable.lucide_ic_play,
            size = 40.dp,
            tint = accent,
            accented = true,
            onClick = onPlay,
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp, 0.dp, 62.dp, 18.dp)) {
            Text(
                group.genre,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
            )
        }
    }
}

@Composable
private fun PlaylistGrid(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    likedSongs: List<Song>,
    listBottomPadding: Dp,
    onClick: (String) -> Unit,
    onPlay: (List<Song>) -> Unit,
    onOpenLikedSongs: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "liked-songs") {
            LikedSongsCard(
                songs = likedSongs,
                onClick = onOpenLikedSongs,
                onPlay = { if (likedSongs.isNotEmpty()) onPlay(likedSongs) }
            )
        }
        items(playlists, key = { it.id }) { playlist ->
            val songs = remember(playlist.songIds, allSongs) {
                playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
            }
            PlaylistCard(
                playlist = playlist,
                songs = songs,
                onClick = { onClick(playlist.id) },
                onPlay = { if (songs.isNotEmpty()) onPlay(songs) }
            )
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    likedSongs: List<Song>,
    state: androidx.compose.foundation.lazy.LazyListState,
    listBottomPadding: Dp,
    onClick: (String) -> Unit,
    onPlay: (List<Song>) -> Unit,
    onOpenLikedSongs: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
    ) {
        item(key = "liked-songs") {
            CollectionListRow(
                title = "Liked Songs",
                subtitle = if (likedSongs.size == 1) "1 song" else "${likedSongs.size} songs",
                onClick = onOpenLikedSongs,
                onPlay = { if (likedSongs.isNotEmpty()) onPlay(likedSongs) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_heart),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        items(playlists, key = { it.id }) { playlist ->
            val songs = remember(playlist.songIds, allSongs) {
                playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
            }
            CollectionListRow(
                title = playlist.name,
                subtitle = if (songs.size == 1) "1 song" else "${songs.size} songs",
                onClick = { onClick(playlist.id) },
                onPlay = { if (songs.isNotEmpty()) onPlay(songs) }
            ) {
                PlaylistCoverArt(
                    playlist = playlist,
                    fallbackSong = songs.firstOrNull(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LikedSongsCard(songs: List<Song>, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_heart),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(52.dp)
            )
            if (songs.isNotEmpty()) {
                TransportButton(
                    iconRes = R.drawable.lucide_ic_play,
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    accented = true,
                    onClick = onPlay,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Liked Songs", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        Text(
            if (songs.size == 1) "1 song" else "${songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// unresolvable songIds (deleted from disk) are just quietly dropped, same as favorites elsewhere
@Composable
private fun PlaylistCard(playlist: Playlist, songs: List<Song>, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            PlaylistCoverArt(playlist = playlist, fallbackSong = songs.firstOrNull(), modifier = Modifier.fillMaxSize())
            if (songs.isNotEmpty()) {
                TransportButton(
                    iconRes = R.drawable.lucide_ic_play,
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    accented = true,
                    onClick = onPlay,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(playlist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (songs.size == 1) "1 song" else "${songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
