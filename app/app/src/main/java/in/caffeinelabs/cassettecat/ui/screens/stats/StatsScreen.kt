package `in`.caffeinelabs.cassettecat.ui.screens.stats

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.stats.ListeningStatsRepository
import `in`.caffeinelabs.cassettecat.data.stats.Milestone
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.PlaylistViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongRowSkeleton
import `in`.caffeinelabs.cassettecat.ui.screens.library.splitArtists
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
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

internal data class MonthComputed(
    val topArtists: List<ArtistStat>,
    val topAlbums: List<AlbumStat>,
    val topSongs: List<SongStat>
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
    val monthlyStats by repository.monthlyStats.collectAsState(initial = emptyMap<String, MonthlyStats>())
    val milestones by repository.milestones.collectAsState(initial = emptyList<Milestone>())
    val libraryState by libraryViewModel.uiState.collectAsState()
    val allSongsById = remember(libraryState) {
        (libraryState as? LibraryUiState.Loaded)?.songs?.associateBy { it.id }.orEmpty()
    }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showAllMostPlayed by rememberSaveable { mutableStateOf(false) }
    var sharePreview by remember { mutableStateOf<Pair<Bitmap, String>?>(null) }

    val availableMonths = remember(monthlyStats) {
        monthlyStats.keys.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.sortedDescending()
    }
    val availableYears = remember(availableMonths) { availableMonths.map { it.year }.distinct() }

    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedMonth by rememberSaveable { mutableStateOf<String?>(null) }
    val year = selectedYear ?: availableYears.firstOrNull()
    val monthsInYear = remember(availableMonths, year) { availableMonths.filter { it.year == year } }
    val isRewindMode = selectedMonth == "REWIND"
    val month = if (isRewindMode) null else selectedMonth?.let { key -> monthsInYear.find { it.toString() == key } } ?: monthsInYear.firstOrNull()

    val activeStats = remember(isRewindMode, month, monthlyStats, year) {
        if (isRewindMode && year != null) {
            val yearEntries = monthlyStats.filterKeys { it.startsWith("$year-") }
            val counts = mutableMapOf<String, Int>()
            val msMap = mutableMapOf<String, Long>()
            var totalMs = 0L
            for ((_, entry) in yearEntries) {
                totalMs += entry.listeningMs
                for ((id, count) in entry.songPlayCounts) {
                    counts[id] = (counts[id] ?: 0) + count
                }
                for ((id, ms) in entry.songListeningMs) {
                    msMap[id] = (msMap[id] ?: 0L) + ms
                }
            }
            MonthlyStats(counts, totalMs, msMap)
        } else {
            month?.let { monthlyStats[it.toString()] }
        }
    }

    val computed = remember(isRewindMode, month, activeStats, allSongsById) {
        val stats = activeStats ?: return@remember MonthComputed(emptyList(), emptyList(), emptyList())
        val playedSongs = stats.songPlayCounts.mapNotNull { (id, count) ->
            allSongsById[id]?.let { SongStat(it, count, stats.songListeningMs[id] ?: 0L) }
        }

        val topArtists = playedSongs.flatMap { stat ->
            stat.song.artist.splitArtists().map { ArtistStat(it, stat.playCount, stat.listeningMs) }
        }
            .groupBy { it.artist }
            .map { (artist, entries) ->
                ArtistStat(artist, entries.sumOf { it.playCount }, entries.sumOf { it.listeningMs })
            }
            .sortedWith(compareByDescending<ArtistStat> { it.listeningMs }.thenByDescending { it.playCount })
            .take(10)

        val topAlbums = playedSongs.groupBy { it.song.albumId }
            .map { (albumId, entries) ->
                AlbumStat(
                    albumId = albumId,
                    album = entries.first().song.album,
                    playCount = entries.sumOf { it.playCount },
                    listeningMs = entries.sumOf { it.listeningMs },
                    artSong = entries.first().song
                )
            }
            .sortedWith(compareByDescending<AlbumStat> { it.listeningMs }.thenByDescending { it.playCount })
            .take(10)

        val topSongs = playedSongs.sortedByDescending { it.playCount }.take(25)

        MonthComputed(topArtists, topAlbums, topSongs)
    }

    val monthMilestones = remember(milestones, month) {
        if (month == null) emptyList() else milestones.filter { isSameMonth(it.reachedAtMs, month) }
    }

    val listeningMinutes = (activeStats?.listeningMs ?: 0L) / 60_000
    val totalPlays = activeStats?.songPlayCounts?.values?.sum() ?: 0
    val uniqueSongs = activeStats?.songPlayCounts?.size ?: 0
    val monthName = if (isRewindMode) "REWIND" else month?.month?.getDisplayName(TextStyle.FULL, Locale.US) ?: ""

    if (showAllMostPlayed) {
        BackHandler { showAllMostPlayed = false }
        MostPlayedTracksScreen(
            songs = computed.topSongs,
            monthName = if (isRewindMode) "Year $year" else monthName,
            onBack = { showAllMostPlayed = false },
            onPlay = { stat ->
                val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                val index = computed.topSongs.indexOfFirst { it.song.id == stat.song.id }
                playbackViewModel.playQueue(computed.topSongs.map { it.song }, index)
                if (wasIdle) onNavigateToNowPlaying()
            },
            listBottomPadding = listBottomPadding
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, end = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack
            )
            Text("Listening Record", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (activeStats != null && year != null) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_share_2,
                    contentDescription = "Share",
                    onClick = {
                        val titleLabel = if (isRewindMode) "Cassette Rewind $year" else "$monthName $year"
                        val bitmap = buildListeningRecordPoster(
                            monthLabel = monthName,
                            yearLabel = year.toString(),
                            listeningMinutes = listeningMinutes,
                            totalPlays = totalPlays,
                            uniqueSongs = uniqueSongs,
                            topArtists = computed.topArtists.map { it.artist },
                            topSongs = computed.topSongs.take(5).map { Triple(it.song.title, it.song.artist, it.playCount) },
                            isRewind = isRewindMode
                        )
                        sharePreview = bitmap to titleLabel
                    }
                )
            }
            if (monthlyStats.isNotEmpty()) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_trash_2,
                    contentDescription = "Clear stats",
                    onClick = { showClearConfirm = true }
                )
            }
        }

        if (libraryState is LibraryUiState.Loading) {
            Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                repeat(8) { SongRowSkeleton() }
            }
        } else if (month == null || year == null) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_chart_no_axes_combined,
                title = "No plays yet",
                message = "Play something past the halfway point and it'll show up here.",
                modifier = Modifier.weight(1f)
            )
        } else {
            if (availableYears.size > 1) {
                YearSelector(
                    years = availableYears,
                    selected = year,
                    onSelect = { selectedYear = it; selectedMonth = null }
                )
            }
            if (monthsInYear.size > 1) {
                MonthTabs(
                    months = monthsInYear,
                    selected = month,
                    isRewindSelected = isRewindMode,
                    onSelectMonth = { selectedMonth = it.toString() },
                    onSelectRewind = { selectedMonth = "REWIND" }
                )
            }
            Spacer(Modifier.height(if (availableYears.size > 1 || monthsInYear.size > 1) 16.dp else 8.dp))

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = listBottomPadding)) {
                item {
                    ListeningRecordReadout(
                        month = month,
                        year = year,
                        isRewind = isRewindMode,
                        listeningMinutes = listeningMinutes,
                        totalPlays = totalPlays,
                        uniqueSongs = uniqueSongs
                    )
                    Spacer(Modifier.height(32.dp))
                }

                statsSections(
                    computed = computed,
                    month = month,
                    monthMilestones = monthMilestones,
                    onNavigateToArtist = onNavigateToArtist,
                    onNavigateToAlbum = onNavigateToAlbum,
                    onPlayTrack = { stat ->
                        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                        val songs = computed.topSongs.map { it.song }
                        val index = songs.indexOfFirst { it.id == stat.song.id }
                        playbackViewModel.playQueue(songs, index)
                        if (wasIdle) onNavigateToNowPlaying()
                    },
                    onViewAllMostPlayed = { showAllMostPlayed = true },
                    onSavePlaylist = {
                        val monthNameFormatted = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        val name = "Listening Record: $monthNameFormatted ${month.year}"
                        val songIds = computed.topSongs.map { it.song.id }
                        playlistViewModel.create(name) { playlist: Playlist ->
                            playlistViewModel.addSongs(playlist.id, songIds)
                            onNavigateToPlaylist(playlist.id)
                        }
                    }
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear listening stats?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = hapticClick {
                    showClearConfirm = false
                    scope.launch { repository.clearAll() }
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = hapticClick { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    sharePreview?.let { (bitmap, title) ->
        ListeningRecordShareSheet(bitmap = bitmap, title = title, onDismiss = { sharePreview = null })
    }
}




