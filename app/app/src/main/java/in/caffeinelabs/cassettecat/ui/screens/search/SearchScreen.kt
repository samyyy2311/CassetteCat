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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.SearchHistoryRepository
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.ArtistGroup
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongRow
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByAlbum
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByArtist
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByFolder
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByGenre
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
    val coroutineScope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(SearchCategory.ALL) }
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

    // Matched results per entity type
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

    val matchedArtists = remember(trimmed, allSongs) {
        if (trimmed.isBlank()) emptyList()
        else allSongs.groupedByArtist().filter {
            it.artist.contains(trimmed, ignoreCase = true) ||
                it.songs.any { s -> s.artist.contains(trimmed, ignoreCase = true) }
        }
    }

    val matchedAlbums = remember(trimmed, allSongs) {
        if (trimmed.isBlank()) emptyList()
        else allSongs.groupedByAlbum().filter {
            it.album.contains(trimmed, ignoreCase = true) ||
                it.artist.contains(trimmed, ignoreCase = true)
        }
    }

    val matchedGenres = remember(trimmed, allSongs) {
        if (trimmed.isBlank()) emptyList()
        else allSongs.groupedByGenre().filter {
            it.genre.contains(trimmed, ignoreCase = true)
        }
    }

    val matchedFolders = remember(trimmed, allSongs) {
        if (trimmed.isBlank()) emptyList()
        else allSongs.groupedByFolder().filter {
            it.folderName.contains(trimmed, ignoreCase = true) ||
                it.folderPath.contains(trimmed, ignoreCase = true)
        }
    }

    val topResult = remember(trimmed, matchedArtists, matchedAlbums, matchedSongs) {
        if (trimmed.isBlank()) null
        else {
            val exactArtist = matchedArtists.firstOrNull { it.artist.equals(trimmed, ignoreCase = true) }
                ?: matchedArtists.firstOrNull { it.artist.startsWith(trimmed, ignoreCase = true) }
            val exactAlbum = matchedAlbums.firstOrNull { it.album.equals(trimmed, ignoreCase = true) }
                ?: matchedAlbums.firstOrNull { it.album.startsWith(trimmed, ignoreCase = true) }
            val exactSong = matchedSongs.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }

            when {
                exactArtist != null -> TopSearchResult.ArtistResult(exactArtist)
                exactAlbum != null -> TopSearchResult.AlbumResult(exactAlbum)
                exactSong != null -> TopSearchResult.SongResult(exactSong)
                matchedArtists.isNotEmpty() -> TopSearchResult.ArtistResult(matchedArtists.first())
                matchedAlbums.isNotEmpty() -> TopSearchResult.AlbumResult(matchedAlbums.first())
                matchedSongs.isNotEmpty() -> TopSearchResult.SongResult(matchedSongs.first())
                else -> null
            }
        }
    }

    val topArtists = remember(allSongs) {
        allSongs.groupedByArtist().sortedByDescending { it.songs.size }.take(10)
    }

    val popularGenres = remember(allSongs) {
        allSongs.groupedByGenre().sortedByDescending { it.songs.size }.take(8)
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
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Find songs, artists, albums, and genres",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) selectedCategory = SearchCategory.ALL
            },
            placeholder = { Text("Search your library") },
            leadingIcon = {
                Icon(painter = painterResource(R.drawable.lucide_ic_search), contentDescription = null)
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
        Spacer(Modifier.height(12.dp))

        when {
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
                                topResult?.let { result ->
                                    item(key = "top_result", contentType = "top_result") {
                                        TopResultSpotlightCard(
                                            result = result,
                                            onNavigateToArtist = {
                                                recordQuery()
                                                onNavigateToArtist(it)
                                            },
                                            onNavigateToAlbum = {
                                                recordQuery()
                                                onNavigateToAlbum(it)
                                            },
                                            onPlaySong = { playSong(matchedSongs, it) },
                                            onPlayGroup = { playGroup(it) }
                                        )
                                        Spacer(Modifier.height(16.dp))
                                    }
                                }

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
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
            Text(
                text = if (artistGroup.songs.size == 1) "1 song" else "${artistGroup.songs.size} songs",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    EmptyState(iconRes = iconRes, title = title, message = subtitle, modifier = modifier)
}
