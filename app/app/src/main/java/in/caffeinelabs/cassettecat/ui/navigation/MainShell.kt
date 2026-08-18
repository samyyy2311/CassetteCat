package `in`.caffeinelabs.cassettecat.ui.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.ui.components.MiniPlayerRow
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.home.HomeScreen
import `in`.caffeinelabs.cassettecat.ui.screens.library.AlbumDetailScreen
import `in`.caffeinelabs.cassettecat.ui.screens.library.ArtistDetailScreen
import `in`.caffeinelabs.cassettecat.ui.screens.library.GenreDetailScreen
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryScreen
import `in`.caffeinelabs.cassettecat.ui.screens.library.LikedSongsScreen
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.PlaylistDetailScreen
import `in`.caffeinelabs.cassettecat.ui.screens.library.PlaylistViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.NowPlayingContent
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.NowPlayingView
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PairingScreen
import `in`.caffeinelabs.cassettecat.ui.screens.search.SearchScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.BackupRestoreScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ConnectServerScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CreditsScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.DownloadsScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.EqualizerScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ExternalServicesScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ManageScanFoldersScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.PlaybackPreferencesScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.PrivacyScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ScrobbleSettingsScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.SettingsScreen
import `in`.caffeinelabs.cassettecat.ui.screens.stats.StatsScreen
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

object MainRoute {
    const val HOME = "main/home"
    const val LIBRARY = "main/library"
    const val SEARCH = "main/search"
    const val SETTINGS = "main/settings"
    const val CONNECT_SERVER = "main/connect/{protocol}"
    fun connectServer(protocol: StreamingProtocol) = "main/connect/${protocol.name}"
    const val ARTIST_DETAIL = "main/library/artist/{artist}"
    fun artistDetail(artist: String) = "main/library/artist/${Uri.encode(artist)}"
    const val ALBUM_DETAIL = "main/library/album/{albumId}"
    fun albumDetail(albumId: String) = "main/library/album/${Uri.encode(albumId)}"
    const val GENRE_DETAIL = "main/library/genre/{genre}"
    fun genreDetail(genre: String) = "main/library/genre/${Uri.encode(genre)}"
    const val PLAYLIST_DETAIL = "main/library/playlist/{playlistId}"
    fun playlistDetail(playlistId: String) = "main/library/playlist/${Uri.encode(playlistId)}"
    const val LIKED_SONGS = "main/library/liked"
    const val STATS = "main/stats"
    const val MANAGE_SCAN_FOLDERS = "main/settings/scan_folders"
    const val EXTERNAL_SERVICES = "main/settings/external_services"
    const val EQUALIZER = "main/settings/equalizer"
    const val BACKUP_RESTORE = "main/settings/backup_restore"
    const val DOWNLOADS = "main/settings/downloads"
    const val SLEEP_TIMER = "main/settings/sleep_timer"
    const val PRIVACY = "main/settings/privacy"
    const val COMPANION_DEVICE = "main/settings/companion"
    const val CREDITS = "main/settings/credits"
    const val SCROBBLING = "main/settings/scrobbling"
}

private val MINI_PLAYER_HEIGHT = 64.dp
private val SHEET_CORNER_RADIUS = 28.dp
// The bottom chrome is one continuous 64dp surface. Keeping this in sync with BottomNavBar
// prevents a one-pixel strip of the page showing between the mini player and navigation.
private val NAV_BAR_TOTAL_HEIGHT = 68.dp

// Now Playing is the sheet's expanded state, not a nav destination.
// nav bar is a fixed overlay, not a layout sibling (avoids a stutter-causing measurement loop)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(playbackViewModel: PlaybackViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    fun navigateToTab(route: String) {
        val activeParent = parentTabRoute(currentRoute)
        if (activeParent == route) {
            if (currentRoute != route) {
                navController.popBackStack(route, inclusive = false)
            }
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    // shared across Library/Home/Search to avoid redundant refetches
    val libraryViewModel: LibraryViewModel = viewModel()
    val playlistViewModel: PlaylistViewModel = viewModel()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    LaunchedEffect(libraryState) {
        (libraryState as? LibraryUiState.Loaded)?.let { playbackViewModel.restoreIfNeeded(it.songs) }
    }
    // Artist art deliberately extends behind the transparent status bar. Other routes retain
    // their normal safe-area layout so ordinary screen headers are never pushed under system UI.
    val artistHeroRoute = currentRoute == MainRoute.ARTIST_DETAIL
    // artist/album/playlist detail are playback-adjacent, unlike CONNECT_SERVER, so chrome stays visible
    val showChrome = currentRoute != null && currentRoute != MainRoute.CONNECT_SERVER
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val hasSong = playbackState.currentSong != null
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // The custom bottom bar is an app overlay, so it must explicitly yield to the IME.
    // Otherwise its labels are left hovering below/over the keyboard on text-entry screens.
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    val navBarReservation = if (showChrome && !imeVisible) NAV_BAR_TOTAL_HEIGHT else 0.dp
    // fixed height, not animated, to avoid a feedback loop with `fraction`
    val peekHeight = if (showChrome) navBarReservation + (if (hasSong) MINI_PLAYER_HEIGHT else 0.dp) else 0.dp
    val scaffoldState = rememberBottomSheetScaffoldState()

    var fraction by remember { mutableStateOf(0f) }
    // State, not by: per-frame drag updates shouldn't recompose MainShell
    val collapsedArtRect = remember { mutableStateOf<Rect?>(null) }
    // hoisted so sheetSwipeEnabled can turn off during Queue/Lyrics
    var nowPlayingView by remember { mutableStateOf(NowPlayingView.PLAYER) }
    var searchFocusRequestId by remember { mutableIntStateOf(0) }

    // fades the sheet fill in Queue/Lyrics mode, where the real sheet doesn't move
    var headerDragRevealFraction by remember { mutableStateOf(0f) }

    // without this, back gesture falls through and closes the app
    BackHandler(enabled = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
    }

    // resets sub-view on collapse so re-expanding lands on the art
    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        if (scaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
            nowPlayingView = NowPlayingView.PLAYER
        }
    }

    Box(modifier.fillMaxSize()) {
        // The library and sheet remain within the system safe area. Lyrics is added below as
        // a root-level layer so it can use the entire display without becoming a "box".
        BoxWithConstraints(
            Modifier.fillMaxSize()
        ) {
            val scaffoldHeight = maxHeight

            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize(),
                scaffoldState = scaffoldState,
                sheetPeekHeight = peekHeight,
                sheetShape = RoundedCornerShape(topStart = SHEET_CORNER_RADIUS, topEnd = SHEET_CORNER_RADIUS),
                sheetContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = fraction),
                sheetTonalElevation = 0.dp,
                sheetDragHandle = null,
                sheetSwipeEnabled = showChrome && hasSong && nowPlayingView == NowPlayingView.PLAYER,
                containerColor = Color.Transparent,
                sheetContent = {
                    var offsetPx by remember { mutableStateOf(Float.NaN) }
                    LaunchedEffect(scaffoldState.bottomSheetState) {
                        snapshotFlow { runCatching { scaffoldState.bottomSheetState.requireOffset() }.getOrNull() }
                            .filterNotNull()
                            .collect { offsetPx = it }
                    }
                    val scaffoldHeightPx = with(density) { scaffoldHeight.toPx() }
                    val peekHeightPx = with(density) { peekHeight.toPx() }
                    val peekOffsetPx = (scaffoldHeightPx - peekHeightPx).coerceAtLeast(1f)
                    fraction = if (offsetPx.isNaN()) {
                        0f
                    } else {
                        (1f - (offsetPx / peekOffsetPx)).coerceIn(0f, 1f)
                    }

                    Box(Modifier.fillMaxWidth().height(scaffoldHeight)) {
                        // both layers start from the sheet's top; peek shows just the mini-player slice
                        if (hasSong) {
                            Box(Modifier.fillMaxWidth().alpha(1f - fraction)) {
                                MiniPlayerRow(
                                    playbackViewModel = playbackViewModel,
                                    onExpand = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                                    onThumbnailBoundsChange = { collapsedArtRect.value = it }
                                )
                            }
                        }
                        val playerAlpha = fraction
                        if (hasSong && playerAlpha > 0f) {
                            Box(Modifier.fillMaxSize().alpha(playerAlpha)) {
                                fun navigateFromNowPlaying(route: String) {
                                    nowPlayingView = NowPlayingView.PLAYER
                                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                                    navController.navigate(route) { launchSingleTop = true }
                                }

                                NowPlayingContent(
                                    playbackViewModel = playbackViewModel,
                                    fraction = fraction,
                                    collapsedArtRect = collapsedArtRect,
                                    activeView = nowPlayingView,
                                    onActiveViewChange = { nowPlayingView = it },
                                    // Queue/Lyrics only; PLAYER mode uses the scaffold's native swipe
                                    onCollapseRequest = { scope.launch { scaffoldState.bottomSheetState.partialExpand() } },
                                    onNavigateToArtist = { artist -> navigateFromNowPlaying(MainRoute.artistDetail(artist)) },
                                    onNavigateToAlbum = { albumId -> navigateFromNowPlaying(MainRoute.albumDetail(albumId)) },
                                    playlists = playlists,
                                    onNavigateToPlaylist = { playlistId -> navigateFromNowPlaying(MainRoute.playlistDetail(playlistId)) },
                                    onHeaderDragProgressChange = { headerDragRevealFraction = it }
                                )
                            }
                        }
                    }
                }
            ) { contentPadding ->
                NavHost(
                    navController = navController,
                    startDestination = MainRoute.LIBRARY,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = contentPadding.calculateTopPadding())
                        .then(if (artistHeroRoute) Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
                ) {
                    composable(MainRoute.HOME) {
                        HomeScreen(
                            playbackViewModel = playbackViewModel,
                            libraryViewModel = libraryViewModel,
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            onNavigateToLibrary = { navigateToTab(MainRoute.LIBRARY) },
                            // The app navigation is a glass overlay.  Do not reserve an opaque
                            // blank strip under it: content should continue beneath the tabs.
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.LIBRARY) {
                        LibraryScreen(
                            playbackViewModel = playbackViewModel,
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            onNavigateToArtist = { artist -> navController.navigate(MainRoute.artistDetail(artist)) },
                            onNavigateToAlbum = { albumId -> navController.navigate(MainRoute.albumDetail(albumId)) },
                            onNavigateToGenre = { genre -> navController.navigate(MainRoute.genreDetail(genre)) },
                            onNavigateToPlaylist = { playlistId -> navController.navigate(MainRoute.playlistDetail(playlistId)) },
                            onNavigateToLikedSongs = { navController.navigate(MainRoute.LIKED_SONGS) },
                            // nav bar is an overlay, not a space-reserving sibling
                            listBottomPadding = contentPadding.calculateBottomPadding(),
                            viewModel = libraryViewModel,
                            playlistViewModel = playlistViewModel
                        )
                    }
                    composable(
                        MainRoute.ARTIST_DETAIL,
                        arguments = listOf(navArgument("artist") { type = NavType.StringType })
                    ) { entry ->
                        val artist = Uri.decode(entry.arguments?.getString("artist").orEmpty())
                        ArtistDetailScreen(
                            artist = artist,
                            libraryViewModel = libraryViewModel,
                            playbackViewModel = playbackViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            onNavigateToAlbum = { albumId -> navController.navigate(MainRoute.albumDetail(albumId)) },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(
                        MainRoute.ALBUM_DETAIL,
                        arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                    ) { entry ->
                        val albumId = Uri.decode(entry.arguments?.getString("albumId").orEmpty())
                        AlbumDetailScreen(
                            albumId = albumId,
                            libraryViewModel = libraryViewModel,
                            playbackViewModel = playbackViewModel,
                            playlistViewModel = playlistViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(
                        MainRoute.GENRE_DETAIL,
                        arguments = listOf(navArgument("genre") { type = NavType.StringType })
                    ) { entry ->
                        val genre = Uri.decode(entry.arguments?.getString("genre").orEmpty())
                        GenreDetailScreen(
                            genre = genre,
                            libraryViewModel = libraryViewModel,
                            playbackViewModel = playbackViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(
                        MainRoute.PLAYLIST_DETAIL,
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                    ) { entry ->
                        val playlistId = Uri.decode(entry.arguments?.getString("playlistId").orEmpty())
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            libraryViewModel = libraryViewModel,
                            playlistViewModel = playlistViewModel,
                            playbackViewModel = playbackViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.LIKED_SONGS) {
                        LikedSongsScreen(
                            libraryViewModel = libraryViewModel,
                            playbackViewModel = playbackViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.SEARCH) {
                        SearchScreen(
                            playbackViewModel = playbackViewModel,
                            libraryViewModel = libraryViewModel,
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            focusRequestId = searchFocusRequestId,
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.SETTINGS) {
                        SettingsScreen(
                            onConnectServer = { protocol -> navController.navigate(MainRoute.connectServer(protocol)) },
                            onNavigateToStats = { navController.navigate(MainRoute.STATS) },
                            onManageScanFolders = { navController.navigate(MainRoute.MANAGE_SCAN_FOLDERS) },
                            onManageExternalServices = { navController.navigate(MainRoute.EXTERNAL_SERVICES) },
                            onNavigateToEqualizer = { navController.navigate(MainRoute.EQUALIZER) },
                            onNavigateToBackupRestore = { navController.navigate(MainRoute.BACKUP_RESTORE) },
                            onNavigateToDownloads = { navController.navigate(MainRoute.DOWNLOADS) },
                            onNavigateToSleepTimer = { navController.navigate(MainRoute.SLEEP_TIMER) },
                            onNavigateToPrivacy = { navController.navigate(MainRoute.PRIVACY) },
                            onNavigateToPairing = { navController.navigate(MainRoute.COMPANION_DEVICE) },
                            onNavigateToCredits = { navController.navigate(MainRoute.CREDITS) },
                            onNavigateToScrobbling = { navController.navigate(MainRoute.SCROBBLING) },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.SCROBBLING) {
                        ScrobbleSettingsScreen(
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.CREDITS) {
                        CreditsScreen(
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.COMPANION_DEVICE) {
                        PairingScreen(
                            onFinish = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.MANAGE_SCAN_FOLDERS) {
                        ManageScanFoldersScreen(onBack = { navController.popBackStack() })
                    }
                    composable(MainRoute.EXTERNAL_SERVICES) {
                        ExternalServicesScreen(onBack = { navController.popBackStack() })
                    }
                    composable(MainRoute.EQUALIZER) {
                        EqualizerScreen(onBack = { navController.popBackStack() })
                    }
                    composable(MainRoute.BACKUP_RESTORE) {
                        BackupRestoreScreen(onBack = { navController.popBackStack() })
                    }
                    composable(MainRoute.DOWNLOADS) {
                        DownloadsScreen(libraryViewModel = libraryViewModel, onBack = { navController.popBackStack() })
                    }
                    composable(MainRoute.SLEEP_TIMER) {
                        PlaybackPreferencesScreen(
                            playbackViewModel = playbackViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(MainRoute.PRIVACY) {
                        PrivacyScreen(onBack = { navController.popBackStack() })
                    }
                    composable(MainRoute.STATS) {
                        StatsScreen(
                            libraryViewModel = libraryViewModel,
                            playlistViewModel = playlistViewModel,
                            playbackViewModel = playbackViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            onNavigateToArtist = { artist -> navController.navigate(MainRoute.artistDetail(artist)) },
                            onNavigateToAlbum = { albumId -> navController.navigate(MainRoute.albumDetail(albumId)) },
                            onNavigateToPlaylist = { playlistId -> navController.navigate(MainRoute.playlistDetail(playlistId)) },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(
                        MainRoute.CONNECT_SERVER,
                        arguments = listOf(navArgument("protocol") { type = NavType.StringType })
                    ) { entry ->
                        val protocol = StreamingProtocol.valueOf(
                            entry.arguments?.getString("protocol") ?: StreamingProtocol.SUBSONIC.name
                        )
                        ConnectServerScreen(protocol = protocol, onDone = { navController.popBackStack() })
                    }
                }
            }
        }

        if (showChrome && !imeVisible) {
            // purely visual; no bearing on the scaffold's own measurements
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(NAV_BAR_TOTAL_HEIGHT * (1f - fraction))
                    .clipToBounds()
            ) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    contentAlpha = 1f - fraction,
                    onNavigate = ::navigateToTab,
                    onSearchLongPress = {
                        if (currentRoute != MainRoute.SEARCH) navigateToTab(MainRoute.SEARCH)
                        searchFocusRequestId++
                    }
                )
            }
        }
    }
}
