package `in`.caffeinelabs.cassettecat.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.SearchHistoryRepository
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.ArtistGroup
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.PlaylistPickerSheet
import `in`.caffeinelabs.cassettecat.ui.screens.library.PlaylistViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongOptionsSheet
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongRow
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongTagEditorSheet
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByAlbum
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByArtist
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByFolder
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByGenre
import `in`.caffeinelabs.cassettecat.ui.util.shareSongs
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.launch

private fun scoreSongMatch(song: Song, query: String, tokens: List<String>): Int {
    val q = query.trim().lowercase()
    val title = song.title.lowercase()
    val artist = song.artist.lowercase()
    val album = song.album.lowercase()

    if (title == q) return 1000
    if (artist == q) return 800
    if (title.startsWith(q)) return 600
    if (artist.startsWith(q)) return 500

    var score = 0
    if (title.contains(q)) score += 300
    if (artist.contains(q)) score += 200
    if (album.contains(q)) score += 100

    val allFields = "$title $artist $album"
    val allTokensMatch = tokens.all { token -> allFields.contains(token) }
    if (!allTokensMatch && score == 0) return 0

    tokens.forEach { token ->
        if (title.contains(token)) score += 60
        if (artist.contains(token)) score += 50
        if (album.contains(token)) score += 25
        if (title.split(" ", "-", "_", "/").any { it.startsWith(token) }) score += 40
        if (artist.split(" ", "-", "_", "/").any { it.startsWith(token) }) score += 35
    }

    return score
}

@Composable
fun SearchScreen(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    playlistViewModel: PlaylistViewModel = viewModel(),
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToGenre: (String) -> Unit = {},
    onNavigateToFolder: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    focusRequestId: Int = 0,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val searchHistoryRepo = remember { SearchHistoryRepository.getInstance(context) }
    val recentQueries by searchHistoryRepo.recentQueries.collectAsStateWithLifecycle(initialValue = emptyList())
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val favoritesRepository = remember { FavoritesRepository(context) }
    val favoriteIds by favoritesRepository.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val coroutineScope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(SearchCategory.ALL) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var songForOptions by remember { mutableStateOf<Song?>(null) }
    var songForTagEdit by remember { mutableStateOf<Song?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = query.isNotEmpty()) {
        query = ""
        selectedCategory = SearchCategory.ALL
        keyboardController?.hide()
    }

    LaunchedEffect(focusRequestId) {
        if (focusRequestId > 0) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val allSongs = (libraryState as? LibraryUiState.Loaded)?.songs.orEmpty()

    val trimmed = query.trim()
    val tokens = remember(trimmed) {
        trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    }

    val matchedSongs = remember(trimmed, allSongs) {
        if (trimmed.isBlank()) emptyList()
        else allSongs
            .mapNotNull { song ->
                val score = scoreSongMatch(song, trimmed, tokens)
                if (score > 0) song to score else null
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    val allArtists = remember(allSongs) { allSongs.groupedByArtist() }
    val allAlbums = remember(allSongs) { allSongs.groupedByAlbum() }
    val allGenres = remember(allSongs) { allSongs.groupedByGenre() }
    val allFolders = remember(allSongs) { allSongs.groupedByFolder() }

    val matchedArtists = remember(trimmed, allArtists) {
        if (trimmed.isBlank()) emptyList()
        else allArtists.filter {
            it.artist.contains(trimmed, ignoreCase = true) ||
                it.songs.any { s -> s.artist.contains(trimmed, ignoreCase = true) }
        }
    }

    val matchedAlbums = remember(trimmed, allAlbums) {
        if (trimmed.isBlank()) emptyList()
        else allAlbums.filter {
            it.album.contains(trimmed, ignoreCase = true) ||
                it.artist.contains(trimmed, ignoreCase = true)
        }
    }

    val matchedGenres = remember(trimmed, allGenres) {
        if (trimmed.isBlank()) emptyList()
        else allGenres.filter {
            it.genre.contains(trimmed, ignoreCase = true)
        }
    }

    val matchedFolders = remember(trimmed, allFolders) {
        if (trimmed.isBlank()) emptyList()
        else allFolders.filter {
            it.folderName.contains(trimmed, ignoreCase = true) ||
                it.folderPath.contains(trimmed, ignoreCase = true)
        }
    }

    val topArtists = remember(allArtists) {
        allArtists.sortedByDescending { it.songs.size }.take(10)
    }

    val popularGenres = remember(allGenres) {
        allGenres.sortedByDescending { it.songs.size }.take(8)
    }

    fun recordQuery() {
        if (query.isNotBlank()) {
            coroutineScope.launch { searchHistoryRepo.addQuery(query) }
        }
    }

    fun playSong(songs: List<Song>, song: Song) {
        recordQuery()
        val index = songs.indexOfFirst { it.id == song.id }
        if (index == -1) return
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        playbackViewModel.playQueue(songs, index)
        if (wasIdle) onNavigateToNowPlaying()
    }

    fun playGroup(songs: List<Song>, shuffle: Boolean = false) {
        recordQuery()
        if (songs.isEmpty()) return
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        if (shuffle) {
            playbackViewModel.shuffleAll(songs)
        } else {
            playbackViewModel.playQueue(songs, 0, shuffle = false)
        }
        if (wasIdle) onNavigateToNowPlaying()
    }

    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                "Search",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Find songs, artists, albums, and genres",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) selectedCategory = SearchCategory.ALL
            },
            placeholder = {
                Text(
                    "Search songs, artists, albums…",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_x,
                        contentDescription = "Clear",
                        onClick = {
                            query = ""
                            selectedCategory = SearchCategory.ALL
                        }
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    recordQuery()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .focusRequester(focusRequester)
        )
        Spacer(Modifier.height(14.dp))

        when {
            query.isBlank() && recentQueries.isEmpty() && topArtists.isEmpty() && popularGenres.isEmpty() -> {
                SearchPrompt(
                    iconRes = R.drawable.lucide_ic_search,
                    title = "Search your library",
                    subtitle = "Find songs, artists, albums, and more.",
                    modifier = Modifier.weight(1f)
                )
            }

            query.isBlank() -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = listBottomPadding + 24.dp)
                ) {
                    if (recentQueries.isNotEmpty()) {
                        item(key = "recent_searches") {
                            RecentSearchesSection(
                                recentQueries = recentQueries,
                                onSelectQuery = { selectedText ->
                                    query = selectedText
                                    recordQuery()
                                },
                                onRemoveQuery = { removedText ->
                                    coroutineScope.launch { searchHistoryRepo.removeQuery(removedText) }
                                },
                                onClearAll = {
                                    coroutineScope.launch { searchHistoryRepo.clearHistory() }
                                }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    if (topArtists.isNotEmpty()) {
                        item(key = "top_artists") {
                            SearchSectionHeader("Top Artists")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
                            ) {
                                items(topArtists, key = { it.artist }) { artistGroup ->
                                    SearchArtistChip(
                                        artistGroup = artistGroup,
                                        onClick = {
                                            recordQuery()
                                            onNavigateToArtist(artistGroup.artist)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (popularGenres.isNotEmpty()) {
                        item(key = "explore_genres") {
                            SearchSectionHeader("Explore Genres")
                            Spacer(Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                popularGenres.chunked(2).forEach { rowGenres ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowGenres.forEach { genreGroup ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                SearchGenreTile(
                                                    genreGroup = genreGroup,
                                                    onClick = {
                                                        recordQuery()
                                                        onNavigateToGenre(genreGroup.genre)
                                                    }
                                                )
                                            }
                                        }
                                        if (rowGenres.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            libraryState is LibraryUiState.Loading -> SearchPrompt(
                iconRes = R.drawable.lucide_ic_search,
                title = "Loading your library...",
                subtitle = "Search will work once it is ready.",
                modifier = Modifier.weight(1f)
            )

            matchedSongs.isEmpty() && matchedArtists.isEmpty() && matchedAlbums.isEmpty() && matchedGenres.isEmpty() && matchedFolders.isEmpty() -> {
                SearchPrompt(
                    iconRes = R.drawable.lucide_ic_search_x,
                    title = "No matches",
                    subtitle = "Nothing found for \"$query\".",
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                Column(modifier = Modifier.weight(1f)) {
                    SearchCategoryPills(
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = listBottomPadding + 24.dp)
                    ) {
                        when (selectedCategory) {
                            SearchCategory.ALL -> {
                                if (matchedArtists.isNotEmpty()) {
                                    item(key = "section_artists", contentType = "section_artists") {
                                        SearchSectionHeader("Artists")
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
                                        ) {
                                            items(matchedArtists, key = { it.artist }) { artistGroup ->
                                                SearchArtistChip(
                                                    artistGroup = artistGroup,
                                                    onClick = {
                                                        recordQuery()
                                                        onNavigateToArtist(artistGroup.artist)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (matchedAlbums.isNotEmpty()) {
                                    item(key = "section_albums", contentType = "section_albums") {
                                        SearchSectionHeader("Albums")
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
                                        ) {
                                            items(matchedAlbums, key = { it.albumId }) { albumGroup ->
                                                SearchAlbumCard(
                                                    albumGroup = albumGroup,
                                                    onClick = {
                                                        recordQuery()
                                                        onNavigateToAlbum(albumGroup.albumId)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (matchedSongs.isNotEmpty()) {
                                    item(key = "section_songs_header", contentType = "section_songs_header") {
                                        SearchSectionHeader("Songs")
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    items(matchedSongs.take(15), key = { it.id }, contentType = { "song" }) { song ->
                                        SongRow(
                                            song = song,
                                            onMoreClick = { songForOptions = song },
                                            onClick = { playSong(matchedSongs, song) }
                                        )
                                    }
                                }

                                if (matchedGenres.isNotEmpty()) {
                                    item(key = "section_genres", contentType = "section_genres") {
                                        Spacer(Modifier.height(12.dp))
                                        SearchSectionHeader("Genres")
                                        Spacer(Modifier.height(8.dp))
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            matchedGenres.take(4).chunked(2).forEach { rowGenres ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    rowGenres.forEach { genreGroup ->
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            SearchGenreTile(
                                                                genreGroup = genreGroup,
                                                                onClick = {
                                                                    recordQuery()
                                                                    onNavigateToGenre(genreGroup.genre)
                                                                }
                                                            )
                                                        }
                                                    }
                                                    if (rowGenres.size == 1) {
                                                        Spacer(Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (matchedFolders.isNotEmpty()) {
                                    item(key = "section_folders", contentType = "section_folders") {
                                        Spacer(Modifier.height(16.dp))
                                        SearchSectionHeader("Folders")
                                        Spacer(Modifier.height(8.dp))
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            matchedFolders.take(4).forEach { folderGroup ->
                                                SearchFolderRow(
                                                    folderGroup = folderGroup,
                                                    onClick = {
                                                        recordQuery()
                                                        onNavigateToFolder(folderGroup.folderPath)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SearchCategory.SONGS -> {
                                items(matchedSongs, key = { it.id }, contentType = { "song" }) { song ->
                                    SongRow(
                                        song = song,
                                        onMoreClick = { songForOptions = song },
                                        onClick = { playSong(matchedSongs, song) }
                                    )
                                }
                            }

                            SearchCategory.ARTISTS -> {
                                items(matchedArtists, key = { it.artist }) { artistGroup ->
                                    SearchArtistRow(
                                        artistGroup = artistGroup,
                                        onClick = {
                                            recordQuery()
                                            onNavigateToArtist(artistGroup.artist)
                                        }
                                    )
                                }
                            }

                            SearchCategory.ALBUMS -> {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        matchedAlbums.chunked(2).forEach { rowAlbums ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                rowAlbums.forEach { albumGroup ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        SearchAlbumCard(
                                                            albumGroup = albumGroup,
                                                            onClick = {
                                                                recordQuery()
                                                                onNavigateToAlbum(albumGroup.albumId)
                                                            },
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                                if (rowAlbums.size == 1) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SearchCategory.GENRES -> {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        matchedGenres.chunked(2).forEach { rowGenres ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowGenres.forEach { genreGroup ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        SearchGenreTile(
                                                            genreGroup = genreGroup,
                                                            onClick = {
                                                                recordQuery()
                                                                onNavigateToGenre(genreGroup.genre)
                                                            }
                                                        )
                                                    }
                                                }
                                                if (rowGenres.size == 1) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SearchCategory.FOLDERS -> {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        matchedFolders.forEach { folderGroup ->
                                            SearchFolderRow(
                                                folderGroup = folderGroup,
                                                onClick = {
                                                    recordQuery()
                                                    onNavigateToFolder(folderGroup.folderPath)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlaylistPicker) {
        PlaylistPickerSheet(
            playlists = playlists,
            onSelect = { playlist ->
                songForOptions?.let { playlistViewModel.addSongs(playlist.id, listOf(it.id)) }
                showPlaylistPicker = false
                songForOptions = null
            },
            onDismiss = {
                showPlaylistPicker = false
                songForOptions = null
            }
        )
    }

    if (!showPlaylistPicker) songForOptions?.let { song ->
        val isFav = song.isFavorite || song.id in favoriteIds
        SongOptionsSheet(
            song = song,
            isFavorite = isFav,
            onPlayNext = {
                playbackViewModel.addToUpNext(listOf(song))
                songForOptions = null
            },
            onAddToQueue = {
                playbackViewModel.addToEndOfQueue(listOf(song))
                songForOptions = null
            },
            onAddToPlaylist = {
                showPlaylistPicker = true
            },
            onToggleFavorite = {
                coroutineScope.launch {
                    favoritesRepository.setFavorite(song.id, !isFav)
                }
                songForOptions = null
            },
            onShare = {
                shareSongs(context, listOf(song))
                songForOptions = null
            },
            onEditTags = {
                val s = song
                songForOptions = null
                songForTagEdit = s
            },
            onDismiss = { songForOptions = null }
        )
    }

    songForTagEdit?.let { song ->
        SongTagEditorSheet(
            song = song,
            onDismiss = { songForTagEdit = null },
            onSaved = { updated ->
                libraryViewModel.updateSongMetadata(updated)
                playbackViewModel.updateSongMetadata(updated)
                songForTagEdit = null
            }
        )
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun SearchArtistChip(artistGroup: ArtistGroup, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .tapScale(onClick)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            ArtistImage(
                artist = artistGroup.artist,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            artistGroup.artist,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchArtistRow(artistGroup: ArtistGroup, onClick: () -> Unit) {
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
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            ArtistImage(
                artist = artistGroup.artist,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artistGroup.artist,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (artistGroup.songs.size == 1) "1 song" else "${artistGroup.songs.size} songs",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val remoteSource = artistGroup.songs.firstOrNull { it.source != MusicSource.Local }?.source
                if (remoteSource != null && artistGroup.songs.all { it.source == remoteSource }) {
                    val (srcLabel, srcColor) = when (remoteSource) {
                        MusicSource.Subsonic -> "Subsonic" to Color(0xFFFF8500)
                        MusicSource.Jellyfin -> "Jellyfin" to Color(0xFF00A4DC)
                        else -> "" to Color.Unspecified
                    }
                    if (srcLabel.isNotEmpty()) {
                        Text(
                            text = srcLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 9.sp),
                            color = srcColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(srcColor.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
        Icon(
            painter = painterResource(R.drawable.lucide_ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SearchPrompt(iconRes: Int, title: String, subtitle: String, modifier: Modifier = Modifier) {
    EmptyState(
        catRes = AppR.drawable.cat_orange_headphones,
        title = title,
        message = subtitle,
        modifier = modifier
    )
}
