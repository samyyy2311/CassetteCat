@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.AppShortcutAction
import `in`.caffeinelabs.cassettecat.data.radio.RadioFavoritesRepository
import `in`.caffeinelabs.cassettecat.data.radio.toSong
import `in`.caffeinelabs.cassettecat.ui.components.MiniPlayerRow
import `in`.caffeinelabs.cassettecat.ui.components.loadSongArtwork
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
import `in`.caffeinelabs.cassettecat.ui.screens.library.SmartPlaylistScreen
import `in`.caffeinelabs.cassettecat.ui.screens.library.SmartPlaylistType
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.NowPlayingContent
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.NowPlayingView
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PairingScreen
import `in`.caffeinelabs.cassettecat.ui.screens.radio.RadioScreen
import `in`.caffeinelabs.cassettecat.ui.screens.search.SearchScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.AboutLegalScreen
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
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.DefaultStartScreen
import `in`.caffeinelabs.cassettecat.data.settings.ThemeAccent
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationAudioEngineScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationHomeFeedScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationLyricsScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationLibraryTabsScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationNowPlayingScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationRoute
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationStartupLibraryScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationStorageScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.CustomizationThemeScreen
import `in`.caffeinelabs.cassettecat.ui.screens.settings.SettingsViewModel
import androidx.compose.ui.platform.LocalContext
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.theme.dominantArtworkAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object MainRoute {
    const val HOME = "main/home"
    const val LIBRARY = "main/library"
    const val SEARCH = "main/search"
    const val RADIO = "main/radio"
    const val SETTINGS = "main/settings"
    const val CUSTOMIZATION = "main/settings/customization"
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
    const val SMART_PLAYLIST_DETAIL = "main/library/smart_playlist/{type}"
    fun smartPlaylistDetail(type: String) = "main/library/smart_playlist/$type"
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
    const val ABOUT_LEGAL = "main/settings/about_legal"
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
fun MainShell(
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
    shortcutAction: String? = null,
    onShortcutHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val scope = rememberCoroutineScope()

    fun navigateToTab(route: String) {
        scope.launch { appPreferencesRepository.setLastOpenedRoute(route) }
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

    var initialTabHandled by remember { mutableStateOf(false) }
    LaunchedEffect(preferences.defaultStartScreen, preferences.lastOpenedRoute) {
        if (!initialTabHandled) {
            initialTabHandled = true
            val targetRoute = when (preferences.defaultStartScreen) {
                DefaultStartScreen.HOME -> MainRoute.HOME
                DefaultStartScreen.LIBRARY -> MainRoute.LIBRARY
                DefaultStartScreen.LAST_OPENED -> when (preferences.lastOpenedRoute) {
                    MainRoute.HOME -> MainRoute.HOME
                    MainRoute.SEARCH -> MainRoute.SEARCH
                    MainRoute.SETTINGS -> MainRoute.SETTINGS
                    else -> MainRoute.LIBRARY
                }
            }
            if (targetRoute != MainRoute.LIBRARY) {
                navController.navigate(targetRoute) {
                    popUpTo(MainRoute.LIBRARY) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    // shared across Library/Home/Search to avoid redundant refetches
    val libraryViewModel: LibraryViewModel = viewModel()
    val playlistViewModel: PlaylistViewModel = viewModel()
    val radioFavoritesRepository = remember { RadioFavoritesRepository(context) }
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val librarySongs = (libraryState as? LibraryUiState.Loaded)?.songs.orEmpty()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val skipRestoreForLaunchShortcut = remember { shortcutAction != null }
    LaunchedEffect(libraryState) {
        if (!skipRestoreForLaunchShortcut) {
            (libraryState as? LibraryUiState.Loaded)?.let { playbackViewModel.restoreIfNeeded(it.songs) }
        }
    }
    // Artist art deliberately extends behind the transparent status bar. Other routes retain
    // their normal safe-area layout so ordinary screen headers are never pushed under system UI.
    val artistHeroRoute = currentRoute == MainRoute.ARTIST_DETAIL
    // artist/album/playlist detail are playback-adjacent, unlike CONNECT_SERVER, so chrome stays visible
    val showChrome = currentRoute != null && currentRoute != MainRoute.CONNECT_SERVER
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val hasSong = playbackState.currentSong != null
    var artworkAccent by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(preferences.artworkAccentEnabled, playbackState.currentSong?.id) {
        artworkAccent = if (preferences.artworkAccentEnabled) {
            playbackState.currentSong?.let { song ->
                runCatching {
                    loadSongArtwork(context, song)?.let { bitmap ->
                        withContext(Dispatchers.Default) { dominantArtworkAccent(bitmap) }
                    }
                }.getOrNull()
            }
        } else {
            null
        }
    }
    val density = LocalDensity.current
    // The custom bottom bar is an app overlay, so it must explicitly yield to the IME.
    // Otherwise its labels are left hovering below/over the keyboard on text-entry screens.
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val navigationBarInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    val navBarReservation = if (showChrome && !imeVisible) NAV_BAR_TOTAL_HEIGHT + navigationBarInset else 0.dp
    // fixed height, not animated, to avoid a feedback loop with `fraction`
    val peekHeight = if (showChrome) navBarReservation + (if (hasSong) MINI_PLAYER_HEIGHT else 0.dp) else 0.dp
    val scaffoldState = rememberBottomSheetScaffoldState()

    var fraction by remember { mutableFloatStateOf(0f) }
    // State, not by: per-frame drag updates shouldn't recompose MainShell
    val collapsedArtRect = remember { mutableStateOf<Rect?>(null) }
    // hoisted so sheetSwipeEnabled can turn off during Queue/Lyrics
    var nowPlayingView by remember { mutableStateOf(NowPlayingView.PLAYER) }
    var searchFocusRequestId by remember { mutableIntStateOf(0) }

    LaunchedEffect(shortcutAction, libraryState) {
        val action = shortcutAction ?: return@LaunchedEffect
        if (libraryState is LibraryUiState.Loading) return@LaunchedEffect
        val songs = when (action) {
            AppShortcutAction.SHUFFLE_ALL -> librarySongs.shuffled()
            AppShortcutAction.PLAY_FAVORITES -> librarySongs.filter { it.isFavorite }
            AppShortcutAction.PLAY_RADIO_FAVORITES -> radioFavoritesRepository.favoriteStations.first().shuffled().take(1).map { it.toSong() }
            else -> emptyList()
        }
        onShortcutHandled()
        if (songs.isEmpty()) {
            Toast.makeText(context, "Nothing to play yet", Toast.LENGTH_SHORT).show()
        } else {
            playbackViewModel.playQueue(songs, 0)
            nowPlayingView = NowPlayingView.PLAYER
            scaffoldState.bottomSheetState.expand()
        }
    }

    // fades the sheet fill in Queue/Lyrics mode, where the real sheet doesn't move
    var headerDragRevealFraction by remember { mutableFloatStateOf(0f) }

    val isSheetExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded ||
            scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded ||
            fraction > 0.4f

    // resets sub-view only when fully collapsed so it doesn't flip views mid-animation
    LaunchedEffect(fraction) {
        if (fraction <= 0.01f && nowPlayingView != NowPlayingView.PLAYER) {
            nowPlayingView = NowPlayingView.PLAYER
        }
    }

    CassetteCatTheme(
        accent = if (artworkAccent != null) ThemeAccent.CUSTOM else preferences.themeAccent,
        customAccentColor = artworkAccent ?: preferences.customAccentColor,
        isAmoled = preferences.amoledDarkTheme
    ) {
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
                sheetShape = RoundedCornerShape(
                    topStart = lerp(SHEET_CORNER_RADIUS, 0.dp, fraction),
                    topEnd = lerp(SHEET_CORNER_RADIUS, 0.dp, fraction)
                ),
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                sheetTonalElevation = 0.dp,
                sheetDragHandle = null,
                sheetSwipeEnabled = showChrome && hasSong && nowPlayingView == NowPlayingView.PLAYER,
                containerColor = Color.Transparent,
                sheetContent = {
                    var offsetPx by remember { mutableFloatStateOf(Float.NaN) }
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
                            val miniPlayerAlpha = ((1f - fraction) / 0.45f).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = miniPlayerAlpha }
                            ) {
                                MiniPlayerRow(
                                    playbackViewModel = playbackViewModel,
                                    onExpand = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                                    onOpenQueue = {
                                        nowPlayingView = NowPlayingView.QUEUE
                                        scope.launch { scaffoldState.bottomSheetState.expand() }
                                    },
                                    onThumbnailBoundsChange = { collapsedArtRect.value = it }
                                )
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = fraction }
                            ) {
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
                                    allSongs = librarySongs,
                                    onSaveQueue = { name, songIds ->
                                        playlistViewModel.create(name, songIds)
                                        Toast.makeText(context, "Queue saved to $name", Toast.LENGTH_SHORT).show()
                                    },
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
                        .then(if (artistHeroRoute) Modifier else Modifier.statusBarsPadding())
                ) {
                    composable(MainRoute.HOME) {
                        HomeScreen(
                            playbackViewModel = playbackViewModel,
                            libraryViewModel = libraryViewModel,
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            onNavigateToLibrary = { navigateToTab(MainRoute.LIBRARY) },
                            onNavigateToArtist = { artist -> navController.navigate(MainRoute.artistDetail(artist)) },
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
                            onNavigateToSmartPlaylist = { type -> navController.navigate(MainRoute.smartPlaylistDetail(type.id)) },
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
                    composable(
                        MainRoute.SMART_PLAYLIST_DETAIL,
                        arguments = listOf(navArgument("type") { type = NavType.StringType })
                    ) { entry ->
                        val typeId = entry.arguments?.getString("type").orEmpty()
                        val type = SmartPlaylistType.fromId(typeId)
                        SmartPlaylistScreen(
                            playlistType = type,
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
                    composable(MainRoute.RADIO) {
                        RadioScreen(
                            playbackViewModel = playbackViewModel,
                            onNavigateToNowPlaying = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.SETTINGS) {
                        SettingsScreen(
                            playbackViewModel = playbackViewModel,
                            libraryViewModel = libraryViewModel,
                            onConnectServer = { protocol -> navController.navigate(MainRoute.connectServer(protocol)) },
                            onNavigateToStats = { navController.navigate(MainRoute.STATS) },
                            onManageScanFolders = { navController.navigate(MainRoute.MANAGE_SCAN_FOLDERS) },
                            onManageExternalServices = { navController.navigate(MainRoute.EXTERNAL_SERVICES) },
                            onNavigateToEqualizer = { navController.navigate(MainRoute.EQUALIZER) },
                            onNavigateToBackupRestore = { navController.navigate(MainRoute.BACKUP_RESTORE) },
                            onNavigateToDownloads = { navController.navigate(MainRoute.DOWNLOADS) },
                            onNavigateToSleepTimer = { navController.navigate(MainRoute.SLEEP_TIMER) },
                            onNavigateToPrivacy = { navController.navigate(MainRoute.PRIVACY) },
                            onNavigateToCustomization = { navController.navigate(MainRoute.CUSTOMIZATION) },
                            onNavigateToPairing = { navController.navigate(MainRoute.COMPANION_DEVICE) },
                            onNavigateToAboutLegal = { navController.navigate(MainRoute.ABOUT_LEGAL) },
                            onNavigateToCredits = { navController.navigate(MainRoute.CREDITS) },
                            onNavigateToScrobbling = { navController.navigate(MainRoute.SCROBBLING) },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.CUSTOMIZATION) {
                        CustomizationScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(CustomizationRoute.THEME) {
                        CustomizationThemeScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(CustomizationRoute.STARTUP_LIBRARY) {
                        CustomizationStartupLibraryScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            onNavigateToLibraryTabs = { navController.navigate(CustomizationRoute.LIBRARY_TABS) },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(CustomizationRoute.LIBRARY_TABS) {
                        CustomizationLibraryTabsScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(CustomizationRoute.NOW_PLAYING) {
                        CustomizationNowPlayingScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(CustomizationRoute.AUDIO_ENGINE) {
                        CustomizationAudioEngineScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(CustomizationRoute.LYRICS) {
                        CustomizationLyricsScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(CustomizationRoute.STORAGE) {
                        CustomizationStorageScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(CustomizationRoute.HOME_FEED) {
                        CustomizationHomeFeedScreen(
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.SCROBBLING) {
                        ScrobbleSettingsScreen(
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
                    }
                    composable(MainRoute.ABOUT_LEGAL) {
                        AboutLegalScreen(
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
                        EqualizerScreen(
                            onBack = { navController.popBackStack() },
                            listBottomPadding = contentPadding.calculateBottomPadding()
                        )
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
                        ConnectServerScreen(
                            protocol = protocol,
                            onDone = {
                                libraryViewModel.refresh()
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
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
                    .height(NAV_BAR_TOTAL_HEIGHT * (1f - fraction) + navigationBarInset)
                    .padding(bottom = navigationBarInset)
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

        val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        DisposableEffect(isSheetExpanded, backDispatcher) {
            if (!isSheetExpanded || backDispatcher == null) return@DisposableEffect onDispose {}
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (nowPlayingView != NowPlayingView.PLAYER) {
                        nowPlayingView = NowPlayingView.PLAYER
                    } else {
                        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    }
                }
            }
            backDispatcher.addCallback(callback)
            onDispose { callback.remove() }
        }
    }
    }
}
