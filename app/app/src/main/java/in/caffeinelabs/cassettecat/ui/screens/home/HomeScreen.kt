package `in`.caffeinelabs.cassettecat.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongRowSkeleton
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByAlbum
import `in`.caffeinelabs.cassettecat.ui.screens.library.groupedByArtist
import `in`.caffeinelabs.cassettecat.ui.screens.library.rememberSkeletonColor
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.util.Calendar

@Composable
fun HomeScreen(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val monthlyStats by playbackViewModel.monthlyStats.collectAsState()

    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())

    val allSongs = (libraryState as? LibraryUiState.Loaded)?.songs.orEmpty()
    val recentlyPlayed = playbackState.history
    val favorites = allSongs.filter { it.isFavorite }
    val shufflePicks = remember(allSongs) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 22 || hour < 5) {
            val soft = allSongs.filter { it.isSoftGenre() }.shuffled()
            (soft + allSongs.shuffled()).distinct().take(8)
        } else {
            allSongs.shuffled().take(8)
        }
    }
    val heroSong = remember(allSongs) { allSongs.randomOrNull() }

    val greeting = remember { getDynamicGreeting() }

    val heavyRotation = remember(allSongs, monthlyStats) {
        val counts = monthlyStats.values
            .flatMap { it.songPlayCounts.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { it.value.sum() }
        val songsById = allSongs.associateBy { it.id }
        counts.entries
            .filter { it.value > 0 && it.key in songsById }
            .sortedByDescending { it.value }
            .take(10)
            .mapNotNull { songsById[it.key] }
    }

    val recentlyAdded = remember(allSongs) {
        allSongs.sortedByDescending { it.dateAddedMs }.take(10)
    }

    val recentIds = remember(recentlyPlayed) { recentlyPlayed.map { it.id }.toSet() }
    val forgottenFavorites = remember(favorites, recentIds) {
        favorites.filter { it.id !in recentIds }.take(10)
    }

    fun play(songs: List<Song>, song: Song) {
        val index = songs.indexOfFirst { it.id == song.id }
        if (index == -1) return
        val wasIdle = playbackState.currentSong == null
        playbackViewModel.playQueue(songs, index)
        if (wasIdle) onNavigateToNowPlaying()
    }

    fun playAll(songs: List<Song>, shuffle: Boolean) {
        if (songs.isEmpty()) return
        val wasIdle = playbackState.currentSong == null
        playbackViewModel.playQueue(if (shuffle) songs.shuffled() else songs, 0)
        if (wasIdle) onNavigateToNowPlaying()
    }

    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                greeting,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Quick picks, recently played, and your favorites",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(10.dp))

        if (libraryState is LibraryUiState.Loading) {
            HomeSkeletonContent(listBottomPadding = listBottomPadding)
        } else if (allSongs.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_folder_search,
                title = "Nothing here yet",
                message = "Scan your library to get started.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(bottom = listBottomPadding + 24.dp)
            ) {
                item {
                    LibrarySnapshot(songs = allSongs, onClick = onNavigateToLibrary)
                    Spacer(Modifier.height(20.dp))
                }
                item {
                    ShuffleAllHero(
                        heroSong = heroSong,
                        songCount = allSongs.size,
                        onClick = { playAll(allSongs, shuffle = true) }
                    )
                    Spacer(Modifier.height(28.dp))
                }
                if (shufflePicks.isNotEmpty()) {
                    item {
                        ShufflePicksHeader()
                        Spacer(Modifier.height(12.dp))
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(shufflePicks, key = { it.id }) { song ->
                                HomeSongCard(song = song, onClick = { play(shufflePicks, song) })
                            }
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
                    if (preferences.showHomeHeavyRotation && heavyRotation.isNotEmpty()) {
                        item {
                            HomeSongSection(
                                title = "Heavy Rotation",
                                subtitle = "Your most played tracks",
                                songs = heavyRotation,
                                onSongClick = { play(heavyRotation, it) },
                                onPlay = { playAll(heavyRotation, shuffle = false) },
                                onShuffle = { playAll(heavyRotation, shuffle = true) }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    if (preferences.showHomeRecentlyPlayed && recentlyPlayed.isNotEmpty()) {
                        item {
                            HomeSongSection(
                                title = "Recently Played",
                                subtitle = "Pick up where you left off",
                                songs = recentlyPlayed,
                                onSongClick = { play(recentlyPlayed, it) },
                                onPlay = { playAll(recentlyPlayed, shuffle = false) },
                                onShuffle = { playAll(recentlyPlayed, shuffle = true) }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    if (preferences.showHomeRecentlyAdded && recentlyAdded.isNotEmpty()) {
                        item {
                            HomeSongSection(
                                title = "Recently Added",
                                subtitle = "Fresh in your library",
                                songs = recentlyAdded,
                                onSongClick = { play(recentlyAdded, it) },
                                onPlay = { playAll(recentlyAdded, shuffle = false) },
                                onShuffle = { playAll(recentlyAdded, shuffle = true) }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    if (preferences.showHomeForgottenFavorites && forgottenFavorites.isNotEmpty() && forgottenFavorites.size >= 3) {
                        item {
                            HomeSongSection(
                                title = "Forgotten Favorites",
                                subtitle = "Rediscover what you loved",
                                songs = forgottenFavorites,
                                onSongClick = { play(forgottenFavorites, it) },
                                onPlay = { playAll(forgottenFavorites, shuffle = false) },
                                onShuffle = { playAll(forgottenFavorites, shuffle = true) }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    if (favorites.isNotEmpty()) {
                        item {
                            HomeSongSection(
                                title = "Favorites",
                                subtitle = "Songs you've loved",
                                songs = favorites,
                                onSongClick = { play(favorites, it) },
                                onPlay = { playAll(favorites, shuffle = false) },
                                onShuffle = { playAll(favorites, shuffle = true) }
                            )
                        }
                    }
                }
            }
        }
    }

// Mirrors the summary, hero, and vertical Shuffle Picks list rather than using a generic spinner.
@Composable
private fun ColumnScope.HomeSkeletonContent(listBottomPadding: Dp) {
    val color = rememberSkeletonColor()
    Column(
        modifier = Modifier.fillMaxSize().weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            Modifier.padding(horizontal = 24.dp).width(220.dp).height(14.dp)
                .clip(RoundedCornerShape(4.dp)).background(color)
        )
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(190.dp)
                .clip(RoundedCornerShape(20.dp)).background(color)
        )
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(Modifier.width(140.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(220.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
        Column {
            repeat(7) { SongRowSkeleton() }
        }
        Spacer(Modifier.height(listBottomPadding))
    }
}

@Composable
private fun LibrarySnapshot(songs: List<Song>, onClick: () -> Unit) {
    val artistCount = songs.groupedByArtist().size
    val albumCount = songs.groupedByAlbum().size
    Text(
        "${songs.size} songs · $artistCount artists · $albumCount albums",
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp).tapScale(onClick)
    )
}

@Composable
private fun ShuffleAllHero(heroSong: Song?, songCount: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(216.dp)
            .clip(RoundedCornerShape(24.dp))
            .tapScale(onClick)
    ) {
        if (heroSong != null) {
            AlbumArt(song = heroSong, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Start somewhere new", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (songCount == 1) "Shuffle 1 song" else "Shuffle all $songCount songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_shuffle,
                contentDescription = "Shuffle library",
                onClick = onClick
            )
        }
    }
}

@Composable
private fun HomeSongSection(
    title: String,
    subtitle: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PressDepthIconButton(iconRes = R.drawable.lucide_ic_play, contentDescription = "Play $title", onClick = onPlay)
            PressDepthIconButton(iconRes = R.drawable.lucide_ic_shuffle, contentDescription = "Shuffle $title", onClick = onShuffle)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                HomeSongCard(song = song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
private fun HomeSongCard(song: Song, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).tapScale(onClick)) {
        Box(modifier = Modifier.size(140.dp).clip(RoundedCornerShape(8.dp))) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
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

@Composable
private fun ShufflePicksHeader() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("For right now", style = MaterialTheme.typography.titleLarge)
        Text(
            "A few picks from your library",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val SOFT_GENRE_KEYWORDS = setOf("ambient", "chill", "lofi", "lo-fi", "jazz", "classical", "acoustic", "folk", "instrumental")

private fun Song.isSoftGenre(): Boolean = genres.any { g -> SOFT_GENRE_KEYWORDS.any { g.lowercase().contains(it) } }

private fun getDynamicGreeting(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

    val options = when (hour) {
        in 5..8 -> listOf(
            "Good morning",
            "Early rise soundtrack",
            "Morning coffee & tape",
            "Start your day"
        )
        in 9..11 -> listOf(
            "Good morning",
            "Morning focus",
            "The daily rotation",
            "Morning soundscape",
            if (isWeekend) "Weekend morning" else "Workday soundtrack"
        )
        in 12..16 -> listOf(
            "Good afternoon",
            "Afternoon session",
            "Midday rhythm",
            "Afternoon groove",
            if (isWeekend) "Weekend afternoon" else "Afternoon flow"
        )
        in 17..21 -> listOf(
            "Good evening",
            "Golden hour grooves",
            "Evening listening",
            "Winding down",
            if (isWeekend) "Saturday night session" else "Evening unwind"
        )
        in 22..23 -> listOf(
            "Late night session",
            "Midnight frequencies",
            "Night owl tunes",
            "Low-light listening",
            "Late night rotation"
        )
        else -> listOf(
            "Late night session",
            "Deep night vibes",
            "Midnight tape",
            "Insomnia session",
            "Quiet hours",
            "After-hours rotation"
        )
    }
    return options.random()
}

