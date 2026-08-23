package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.ArtistBiography
import `in`.caffeinelabs.cassettecat.data.library.WikipediaInfoLoader
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.DownloadStatusIcon
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.shareSongs
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.flow.first

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
    val songs = (uiState as? LibraryUiState.Loaded)?.songs?.filter { artist in it.artist.splitArtists() }.orEmpty()

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
            wikipediaEnabled = settings.wikipediaEnabled,
            audioDbEnabled = settings.audioDbEnabled
        )
    }
    val albums = remember(songs) {
        songs.groupBy { it.albumId }
            .map { (_, tracks) -> ArtistAlbum(tracks.first(), tracks) }
            .sortedByDescending { it.cover.releaseYear ?: 0 }
    }
    // A release belongs to exactly one shelf. The old `drop(6).ifEmpty { albums }`
    // fallback repeated the same albums under a second heading.
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
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val heroHeightPx = with(density) { 430.dp.toPx() }
    val compactHeaderProgress by remember(listState, heroHeightPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else {
                // Keep the hero in charge for the first third of its travel. The compact
                // header then fades over the rest of that travel instead of arriving abruptly.
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
            playbackViewModel.playQueue(songs.shuffled(), 0)
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
            item(key = "hero") {
                ArtistCatalogHero(
                    artist = artist,
                    songCount = songs.size,
                    albumCount = albums.size,
                    collapseProgress = compactHeaderProgress,
                    onBack = onBack,
                    onPlay = ::playAll,
                    onShuffle = ::shuffleAll
                )
            }
            if (recentTracks.isNotEmpty()) {
                item(key = "recent-title") { ArtistSectionTitle("Recently added") }
                items(recentTracks, key = { "recent:${it.id}" }) { song -> ArtistSongRow(song = song, onClick = { play(song) }) }
            }
            if (albumsOnly.isNotEmpty()) {
                item(key = "albums") {
                    ArtistAlbumShelf(
                        title = "Albums",
                        albums = albumsOnly,
                        onAlbumClick = { onNavigateToAlbum(it.cover.albumId) }
                    )
                }
            }
            if (singles.isNotEmpty()) {
                item(key = "singles") {
                    ArtistAlbumShelf(
                        title = "Singles & EPs",
                        albums = singles,
                        onAlbumClick = { onNavigateToAlbum(it.cover.albumId) }
                    )
                }
            }
            if (appearsOn.isNotEmpty()) {
                item(key = "appears") {
                    ArtistAlbumShelf(
                        title = "Appears On",
                        albums = appearsOn,
                        onAlbumClick = { onNavigateToAlbum(it.cover.albumId) }
                    )
                }
            }
            // About is intentionally final: the library/catalog content comes first.
            about?.let { biography ->
                item(key = "about") { ArtistAboutSection(biography) }
            }
        }
        // The compact title is not a second header. It fades in only as the hero title leaves,
        // while its surface gradually picks up opacity from the same scroll progress.
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
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
        Text("About", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = biography.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_book_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                biography.source,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArtistCatalogHero(
    artist: String,
    songCount: Int,
    albumCount: Int,
    collapseProgress: Float,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(430.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        ArtistImage(artist = artist, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f))
            )
        )
        Text(
            artist,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .graphicsLayer { alpha = (1f - ((collapseProgress - 0.18f) / 0.58f)).coerceIn(0f, 1f) }
        )
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
        Text(
            "$songCount songs · $albumCount albums",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 8.dp)
                .graphicsLayer { alpha = (1f - ((collapseProgress - 0.1f) / 0.6f)).coerceIn(0f, 1f) }
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            items(albums, key = { it.cover.albumId }) { album ->
                Column(modifier = Modifier.width(176.dp).tapScale { onAlbumClick(album) }) {
                    Box(modifier = Modifier.size(176.dp).clip(RoundedCornerShape(12.dp))) {
                        AlbumArt(song = album.cover, modifier = Modifier.fillMaxSize())
                    }
                    Text(album.cover.album, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text(album.cover.releaseYear?.toString() ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val songs = (uiState as? LibraryUiState.Loaded)?.songs?.filter { it.albumId == albumId }.orEmpty()

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
    val songs = (uiState as? LibraryUiState.Loaded)?.songs?.filter { genre in it.effectiveGenres() }.orEmpty()

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
        modifier = modifier,
        listBottomPadding = listBottomPadding
    )
}

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
    wikipediaAlbumArtist: String? = null,
    playlistViewModel: PlaylistViewModel? = null,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val downloadableSongs = remember(songs) { songs.filter { it.source != MusicSource.Local } }
    val playlists = playlistViewModel?.playlists?.collectAsStateWithLifecycle()?.value.orEmpty()
    val loader = remember { WikipediaInfoLoader() }
    val settingsRepo = remember { ServiceSettingsRepository(context) }
    var about by remember(wikipediaQuery) { mutableStateOf<String?>(null) }
    var showAlbumActions by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    LaunchedEffect(wikipediaQuery) {
        val settings = settingsRepo.settings.first()
        about = wikipediaQuery
            ?.takeIf { settings.wikipediaEnabled }
            ?.let { query ->
                if (wikipediaAlbumArtist != null) loader.fetchAlbumSummary(query, wikipediaAlbumArtist)
                else loader.fetchSummary(query)
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
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
                playbackViewModel.playQueue(songs.shuffled(), 0)
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
            // The complete detail header belongs to the scroll content. This lets artwork,
            // actions, and metadata move naturally out of the way as the song list takes over.
            item(key = "header") {
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
                } else if (albumHeroSong != null) {
                    AlbumDetailHeader(
                        song = albumHeroSong,
                        songCount = songs.size,
                        canDownload = downloadableSongs.isNotEmpty(),
                        onBack = onBack,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                        onDownloadAll = { downloadableSongs.forEach(downloadRepository::download) },
                        onMore = { showAlbumActions = true }
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
            if (metadata.isNotEmpty()) {
                item { MusicMetadataBlock(metadata) }
            }
            about?.let { aboutText ->
                item { WikipediaAboutBlock(aboutText) }
            }
            if ((artistForHero != null || albumHeroSong != null) && songs.isNotEmpty()) {
                item { SongSectionHeader(if (albumHeroSong != null) "Tracks" else "All songs") }
            }
            items(songs, key = { it.id }) { song ->
                val onSongClick = {
                    val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                    val index = songs.indexOfFirst { it.id == song.id }
                    playbackViewModel.playQueue(songs, index)
                    if (wasIdle) onNavigateToNowPlaying()
                }
                if (artistForHero != null) {
                    ArtistSongRow(song = song, onClick = onSongClick)
                } else {
                    SongRow(song = song, onClick = onSongClick)
                }
            }
        }
        if (showAlbumActions) {
            AlbumActionsSheet(
                onPlayNext = { playNext(); showAlbumActions = false },
                onAddToPlaylist = { showAlbumActions = false; showPlaylistPicker = true },
                onDownload = { downloadableSongs.forEach(downloadRepository::download); showAlbumActions = false },
                onShare = { shareSongs(context, songs); showAlbumActions = false },
                onDismiss = { showAlbumActions = false }
            )
        }
        if (showPlaylistPicker) {
            PlaylistPickerSheet(
                playlists = playlists,
                onSelect = { playlist ->
                    playlistViewModel?.addSongs(playlist.id, songs.map { it.id })
                    showPlaylistPicker = false
                },
                onDismiss = { showPlaylistPicker = false }
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
            // Artist photography is often portrait or square. Fit preserves the full source
            // instead of cutting off faces at the edges; the surface behind it fills the hero.
            ArtistImage(
                artist = artist,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
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
private fun ArtistSongRow(song: Song, onClick: () -> Unit) {
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
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) {
                AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(song.artist, song.releaseYear?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DownloadStatusIcon(song = song, modifier = Modifier.padding(start = 12.dp))
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            modifier = Modifier.padding(start = 72.dp)
        )
    }
}

@Composable
private fun AlbumDetailHeader(
    song: Song,
    songCount: Int,
    canDownload: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onDownloadAll: () -> Unit,
    onMore: () -> Unit
) {
    val genres = song.genres.take(2).joinToString(", ")
    val releaseDetails = listOfNotNull(
        genres.takeIf { it.isNotBlank() },
        song.releaseYear?.toString(),
        "$songCount ${if (songCount == 1) "track" else "tracks"}"
    ).joinToString(" / ")

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
                PressDepthIconButton(R.drawable.lucide_ic_download, "Download album", onDownloadAll)
            }
            PressDepthIconButton(R.drawable.lucide_ic_ellipsis_vertical, "Album actions", onMore)
        }
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(232.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        }
        Text(
            song.album,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 18.dp)
        )
        Text(
            song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
        )
        if (releaseDetails.isNotBlank()) {
            Text(
                releaseDetails,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp)
            )
        }
        CatalogPlaybackActions(
            onPlay = onPlayAll,
            onShuffle = onShuffleAll,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumActionsSheet(
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            AlbumActionRow(R.drawable.lucide_ic_play, "Play Next", onPlayNext)
            AlbumActionRow(R.drawable.lucide_ic_list_music, "Add to Playlist", onAddToPlaylist)
            AlbumActionRow(R.drawable.lucide_ic_download, "Download Album", onDownload)
            AlbumActionRow(R.drawable.lucide_ic_share_2, "Share Album", onShare)
        }
    }
}

@Composable
private fun AlbumActionRow(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().tapScale(onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GroupDetailHeader(
    title: String,
    subtitle: String?,
    songCount: Int,
    canDownload: Boolean,
    onBack: () -> Unit,
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
private fun SongSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp)
    )
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
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            "ABOUT",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            aboutText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_book_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Wikipedia", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Description source / CC BY-SA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
