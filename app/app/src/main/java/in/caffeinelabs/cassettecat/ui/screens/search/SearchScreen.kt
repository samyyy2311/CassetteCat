package `in`.caffeinelabs.cassettecat.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.ArtistGroup
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongRow
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByArtist
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

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
    modifier: Modifier = Modifier,
    focusRequestId: Int = 0,
    listBottomPadding: Dp = 0.dp
) {
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = query.isNotEmpty()) {
        query = ""
        keyboardController?.hide()
    }

    LaunchedEffect(focusRequestId) {
        if (focusRequestId > 0) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val allSongs = (libraryState as? LibraryUiState.Loaded)?.songs.orEmpty()
    val results = remember(query, allSongs) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            emptyList()
        } else {
            val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            allSongs
                .mapNotNull { song ->
                    val score = scoreSongMatch(song, trimmed, tokens)
                    if (score > 0) song to score else null
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }
    }

    val topArtists = remember(allSongs) {
        allSongs.groupedByArtist().sortedByDescending { it.songs.size }.take(10)
    }

    fun playSong(songs: List<Song>, song: Song) {
        val index = songs.indexOfFirst { it.id == song.id }
        if (index == -1) return
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        playbackViewModel.playQueue(songs, index)
        if (wasIdle) onNavigateToNowPlaying()
    }

    Column(modifier = modifier.fillMaxSize().padding(top = 4.dp)) {
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
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search your library") },
            leadingIcon = {
                Icon(painter = painterResource(R.drawable.lucide_ic_search), contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_x,
                        contentDescription = "Clear",
                        onClick = { query = "" }
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).focusRequester(focusRequester)
        )
        Spacer(Modifier.height(16.dp))

        when {
            query.isBlank() -> {
                if (topArtists.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = listBottomPadding + 24.dp)
                    ) {
                        item {
                            SearchSectionHeader("Top Artists")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
                            ) {
                                items(topArtists, key = { it.artist }) { artistGroup ->
                                    SearchArtistChip(
                                        artistGroup = artistGroup,
                                        onClick = { query = artistGroup.artist }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    SearchPrompt(
                        iconRes = R.drawable.lucide_ic_search,
                        title = "Search your library",
                        subtitle = "Find songs by title, artist, or album.",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            libraryState is LibraryUiState.Loading -> SearchPrompt(
                iconRes = R.drawable.lucide_ic_search,
                title = "Loading your library...",
                subtitle = "Search will work once it is ready.",
                modifier = Modifier.weight(1f)
            )

            results.isEmpty() -> SearchPrompt(
                iconRes = R.drawable.lucide_ic_search_x,
                title = "No matches",
                subtitle = "Nothing found for \"$query\".",
                modifier = Modifier.weight(1f)
            )

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = listBottomPadding + 24.dp)
            ) {
                items(results, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        onClick = { playSong(results, song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
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
private fun SearchPrompt(iconRes: Int, title: String, subtitle: String, modifier: Modifier = Modifier) {
    EmptyState(iconRes = iconRes, title = title, message = subtitle, modifier = modifier)
}
