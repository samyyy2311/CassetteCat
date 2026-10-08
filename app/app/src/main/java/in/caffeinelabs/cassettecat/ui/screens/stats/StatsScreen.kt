package `in`.caffeinelabs.cassettecat.ui.screens.stats

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.device.DesktopRemoteRepository
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.stats.Listen
import `in`.caffeinelabs.cassettecat.data.stats.ListeningStatsRepository
import `in`.caffeinelabs.cassettecat.data.stats.Milestone
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.data.stats.monthKey
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.PlaylistViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongRowSkeleton
import `in`.caffeinelabs.cassettecat.ui.screens.library.UnderlineTabs
import `in`.caffeinelabs.cassettecat.ui.screens.library.rememberSkeletonColor
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import java.time.YearMonth
import java.time.format.TextStyle
import kotlinx.coroutines.launch

internal data class SongStat(val song: Song, val playCount: Int, val listeningMs: Long)

internal data class ArtistStat(val artist: String, val playCount: Int, val listeningMs: Long)

internal data class AlbumStat(
    val albumId: String,
    val album: String,
    val playCount: Int,
    val listeningMs: Long,
    val artSong: Song
)

internal data class GenreStat(val genre: String, val playCount: Int, val listeningMs: Long)

internal data class MonthComputed(
    val topArtists: List<ArtistStat>,
    val topAlbums: List<AlbumStat>,
    val topSongs: List<SongStat>,
    val topGenres: List<GenreStat> = emptyList(),
    val recentListens: List<Listen> = emptyList()
)

@Composable
fun StatsScreen(
    libraryViewModel: LibraryViewModel,
    playlistViewModel: PlaylistViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ListeningStatsRepository(context) }
    val desktop = remember { DesktopRemoteRepository.getInstance(context) }
    val desktopState by desktop.state.collectAsStateWithLifecycle()
    var syncing by remember { mutableStateOf(false) }
    val monthlyStats by repository.monthlyStats.collectAsStateWithLifecycle(initialValue = emptyMap<String, MonthlyStats>())
    val milestones by repository.milestones.collectAsStateWithLifecycle(initialValue = emptyList<Milestone>())
    val listens by repository.listens.collectAsStateWithLifecycle(initialValue = emptyList())
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val allSongsById = remember(libraryState) {
        (libraryState as? LibraryUiState.Loaded)?.songs?.associateBy { it.id }.orEmpty()
    }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val playlistTitleTemplate = stringResource(AppR.string.stats_playlist_title)
    val locale = LocalLocale.current.platformLocale

    val availableMonths = remember(monthlyStats) {
        monthlyStats.keys.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.sortedDescending()
    }
    val availableYears = remember(availableMonths) { availableMonths.map { it.year }.distinct() }

    // Rewind looks at one year, or one month of it; the other tabs cover the whole record, as on the computer.
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedMonth by rememberSaveable { mutableStateOf<String?>(null) }
    val year = selectedYear ?: availableYears.firstOrNull()
    val monthsInYear = remember(availableMonths, year) { availableMonths.filter { it.year == year } }
    val rewindMonth = selectedMonth?.let { key -> monthsInYear.find { it.toString() == key } }

    val allTime = remember(monthlyStats, listens, allSongsById) { periodRecord(monthlyStats.values, listens, allSongsById) }
    val rewind = remember(monthlyStats, listens, allSongsById, year, rewindMonth) {
        val prefix = rewindMonth?.toString() ?: "$year-"
        periodRecord(
            monthlyStats.filterKeys { it.startsWith(prefix) }.values,
            listens.filter { it.monthKey.startsWith(prefix) },
            allSongsById
        )
    }
    val monthMilestones = remember(milestones, rewindMonth) {
        rewindMonth?.let { month -> milestones.filter { isSameMonth(it.reachedAtMs, month) } }.orEmpty()
    }
    val busiestMonth = if (rewindMonth == null) {
        monthlyStats.filterKeys { it.startsWith("$year-") }.maxByOrNull { it.value.listeningMs }?.key
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    } else null

    val pagerState = rememberPagerState { StatsTab.entries.size }
    val currentTab = StatsTab.entries[pagerState.currentPage]
    fun showTab(tab: StatsTab) {
        scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
    }
    val tabLabels = StatsTab.entries.associateWith { stringResource(it.labelRes) }

    fun playSongs(songs: List<Song>, index: Int) {
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        playbackViewModel.playQueue(songs, index)
        if (wasIdle) onNavigateToNowPlaying()
    }
    fun playStat(songs: List<SongStat>, stat: SongStat) = playSongs(songs.map { it.song }, songs.indexOf(stat))
    fun playListen(listen: Listen) {
        allSongsById[listen.songId]?.let { playSongs(listOf(it), 0) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, end = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = stringResource(AppR.string.action_back),
                onClick = onBack
            )
            Text(stringResource(AppR.string.stats_listening_record), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (desktopState.address != null) {
                val computer = desktopState.name ?: stringResource(AppR.string.desktop_remote_your_computer)
                val synced = stringResource(AppR.string.stats_synced, computer)
                val notReached = stringResource(AppR.string.stats_sync_failed, computer)
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_refresh_cw,
                    contentDescription = stringResource(AppR.string.stats_sync_desc, computer),
                    onClick = {
                        if (!syncing) {
                            syncing = true
                            scope.launch {
                                val reached = desktop.syncWithDesktop()
                                syncing = false
                                Toast.makeText(context, if (reached) synced else notReached, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            if (currentTab == StatsTab.REWIND && year != null && rewind.stats.listeningMs > 0) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_share_2,
                    contentDescription = stringResource(AppR.string.action_share),
                    onClick = { showShareSheet = true }
                )
            }
            if (monthlyStats.isNotEmpty()) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_trash_2,
                    contentDescription = stringResource(AppR.string.stats_clear_stats_desc),
                    onClick = { showClearConfirm = true }
                )
            }
        }

        if (libraryState is LibraryUiState.Loading) {
            val skeletonColor = rememberSkeletonColor()
            Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                repeat(8) { SongRowSkeleton(skeletonColor) }
            }
        } else if (year == null) {
            EmptyState(
                catRes = AppR.drawable.cat_orange_headphones,
                title = stringResource(AppR.string.stats_empty_title),
                message = stringResource(AppR.string.stats_empty_message),
                modifier = Modifier.weight(1f)
            )
        } else {
            UnderlineTabs(
                modes = StatsTab.entries,
                selected = currentTab,
                label = { tabLabels.getValue(it) },
                onSelect = ::showTab
            )
            Spacer(Modifier.height(8.dp))

            val rewindTitle = stringResource(AppR.string.stats_rewind_title, year)
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
                when (StatsTab.entries[page]) {
                    StatsTab.OVERVIEW -> LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = listBottomPadding + 24.dp)) {
                        item {
                            ListeningRecordReadout(
                                month = null,
                                year = null,
                                isRewind = false,
                                listeningMinutes = allTime.stats.listeningMs / 60_000,
                                totalPlays = allTime.stats.songPlayCounts.values.sum(),
                                uniqueSongs = allTime.stats.songPlayCounts.size,
                                busiestMonth = null,
                                firstListenAt = listens.minOfOrNull { it.at }
                            )
                            Spacer(Modifier.height(28.dp))
                            MonthlyListeningChart(
                                year = availableYears.first(),
                                monthlyStats = monthlyStats,
                                selected = null,
                                onSelect = { month ->
                                    selectedYear = month.year
                                    selectedMonth = month.toString()
                                    showTab(StatsTab.REWIND)
                                }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                        statsSections(
                            computed = allTime.computed.summary(),
                            monthMilestones = emptyList(),
                            onNavigateToArtist = onNavigateToArtist,
                            onNavigateToAlbum = onNavigateToAlbum,
                            onPlayTrack = { playStat(allTime.computed.topSongs, it) },
                            onViewAllMostPlayed = { showTab(StatsTab.TRACKS) },
                            onPlayListen = ::playListen,
                            onSavePlaylist = null
                        )
                    }
                    StatsTab.TRACKS -> TopTracksTab(allTime.computed.topSongs, { playStat(allTime.computed.topSongs, it) }, listBottomPadding)
                    StatsTab.ARTISTS -> TopArtistsTab(allTime.computed.topArtists, onNavigateToArtist, listBottomPadding)
                    StatsTab.ALBUMS -> TopAlbumsTab(allTime.computed.topAlbums, onNavigateToAlbum, listBottomPadding)
                    StatsTab.HISTORY -> HistoryTab(allTime.computed.recentListens, ::playListen, listBottomPadding)
                    StatsTab.REWIND -> LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = listBottomPadding + 24.dp)) {
                        item {
                            if (availableYears.size > 1) {
                                YearSelector(
                                    years = availableYears,
                                    selected = year,
                                    onSelect = { selectedYear = it; selectedMonth = null }
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                            if (monthsInYear.size > 1) {
                                MonthTabs(
                                    months = monthsInYear,
                                    selected = rewindMonth,
                                    isRewindSelected = rewindMonth == null,
                                    onSelectMonth = { selectedMonth = it.toString() },
                                    onSelectRewind = { selectedMonth = null }
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                            ListeningRecordReadout(
                                month = rewindMonth,
                                year = year,
                                isRewind = rewindMonth == null,
                                listeningMinutes = rewind.stats.listeningMs / 60_000,
                                totalPlays = rewind.stats.songPlayCounts.values.sum(),
                                uniqueSongs = rewind.stats.songPlayCounts.size,
                                busiestMonth = busiestMonth,
                                firstListenAt = rewind.computed.recentListens.minOfOrNull { it.at }
                            )
                            Spacer(Modifier.height(28.dp))
                            MonthlyListeningChart(
                                year = year,
                                monthlyStats = monthlyStats,
                                selected = rewindMonth,
                                onSelect = { selectedMonth = if (it == rewindMonth) null else it.toString() }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                        statsSections(
                            computed = rewind.computed.summary(),
                            monthMilestones = monthMilestones,
                            onNavigateToArtist = onNavigateToArtist,
                            onNavigateToAlbum = onNavigateToAlbum,
                            onPlayTrack = { playStat(rewind.computed.topSongs, it) },
                            onViewAllMostPlayed = null,
                            onPlayListen = ::playListen,
                            onSavePlaylist = {
                                val period = rewindMonth?.month?.getDisplayName(TextStyle.FULL, locale)
                                val name = if (period != null) {
                                    String.format(locale, playlistTitleTemplate, period, year)
                                } else {
                                    rewindTitle
                                }
                                val songIds = rewind.computed.topSongs.take(25).map { it.song.id }
                                playlistViewModel.create(name) { playlist: Playlist ->
                                    playlistViewModel.addSongs(playlist.id, songIds)
                                    onNavigateToPlaylist(playlist.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(AppR.string.stats_clear_confirm_title)) },
            text = { Text(stringResource(AppR.string.stats_clear_confirm_message)) },
            confirmButton = {
                TextButton(onClick = hapticClick {
                    showClearConfirm = false
                    scope.launch { repository.clearAll() }
                }) {
                    Text(stringResource(AppR.string.action_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = hapticClick { showClearConfirm = false }) { Text(stringResource(AppR.string.action_cancel)) }
            }
        )
    }

    if (showShareSheet && year != null) {
        val monthName = rewindMonth?.month?.getDisplayName(TextStyle.FULL, locale)
        ListeningRecordShareSheet(
            monthAbbreviation = rewindMonth?.month?.getDisplayName(TextStyle.SHORT_STANDALONE, locale).orEmpty(),
            yearLabel = year.toString(),
            periodTitle = if (monthName == null) {
                stringResource(AppR.string.stats_rewind_title, year)
            } else {
                stringResource(AppR.string.stats_period_title, monthName, year)
            },
            listeningMinutes = rewind.stats.listeningMs / 60_000,
            totalPlays = rewind.stats.songPlayCounts.values.sum(),
            uniqueSongs = rewind.stats.songPlayCounts.size,
            topArtists = rewind.computed.topArtists.take(10),
            topSongs = rewind.computed.topSongs.take(25),
            topAlbums = rewind.computed.topAlbums.take(10),
            isRewind = rewindMonth == null,
            onDismiss = { showShareSheet = false }
        )
    }
}
