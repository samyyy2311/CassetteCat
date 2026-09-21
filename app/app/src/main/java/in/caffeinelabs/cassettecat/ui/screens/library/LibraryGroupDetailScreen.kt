package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.AlbumCoverRepository
import `in`.caffeinelabs.cassettecat.data.library.AlbumCoverStorage
import `in`.caffeinelabs.cassettecat.ui.components.invalidateAlbumArtCache
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.FolderCoverRepository
import `in`.caffeinelabs.cassettecat.data.library.FolderCoverStorage
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullScreenArtworkSheet
import `in`.caffeinelabs.cassettecat.data.library.ArtistBiography
import `in`.caffeinelabs.cassettecat.data.library.WikipediaInfoLoader
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.rememberLocalFileCoverBitmap
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.DownloadStatusIcon
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.deleteSongFile
import `in`.caffeinelabs.cassettecat.ui.util.retryDeleteAfterConsent
import `in`.caffeinelabs.cassettecat.ui.util.shareSongs
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
import `in`.caffeinelabs.cassettecat.ui.theme.ArtworkAtmospherePalette
import `in`.caffeinelabs.cassettecat.ui.theme.extractArtworkAtmospherePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ArtistDetailScreen(
    artist: String,
    libraryViewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val sourceFilter by libraryViewModel.sourceFilter.collectAsStateWithLifecycle()
    val allSongs = (uiState as? LibraryUiState.Loaded)?.songs?.filter { artist in it.artist.splitArtists() }.orEmpty()
    val songs = allSongs.filterBySource(sourceFilter).ifEmpty { allSongs }

    ArtistCatalogScreen(
        artist = artist,
        songs = songs,
        playbackViewModel = playbackViewModel,
        onBack = onBack,
        onNavigateToNowPlaying = onNavigateToNowPlaying,
        onNavigateToAlbum = onNavigateToAlbum,
        modifier = modifier,
        listBottomPadding = listBottomPadding
    )
}

private data class ArtistAlbum(val cover: Song, val tracks: List<Song>)

@Composable
private fun ArtistCatalogScreen(
    artist: String,
    songs: List<Song>,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    modifier: Modifier,
    listBottomPadding: Dp
) {
    val context = LocalContext.current
    val wikipediaLoader = remember { WikipediaInfoLoader() }
    val serviceSettings = remember { ServiceSettingsRepository(context) }
    var about by remember(artist) { mutableStateOf<ArtistBiography?>(null) }
    LaunchedEffect(artist) {
        val settings = serviceSettings.settings.first()
        about = wikipediaLoader.fetchArtistBiography(
            artist = artist,
            wikipediaEnabled = settings.isEnabled(ExternalService.WIKIPEDIA),
            audioDbEnabled = settings.isEnabled(ExternalService.AUDIODB)
        )
    }
    val albums = remember(songs) {
        songs.groupBy { it.albumId }
            .map { (_, tracks) -> ArtistAlbum(tracks.first(), tracks) }
            .sortedByDescending { it.cover.releaseYear ?: 0 }
    }
    val appearsOn = remember(albums, artist) {
        albums.filter { it.cover.artist.splitArtists().firstOrNull() != artist }
    }
    val artistReleases = remember(albums, appearsOn) {
        val appearanceIds = appearsOn.map { it.cover.albumId }.toSet()
        albums.filterNot { it.cover.albumId in appearanceIds }
    }
    val singles = remember(artistReleases) {
        artistReleases.filter {
            it.tracks.size <= 3 ||
                it.cover.album.contains("single", ignoreCase = true) ||
                it.cover.album.contains("ep", ignoreCase = true)
        }
    }
    val albumsOnly = remember(artistReleases, singles) {
        val singleIds = singles.map { it.cover.albumId }.toSet()
        artistReleases.filterNot { it.cover.albumId in singleIds }
    }
    val recentTracks = remember(songs) { songs.sortedByDescending { it.dateAddedMs }.take(5) }
    val allSongsSorted = remember(songs) { songs.sortedBy { it.title.lowercase() } }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val heroHeightPx = with(density) { 430.dp.toPx() }
    val compactHeaderProgress by remember(listState, heroHeightPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else {
                val heroScroll = listState.firstVisibleItemScrollOffset / heroHeightPx
                ((heroScroll - 0.35f) / 0.65f).coerceIn(0f, 1f)
            }
        }
    }

    fun playAll() {
        if (songs.isNotEmpty()) {
            val wasIdle = playbackViewModel.playbackState.value.currentSong == null
            playbackViewModel.playQueue(songs, 0)
            if (wasIdle) onNavigateToNowPlaying()
        }
    }

    fun shuffleAll() {
        if (songs.isNotEmpty()) {
            val wasIdle = playbackViewModel.playbackState.value.currentSong == null
            playbackViewModel.shuffleAll(songs)
            if (wasIdle) onNavigateToNowPlaying()
        }
    }

    fun play(song: Song) {
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        playbackViewModel.playQueue(songs, songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
        if (wasIdle) onNavigateToNowPlaying()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = listBottomPadding)) {
            item(key = "hero", contentType = "hero") {
                ArtistCatalogHero(
                    artist = artist,
                    songCount = songs.size,
                    albumCount = albums.size,
                    totalDurationMs = songs.sumOf { it.durationMs },
                    collapseProgress = compactHeaderProgress,
                    onBack = onBack,
                    onPlay = ::playAll,
                    onShuffle = ::shuffleAll
                )
            }
            if (recentTracks.isNotEmpty()) {
                item(key = "recent-title", contentType = "section_title") { ArtistSectionTitle("Recently Added") }
                itemsIndexed(recentTracks, key = { _, it -> "recent:${it.id}" }, contentType = { _, _ -> "song" }) { index, song ->
                    ArtistSongRow(song = song, trackIndex = index + 1, onClick = { play(song) })
                }
            }
            if (albumsOnly.isNotEmpty()) {
                item(key = "albums", contentType = "album_shelf") {
                    ArtistAlbumShelf(
                        title = "Albums",
                        albums = albumsOnly,
                        onAlbumClick = { onNavigateToAlbum(it.cover.albumId) }
                    )
                }
            }
            if (singles.isNotEmpty()) {
                item(key = "singles", contentType = "album_shelf") {
                    ArtistAlbumShelf(
                        title = "Singles & EPs",
                        albums = singles,
                        onAlbumClick = { onNavigateToAlbum(it.cover.albumId) }
                    )
                }
            }
            if (appearsOn.isNotEmpty()) {
                item(key = "appears", contentType = "album_shelf") {
                    ArtistAlbumShelf(
                        title = "Appears On",
                        albums = appearsOn,
                        onAlbumClick = { onNavigateToAlbum(it.cover.albumId) }
                    )
                }
            }
            if (allSongsSorted.isNotEmpty()) {
                item(key = "all-songs-title", contentType = "section_title") { ArtistSectionTitle("All Songs") }
                itemsIndexed(allSongsSorted, key = { _, it -> "all:${it.id}" }, contentType = { _, _ -> "song" }) { index, song ->
                    ArtistSongRow(song = song, trackIndex = index + 1, onClick = { play(song) })
                }
            }
            about?.let { biography ->
                item(key = "about", contentType = "about") { ArtistAboutSection(biography) }
            }
        }
        if (compactHeaderProgress > 0f) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = compactHeaderProgress))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .graphicsLayer { alpha = compactHeaderProgress },
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
                Text(
                    artist,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).graphicsLayer {
                        alpha = ((compactHeaderProgress - 0.62f) / 0.38f).coerceIn(0f, 1f)
                    }
                )
                TransportButton(
                    iconRes = R.drawable.lucide_ic_play,
                    size = 40.dp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    accented = true,
                    onClick = ::playAll
                )
                Spacer(Modifier.width(8.dp))
                TransportButton(
                    iconRes = R.drawable.lucide_ic_shuffle,
                    size = 40.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = ::shuffleAll
                )
            }
        }
    }
}

@Composable
private fun ArtistAboutSection(biography: ArtistBiography) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(20.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("About the Artist", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_book_open),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    biography.source,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = biography.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (biography.text.length > 160 || biography.text.lines().size > 2) {
            Text(
                text = if (expanded) "Show less" else "Read more",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun ArtistCatalogHero(
    artist: String,
    songCount: Int,
    albumCount: Int,
    totalDurationMs: Long,
    collapseProgress: Float,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    val durationText = remember(totalDurationMs) {
        if (totalDurationMs > 0) {
            val totalSeconds = totalDurationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
        } else ""
    }

    val statsText = listOfNotNull(
        "$songCount ${if (songCount == 1) "song" else "songs"}",
        "$albumCount ${if (albumCount == 1) "album" else "albums"}",
        durationText.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Box(modifier = Modifier.fillMaxWidth().height(430.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        ArtistImage(artist = artist, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, thumbnail = false)
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.35f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.70f),
                        Color.Black.copy(alpha = 0.95f)
                    )
                )
            )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .graphicsLayer { alpha = (1f - ((collapseProgress - 0.18f) / 0.58f)).coerceIn(0f, 1f) }
        ) {
            Text(
                artist,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    statsText,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_chevron_left,
            contentDescription = "Back",
            onClick = onBack,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp)
        )
        CatalogPlaybackActions(
            onPlay = onPlay,
            onShuffle = onShuffle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .graphicsLayer { alpha = (1f - (collapseProgress * 2f)).coerceIn(0f, 1f) }
        )
    }
}

@Composable
private fun CatalogPlaybackActions(onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(
            iconRes = R.drawable.lucide_ic_play,
            size = 56.dp,
            tint = MaterialTheme.colorScheme.tertiary,
            accented = true,
            onClick = onPlay
        )
        TransportButton(
            iconRes = R.drawable.lucide_ic_shuffle,
            size = 56.dp,
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onShuffle
        )
    }
}

@Composable
private fun ArtistSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp))
}

@Composable
private fun ArtistAlbumShelf(title: String, albums: List<ArtistAlbum>, onAlbumClick: (ArtistAlbum) -> Unit) {
    if (albums.isEmpty()) return
    Column(modifier = Modifier.padding(top = 20.dp)) {
        ArtistSectionTitle(title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            items(albums, key = { it.cover.albumId }) { album ->
                Column(modifier = Modifier.width(170.dp).tapScale { onAlbumClick(album) }) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        AlbumArt(song = album.cover, modifier = Modifier.fillMaxSize())
                        album.cover.releaseYear?.let { year ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    year.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 10.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        album.cover.album,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "${album.tracks.size} ${if (album.tracks.size == 1) "track" else "tracks"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    albumId: String,
    libraryViewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistViewModel: PlaylistViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val sourceFilter by libraryViewModel.sourceFilter.collectAsStateWithLifecycle()
    val allSongs = (uiState as? LibraryUiState.Loaded)?.songs?.filter { it.albumId == albumId }.orEmpty()
    val songs = allSongs.filterBySource(sourceFilter).ifEmpty { allSongs }

    LibraryGroupDetailScreen(
        title = songs.firstOrNull()?.album ?: "Album",
        subtitle = songs.firstOrNull()?.artist,
        wikipediaQuery = songs.firstOrNull()?.album,
        wikipediaAlbumArtist = songs.firstOrNull()?.artist,
        artistForHero = null,
        albumHeroSong = songs.firstOrNull(),
        playlistViewModel = playlistViewModel,
        metadata = emptyList(),
        songs = songs,
        playbackViewModel = playbackViewModel,
        onBack = onBack,
        onNavigateToNowPlaying = onNavigateToNowPlaying,
        onUpdateSong = { libraryViewModel.updateSongMetadata(it) },
        modifier = modifier,
        listBottomPadding = listBottomPadding
    )
}

@Composable
fun GenreDetailScreen(
    genre: String,
    libraryViewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val sourceFilter by libraryViewModel.sourceFilter.collectAsStateWithLifecycle()
    val allSongs = (uiState as? LibraryUiState.Loaded)?.songs?.filter { genre in it.effectiveGenres() }.orEmpty()
    val songs = allSongs.filterBySource(sourceFilter).ifEmpty { allSongs }

    LibraryGroupDetailScreen(
        title = genre,
        subtitle = null,
        wikipediaQuery = null,
        artistForHero = null,
        albumHeroSong = null,
        metadata = emptyList(),
        songs = songs,
        playbackViewModel = playbackViewModel,
        onBack = onBack,
        onNavigateToNowPlaying = onNavigateToNowPlaying,
        onUpdateSong = { libraryViewModel.updateSongMetadata(it) },
        modifier = modifier,
        listBottomPadding = listBottomPadding
    )
}

@Composable
fun FolderDetailScreen(
    folderPath: String,
    libraryViewModel: LibraryViewModel,
    playlistViewModel: PlaylistViewModel? = null,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val songs = (uiState as? LibraryUiState.Loaded)?.songs?.filter {
        it.filePath != null && (
            java.io.File(it.filePath).parentFile?.absolutePath == folderPath ||
            java.io.File(it.filePath).parent == folderPath ||
            java.io.File(it.filePath).parentFile?.name == folderPath
        )
    }.orEmpty()

    val folderName = java.io.File(folderPath).name.ifBlank { folderPath }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folderCoverRepository = remember { FolderCoverRepository(context) }
    val folderCoverStorage = remember { FolderCoverStorage(context) }
    val folderCovers by folderCoverRepository.folderCovers.collectAsStateWithLifecycle(initialValue = emptyMap())
    val customCoverPath = folderCovers[folderPath]
    val coverPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val newPath = folderCoverStorage.save(folderPath, uri) ?: return@launch
                try {
                    folderCoverRepository.setCover(folderPath, newPath)
                } catch (e: Throwable) {
                    folderCoverStorage.delete(newPath)
                    throw e
                }
                if (customCoverPath != null && customCoverPath != newPath) {
                    folderCoverStorage.delete(customCoverPath)
                }
            }
        }
    }
    var songPendingConsentRetry by remember { mutableStateOf<Song?>(null) }
    val deleteRecoveryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            songPendingConsentRetry?.let { retryDeleteAfterConsent(context, it) }
        }
        songPendingConsentRetry = null
        libraryViewModel.refresh()
    }

    LibraryGroupDetailScreen(
        title = folderName,
        subtitle = if (songs.size == 1) "1 song" else "${songs.size} songs",
        wikipediaQuery = null,
        artistForHero = null,
        albumHeroSong = null,
        folderHeroPath = folderPath,
        folderCustomCoverPath = customCoverPath,
        onChangeFolderCover = { coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onClearFolderCover = {
            scope.launch {
                folderCoverRepository.clearCover(folderPath)
                customCoverPath?.let { folderCoverStorage.delete(it) }
            }
        },
        playlistViewModel = playlistViewModel,
        metadata = emptyList(),
        songs = songs,
        playbackViewModel = playbackViewModel,
        onBack = onBack,
        onNavigateToNowPlaying = onNavigateToNowPlaying,
        onUpdateSong = { libraryViewModel.updateSongMetadata(it) },
        modifier = modifier,
        listBottomPadding = listBottomPadding,
        onDeleteSong = { song ->
            deleteSongFile(context, song, deleteRecoveryLauncher) { pending -> songPendingConsentRetry = pending }
            libraryViewModel.refresh()
        }
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun LibraryGroupDetailScreen(
    title: String,
    subtitle: String?,
    wikipediaQuery: String?,
    artistForHero: String?,
    albumHeroSong: Song?,
    metadata: List<Pair<String, String>>,
    songs: List<Song>,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    folderHeroPath: String? = null,
    folderCustomCoverPath: String? = null,
    onChangeFolderCover: () -> Unit = {},
    onClearFolderCover: () -> Unit = {},
    wikipediaAlbumArtist: String? = null,
    playlistViewModel: PlaylistViewModel? = null,
    listBottomPadding: Dp = 0.dp,
    onDeleteSong: ((Song) -> Unit)? = null,
    onUpdateSong: ((Song) -> Unit)? = null
) {
    val context = LocalContext.current
    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val playlists = playlistViewModel?.playlists?.collectAsStateWithLifecycle()?.value.orEmpty()
    val loader = remember { WikipediaInfoLoader() }
    val settingsRepo = remember { ServiceSettingsRepository(context) }
    var about by remember(wikipediaQuery) { mutableStateOf<String?>(null) }
    var showAlbumActions by remember { mutableStateOf(false) }
    var showArtworkViewer by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var songForOptions by remember { mutableStateOf<Song?>(null) }
    var songForTagEdit by remember { mutableStateOf<Song?>(null) }
    var coverSearchSong by remember { mutableStateOf<Song?>(null) }
    var songPendingDelete by remember { mutableStateOf<Song?>(null) }
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()

    var songFilter by remember { mutableStateOf(SongFilter.ALL) }
    var sortOrder by remember { mutableStateOf(SongSortOrder.TITLE) }
    var sortDirection by remember { mutableStateOf(SortDirection.ASCENDING) }
    var showRefineSheet by remember { mutableStateOf(false) }
    val isRefined = folderHeroPath != null && (songFilter != SongFilter.ALL || sortOrder != SongSortOrder.TITLE || sortDirection != SortDirection.ASCENDING)

    val favoritesRepository = remember { FavoritesRepository(context) }
    val favoriteIds by favoritesRepository.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val downloads by downloadRepository.downloads.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val albumCoverRepo = remember { AlbumCoverRepository.getInstance(context) }
    val customCovers by albumCoverRepo.albumCovers.collectAsStateWithLifecycle()

    // Sort/filter only apply inside a folder's track list; every other detail screen keeps its natural order.
    val songs = remember(songs, folderHeroPath, songFilter, sortOrder, sortDirection, favoriteIds, downloads) {
        if (folderHeroPath == null) songs
        else {
            val filtered = when (songFilter) {
                SongFilter.ALL -> songs
                SongFilter.FAVORITES -> songs.filter { it.isFavorite || it.id in favoriteIds }
                SongFilter.DOWNLOADED -> songs.filter {
                    it.source == MusicSource.Local || downloads[it.id]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
                }
                SongFilter.RECENTLY_ADDED -> songs.sortedByDescending { it.dateAddedMs }
            }
            if (songFilter == SongFilter.RECENTLY_ADDED) filtered
            else {
                val comparator = sortOrder.comparator().let { if (sortDirection == SortDirection.DESCENDING) it.reversed() else it }
                filtered.sortedWith(comparator)
            }
        }
    }
    val downloadableSongs = remember(songs) { songs.filter { it.source != MusicSource.Local } }

    LaunchedEffect(wikipediaQuery) {
        val settings = settingsRepo.settings.first()
        about = wikipediaQuery
            ?.takeIf { settings.isEnabled(ExternalService.WIKIPEDIA) }
            ?.let { query ->
                if (wikipediaAlbumArtist != null) loader.fetchAlbumSummary(query, wikipediaAlbumArtist)
                else loader.fetchSummary(query)
            }
    }

    var atmospherePalette by remember(albumHeroSong?.id) { mutableStateOf<ArtworkAtmospherePalette?>(null) }
    LaunchedEffect(albumHeroSong?.id) {
        val hero = albumHeroSong ?: return@LaunchedEffect
        val bitmap = withContext(Dispatchers.IO) { loadSongArtwork(context, hero) }
        if (bitmap != null) {
            atmospherePalette = withContext(Dispatchers.Default) { extractArtworkAtmospherePalette(bitmap) }
        }
    }
    val atmosphereColor = if (albumHeroSong != null) (atmospherePalette?.darkBase ?: MaterialTheme.colorScheme.surface) else MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxSize().background(atmosphereColor)) {
        val playAll = {
            if (songs.isNotEmpty()) {
                val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                playbackViewModel.playQueue(songs, 0)
                if (wasIdle) onNavigateToNowPlaying()
            }
        }
        val shuffleAll = {
            if (songs.isNotEmpty()) {
                val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                playbackViewModel.shuffleAll(songs)
                if (wasIdle) onNavigateToNowPlaying()
            }
        }
        val playNext = {
            if (songs.isNotEmpty()) {
                if (playbackViewModel.playbackState.value.currentSong == null) playAll()
                else playbackViewModel.addToUpNext(songs)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = listBottomPadding)
        ) {
            item(key = "header", contentType = "header") {
                if (artistForHero != null) {
                    ArtistDetailHeader(
                        artist = artistForHero,
                        songCount = songs.size,
                        albumCount = songs.map { it.albumId }.distinct().size,
                        canDownload = downloadableSongs.isNotEmpty(),
                        onBack = onBack,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                        onDownloadAll = { downloadableSongs.forEach(downloadRepository::download) }
                    )
                } else if (folderHeroPath != null) {
                    FolderDetailHeader(
                        folderName = title,
                        folderPath = folderHeroPath,
                        songCount = songs.size,
                        sampleSong = songs.firstOrNull(),
                        customCoverPath = folderCustomCoverPath,
                        canDownload = downloadableSongs.isNotEmpty(),
                        onBack = onBack,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                        onDownloadAll = { downloadableSongs.forEach(downloadRepository::download) },
                        onChangeCover = onChangeFolderCover,
                        onClearCover = onClearFolderCover
                    )
                } else if (albumHeroSong != null) {
                    AlbumDetailHeader(
                        song = albumHeroSong,
                        songCount = songs.size,
                        totalDurationMs = songs.sumOf { it.durationMs },
                        canDownload = downloadableSongs.isNotEmpty(),
                        atmosphereColor = atmosphereColor,
                        onBack = onBack,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                        onDownloadAll = { downloadableSongs.forEach(downloadRepository::download) },
                        onMore = { showAlbumActions = true },
                        onViewArtwork = { showArtworkViewer = true },
                        onChangeCover = { coverSearchSong = albumHeroSong }
                    )
                } else {
                    GroupDetailHeader(
                        title = title,
                        subtitle = subtitle,
                        songCount = songs.size,
                        canDownload = downloadableSongs.isNotEmpty(),
                        onBack = onBack,
                        onDownloadAll = { downloadableSongs.forEach(downloadRepository::download) }
                    )
                }
            }
            if (songs.isNotEmpty()) {
                item(contentType = "songs_header") {
                    SongSectionHeader(
                        title = if (albumHeroSong != null) "Tracks" else if (artistForHero != null) "Popular Songs" else "Tracks",
                        songs = songs,
                        onPlayAll = if (albumHeroSong == null && artistForHero == null && folderHeroPath == null) playAll else null,
                        onShuffleAll = if (albumHeroSong == null && artistForHero == null && folderHeroPath == null) shuffleAll else null,
                        onRefine = if (folderHeroPath != null) { { showRefineSheet = true } } else null,
                        isRefined = isRefined
                    )
                }
            }
            items(songs, key = { it.id }, contentType = { "song" }) { song ->
                val isCurrentSong = playbackState.currentSong?.id == song.id
                val isPlaying = playbackState.isPlaying
                val onSongClick = {
                    val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                    val index = songs.indexOfFirst { it.id == song.id }
                    playbackViewModel.playQueue(songs, index)
                    if (wasIdle) onNavigateToNowPlaying()
                }
                SongRow(
                    song = song,
                    isCurrentSong = isCurrentSong,
                    isPlaying = isPlaying,
                    onMoreClick = { songForOptions = song },
                    onClick = onSongClick
                )
            }
            if (metadata.isNotEmpty()) {
                item(contentType = "metadata") { MusicMetadataBlock(metadata) }
            }
            about?.let { aboutText ->
                item(contentType = "about") { WikipediaAboutBlock(aboutText) }
            }
        }
        if (showAlbumActions) {
            val hasAlbumCustomCover = albumHeroSong?.let {
                albumCoverRepo.getCoverPath(it.album, it.artist, it.albumId) != null
            } ?: false
            AlbumActionsSheet(
                onPlayNext = { playNext(); showAlbumActions = false },
                onAddToPlaylist = { showAlbumActions = false; showPlaylistPicker = true },
                onDownload = { downloadableSongs.forEach(downloadRepository::download); showAlbumActions = false },
                onShare = { shareSongs(context, songs); showAlbumActions = false },
                onViewArtwork = if (albumHeroSong != null) {
                    { showAlbumActions = false; showArtworkViewer = true }
                } else null,
                onSearchCoverOnline = if (albumHeroSong != null) {
                    { showAlbumActions = false; coverSearchSong = albumHeroSong }
                } else null,
                onRemoveCustomCover = if (hasAlbumCustomCover) {
                    {
                        showAlbumActions = false
                        val hero = albumHeroSong
                        coroutineScope.launch {
                            val path = albumCoverRepo.getCoverPath(hero.album, hero.artist, hero.albumId)
                            albumCoverRepo.clearCover(hero.album, hero.artist, hero.albumId)
                            if (path != null) {
                                AlbumCoverStorage(context).delete(path)
                            }
                            invalidateAlbumArtCache(context)
                        }
                    }
                } else null,
                onDismiss = { showAlbumActions = false }
            )
        }
        if (showArtworkViewer && albumHeroSong != null) {
            FullScreenArtworkSheet(
                song = albumHeroSong,
                onDismiss = { showArtworkViewer = false },
                onSearchCoverOnline = {
                    showArtworkViewer = false
                    coverSearchSong = albumHeroSong
                }
            )
        }
        if (showPlaylistPicker) {
            PlaylistPickerSheet(
                playlists = playlists,
                onSelect = { playlist ->
                    val idsToAdd = if (songForOptions != null) listOf(songForOptions!!.id) else songs.map { it.id }
                    playlistViewModel?.addSongs(playlist.id, idsToAdd)
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
            val hasSongCustomCover = albumCoverRepo.getCoverPath(song.album, song.artist, song.albumId) != null

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
                onSearchCoverOnline = {
                    val targetSong = song
                    songForOptions = null
                    coverSearchSong = targetSong
                },
                onRemoveCustomCover = if (hasSongCustomCover) {
                    {
                        val targetSong = song
                        songForOptions = null
                        coroutineScope.launch {
                            val path = albumCoverRepo.getCoverPath(targetSong.album, targetSong.artist, targetSong.albumId)
                            albumCoverRepo.clearCover(targetSong.album, targetSong.artist, targetSong.albumId)
                            if (path != null) {
                                AlbumCoverStorage(context).delete(path)
                            }
                            invalidateAlbumArtCache(context)
                        }
                    }
                } else null,
                onDelete = if (onDeleteSong != null) {
                    { songPendingDelete = song; songForOptions = null }
                } else null,
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
                    onUpdateSong?.invoke(updated)
                    playbackViewModel.updateSongMetadata(updated)
                    songForTagEdit = null
                }
            )
        }
        if (folderHeroPath != null && showRefineSheet) {
            LibraryRefineSheet(
                filter = songFilter,
                onFilterSelect = { songFilter = it },
                sortOptions = SongSortOrder.entries,
                sortLabelOf = { it.label },
                selectedSort = sortOrder,
                sortDirection = sortDirection,
                onSortSelect = { order ->
                    if (order == sortOrder) sortDirection = sortDirection.flipped() else {
                        sortOrder = order
                        sortDirection = SortDirection.ASCENDING
                    }
                },
                onDismiss = { showRefineSheet = false }
            )
        }
        coverSearchSong?.let { targetSong ->
            OnlineAlbumCoverSearchSheet(
                initialAlbum = targetSong.album,
                initialArtist = targetSong.artist,
                albumId = targetSong.albumId,
                onDismiss = { coverSearchSong = null }
            )
        }
        songPendingDelete?.let { song ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { songPendingDelete = null },
                title = { Text("Delete \"${song.title}\"?") },
                text = { Text("This permanently removes the file from your device. This can't be undone.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        onDeleteSong?.invoke(song)
                        songPendingDelete = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { songPendingDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun ArtistDetailHeader(
    artist: String,
    songCount: Int,
    albumCount: Int,
    canDownload: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onDownloadAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            ArtistImage(
                artist = artist,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                thumbnail = false
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        )
                    )
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
                Spacer(Modifier.weight(1f))
                if (canDownload) {
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_download,
                        contentDescription = "Download all songs",
                        onClick = onDownloadAll
                    )
                }
            }
            Text(
                artist,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp, vertical = 18.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$songCount ${if (songCount == 1) "song" else "songs"} · $albumCount ${if (albumCount == 1) "album" else "albums"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 48.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = onPlayAll,
                modifier = Modifier.padding(end = 10.dp)
            )
            TransportButton(
                iconRes = R.drawable.lucide_ic_shuffle,
                size = 48.dp,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onShuffleAll
            )
        }
    }
}

@Composable
private fun ArtistSongRow(
    song: Song,
    trackIndex: Int,
    onClick: () -> Unit
) {
    val durationText = remember(song.durationMs) {
        if (song.durationMs > 0) {
            val totalSeconds = song.durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "%d:%02d".format(minutes, seconds)
        } else ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(start = 24.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "%02d".format(trackIndex),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.width(28.dp)
            )
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))) {
                AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(song.album.takeIf { it.isNotEmpty() && it != song.title }, song.releaseYear?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.source != MusicSource.Local) {
                    val (sourceLabel, sourceColor) = when (song.source) {
                        MusicSource.Subsonic -> "Subsonic" to Color(0xFFFF8500)
                        MusicSource.Jellyfin -> "Jellyfin" to Color(0xFF00A4DC)
                        MusicSource.Radio -> "Radio" to MaterialTheme.colorScheme.tertiary
                        MusicSource.Local, MusicSource.ListeningRoomHost -> "" to Color.Unspecified
                    }
                    if (sourceLabel.isNotEmpty()) {
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 9.sp),
                            color = sourceColor,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(sourceColor.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (durationText.isNotEmpty()) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            DownloadStatusIcon(song = song, modifier = Modifier.padding(start = 8.dp))
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            modifier = Modifier.padding(start = 90.dp)
        )
    }
}

@Composable
private fun AlbumDetailHeader(
    song: Song,
    songCount: Int,
    totalDurationMs: Long = 0L,
    canDownload: Boolean,
    atmosphereColor: Color = MaterialTheme.colorScheme.surface,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onDownloadAll: () -> Unit,
    onMore: () -> Unit,
    onViewArtwork: (() -> Unit)? = null,
    onChangeCover: (() -> Unit)? = null
) {
    val durationText = if (totalDurationMs > 0) {
        val totalSeconds = totalDurationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
    } else ""

    val genres = song.genres.take(1).joinToString("")
    val releaseDetails = listOfNotNull(
        genres.takeIf { it.isNotBlank() },
        song.releaseYear?.toString(),
        "$songCount ${if (songCount == 1) "track" else "tracks"}",
        durationText.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            if (canDownload) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_download,
                    contentDescription = "Download album",
                    onClick = onDownloadAll,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_ellipsis_vertical,
                contentDescription = "Album actions",
                onClick = onMore,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 16.dp)
                .size(240.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.7f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(if (onViewArtwork != null) Modifier.tapScale(onViewArtwork) else Modifier)
        ) {
            AlbumArt(
                song = song,
                modifier = Modifier.fillMaxSize(),
                thumbnail = false
            )
            if (onChangeCover != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .tapScale(onChangeCover),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_image),
                        contentDescription = "Search album cover online",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Text(
            text = song.album,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        if (releaseDetails.isNotBlank() || song.source != MusicSource.Local) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (song.source != MusicSource.Local) {
                    val (sourceLabel, sourceColor) = when (song.source) {
                        MusicSource.Subsonic -> "Subsonic" to Color(0xFFFF8500)
                        MusicSource.Jellyfin -> "Jellyfin" to Color(0xFF00A4DC)
                        MusicSource.Radio -> "Radio" to MaterialTheme.colorScheme.tertiary
                        MusicSource.Local, MusicSource.ListeningRoomHost -> "" to Color.Unspecified
                    }
                    if (sourceLabel.isNotEmpty()) {
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 10.sp),
                            color = sourceColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(sourceColor.copy(alpha = 0.14f))
                                .border(1.dp, sourceColor.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (releaseDetails.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            releaseDetails,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        CatalogPlaybackActions(
            onPlay = onPlayAll,
            onShuffle = onShuffleAll
        )
    }
}

@Composable
private fun AlbumActionsSheet(
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onViewArtwork: (() -> Unit)? = null,
    onSearchCoverOnline: (() -> Unit)? = null,
    onRemoveCustomCover: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Album Options",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AlbumActionRow(R.drawable.lucide_ic_play, "Play Next", "Add album tracks next in queue", onPlayNext)
                AlbumActionRow(R.drawable.lucide_ic_list_music, "Add to Playlist", "Add all tracks to a playlist", onAddToPlaylist)
                AlbumActionRow(R.drawable.lucide_ic_download, "Download Album", "Save album for offline playback", onDownload)
                AlbumActionRow(R.drawable.lucide_ic_share_2, "Share Album", "Share album link or info", onShare)
                if (onViewArtwork != null) {
                    AlbumActionRow(R.drawable.lucide_ic_maximize_2, "View Full Artwork", "Inspect cover art in full size", onViewArtwork)
                }
                if (onSearchCoverOnline != null) {
                    AlbumActionRow(R.drawable.lucide_ic_image, "Search Cover Online", "Find and apply high-resolution artwork", onSearchCoverOnline)
                }
                if (onRemoveCustomCover != null) {
                    AlbumActionRow(R.drawable.lucide_ic_rotate_ccw, "Reset to Original Cover", "Restore default embedded artwork", onRemoveCustomCover)
                }
            }
        }
    }
}

@Composable
private fun AlbumActionRow(
    iconRes: Int,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .tapScale(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.lucide_ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun FolderDetailHeader(
    folderName: String,
    folderPath: String,
    songCount: Int,
    sampleSong: Song?,
    canDownload: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onDownloadAll: () -> Unit,
    customCoverPath: String? = null,
    onChangeCover: () -> Unit = {},
    onClearCover: () -> Unit = {}
) {
    val customCover = rememberLocalFileCoverBitmap(customCoverPath)
    val displayPath = folderPath
        .replace("/storage/emulated/0", "~")
        .replace("/storage/emulated/legacy", "~")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
            Spacer(Modifier.weight(1f))
            if (canDownload) {
                PressDepthIconButton(R.drawable.lucide_ic_download, "Download folder", onDownloadAll)
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            val hasArt = customCover != null || sampleSong != null
            if (customCover != null) {
                Image(
                    bitmap = customCover.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (sampleSong != null) {
                AlbumArt(song = sampleSong, modifier = Modifier.fillMaxSize())
            }
            if (hasArt) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to Color.Black.copy(alpha = 0.45f),
                                1f to Color.Black.copy(alpha = 0.85f)
                            )
                        )
                )
            }
            if (customCover == null) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_folder),
                    contentDescription = null,
                    tint = if (hasArt) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(56.dp)
                )
            }
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (customCoverPath != null) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_trash_2),
                        contentDescription = "Remove folder cover",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(3.dp)
                            .tapScale(onClearCover)
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_image_plus),
                    contentDescription = "Change folder cover",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(3.dp)
                        .tapScale(onChangeCover)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = folderName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Text(
            text = displayPath,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
        )

        Text(
            text = if (songCount == 1) "1 track" else "$songCount tracks",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
        )

        CatalogPlaybackActions(
            onPlay = onPlayAll,
            onShuffle = onShuffleAll,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
private fun GroupDetailHeader(
    title: String,
    subtitle: String?,
    songCount: Int,
    canDownload: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit = {},
    onShuffleAll: () -> Unit = {},
    onDownloadAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (songCount == 1) "1 song" else "$songCount songs",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (canDownload) {
            PressDepthIconButton(R.drawable.lucide_ic_download, "Download all songs", onDownloadAll)
        }
    }
}

@Composable
private fun SongSectionHeader(
    title: String,
    songs: List<Song> = emptyList(),
    onPlayAll: (() -> Unit)? = null,
    onShuffleAll: (() -> Unit)? = null,
    onRefine: (() -> Unit)? = null,
    isRefined: Boolean = false
) {
    val totalDurationText = remember(songs) {
        if (songs.isEmpty()) "" else {
            val totalMs = songs.sumOf { it.durationMs }
            val totalMinutes = totalMs / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (hours > 0) "${songs.size} tracks • ${hours}h ${minutes}m" else "${songs.size} tracks • ${minutes} min"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            if (totalDurationText.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    totalDurationText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onPlayAll != null && onShuffleAll != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportButton(
                    iconRes = R.drawable.lucide_ic_play,
                    size = 42.dp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    accented = true,
                    onClick = onPlayAll
                )
                TransportButton(
                    iconRes = R.drawable.lucide_ic_shuffle,
                    size = 42.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = onShuffleAll
                )
            }
        } else if (onRefine != null) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_sliders_horizontal,
                contentDescription = "Refine tracks",
                tint = if (isRefined) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onRefine
            )
        }
    }
}

@Composable
private fun MusicMetadataBlock(metadata: List<Pair<String, String>>) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            "DETAILS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        metadata.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun WikipediaAboutBlock(aboutText: String) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .animateContentSize()
    ) {
        Text(
            "ABOUT",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            aboutText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (aboutText.length > 160 || aboutText.lines().size > 2) {
            Text(
                text = if (expanded) "Show less" else "Read more",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { expanded = !expanded }
            )
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_book_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Wikipedia", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Description source / CC BY-SA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
