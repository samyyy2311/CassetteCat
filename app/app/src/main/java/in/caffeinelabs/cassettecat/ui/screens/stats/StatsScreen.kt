package `in`.caffeinelabs.cassettecat.ui.screens.stats

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.stats.ListeningStatsRepository
import `in`.caffeinelabs.cassettecat.data.stats.Milestone
import `in`.caffeinelabs.cassettecat.data.stats.MilestoneType
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.PlaylistViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongRowSkeleton
import `in`.caffeinelabs.cassettecat.ui.screens.library.splitArtists
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.io.File
import java.io.FileOutputStream
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private data class SongStat(val song: Song, val playCount: Int, val listeningMs: Long)

private data class ArtistStat(val artist: String, val playCount: Int, val listeningMs: Long)

private data class AlbumStat(
    val albumId: String,
    val album: String,
    val playCount: Int,
    val listeningMs: Long,
    val artSong: Song
)

private data class MonthComputed(
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

    val availableMonths = remember(monthlyStats) {
        monthlyStats.keys.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.sortedDescending()
    }
    val availableYears = remember(availableMonths) { availableMonths.map { it.year }.distinct() }

    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedMonth by rememberSaveable { mutableStateOf<String?>(null) }
    val year = selectedYear ?: availableYears.firstOrNull()
    val monthsInYear = remember(availableMonths, year) { availableMonths.filter { it.year == year } }
    val month = selectedMonth?.let { key -> monthsInYear.find { it.toString() == key } } ?: monthsInYear.firstOrNull()

    val monthStats = month?.let { monthlyStats[it.toString()] }

    val computed = remember(month, monthStats, allSongsById) {
        val stats = monthStats ?: return@remember MonthComputed(emptyList(), emptyList(), emptyList())
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

    val listeningMinutes = (monthStats?.listeningMs ?: 0L) / 60_000
    val totalPlays = monthStats?.songPlayCounts?.values?.sum() ?: 0
    val uniqueSongs = monthStats?.songPlayCounts?.size ?: 0
    val monthName = month?.month?.getDisplayName(TextStyle.FULL, Locale.US) ?: ""

    if (showAllMostPlayed) {
        BackHandler { showAllMostPlayed = false }
        MostPlayedTracksScreen(
            songs = computed.topSongs,
            monthName = monthName,
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
            if (month != null) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_share_2,
                    contentDescription = "Share",
                    onClick = {
                        val bitmap = buildListeningRecordPoster(
                            month = month,
                            listeningMinutes = listeningMinutes,
                            totalPlays = totalPlays,
                            uniqueSongs = uniqueSongs,
                            topArtists = computed.topArtists.map { it.artist }
                        )
                        shareListeningRecordPoster(context, bitmap, month)
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
                    onSelect = { selectedMonth = it.toString() }
                )
            }
            Spacer(Modifier.height(if (availableYears.size > 1 || monthsInYear.size > 1) 16.dp else 8.dp))

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = listBottomPadding)) {
                item {
                    ListeningRecordReadout(
                        month = month,
                        listeningMinutes = listeningMinutes,
                        totalPlays = totalPlays,
                        uniqueSongs = uniqueSongs
                    )
                    Spacer(Modifier.height(32.dp))
                }

                if (computed.topArtists.isNotEmpty()) {
                    item {
                        SectionHeader("MOST PLAYED ARTISTS")
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(computed.topArtists) { index, artist ->
                                RankedCard(
                                    rank = index + 1,
                                    title = artist.artist,
                                    subtitle = formatRecordedMinutes(artist.listeningMs),
                                    onClick = { onNavigateToArtist(artist.artist) },
                                    art = { artModifier -> ArtistImage(artist = artist.artist, modifier = artModifier) }
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                if (computed.topSongs.isNotEmpty()) {
                    item {
                        SectionHeader("MOST PLAYED TRACKS")
                    }
                    items(computed.topSongs.take(5), key = { it.song.id }) { stat ->
                        StatSongRow(
                            song = stat.song,
                            count = stat.playCount,
                            onClick = {
                                val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                                val songs = computed.topSongs.map { it.song }
                                val index = songs.indexOfFirst { it.id == stat.song.id }
                                playbackViewModel.playQueue(songs, index)
                                if (wasIdle) onNavigateToNowPlaying()
                            }
                        )
                    }
                    item {
                        if (computed.topSongs.size > 5) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tapScale { showAllMostPlayed = true }
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "View all most played",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    painter = painterResource(R.drawable.lucide_ic_chevron_right),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tapScale {
                                    val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                                    val name = "Listening Record: $monthName ${month.year}"
                                    val songIds = computed.topSongs.map { it.song.id }
                                    playlistViewModel.create(name) { playlist: Playlist ->
                                        playlistViewModel.addSongs(playlist.id, songIds)
                                        onNavigateToPlaylist(playlist.id)
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_list_plus),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Save these tracks as a playlist",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                if (computed.topAlbums.isNotEmpty()) {
                    item {
                        SectionHeader("MOST PLAYED ALBUMS")
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(computed.topAlbums.take(5)) { index, album ->
                                RankedCard(
                                    rank = index + 1,
                                    title = album.album,
                                    subtitle = formatRecordedMinutes(album.listeningMs),
                                    onClick = { onNavigateToAlbum(album.albumId) },
                                    art = { artModifier -> AlbumArt(song = album.artSong, modifier = artModifier) },
                                    width = 224.dp
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                if (monthMilestones.isNotEmpty()) {
                    item { SectionHeader("MILESTONES") }
                    items(monthMilestones) { milestone -> MilestoneRow(milestone) }
                }
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
}

private fun isSameMonth(epochMs: Long, month: YearMonth): Boolean =
    YearMonth.from(java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault())) == month

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun YearSelector(years: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            "YEAR",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(years) { year ->
                val isSelected = year == selected
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .tapScale { onSelect(year) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthTabs(months: List<YearMonth>, selected: YearMonth, onSelect: (YearMonth) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(months.sorted()) { month ->
            val isSelected = month == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.tapScale { onSelect(month) }
            ) {
                Text(
                    month.month.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(Locale.US),
                    style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun ListeningRecordReadout(
    month: YearMonth,
    listeningMinutes: Long,
    totalPlays: Int,
    uniqueSongs: Int
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            "RECORDED // ${month.month.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(Locale.US)} ${month.year}",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = IbmPlexMonoFontFamily,
                lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(10.dp))
        Text(
            formatListeningTime(listeningMinutes),
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = IbmPlexMonoFontFamily)
        )
        Text(
            if (listeningMinutes == 0L) "No listening time has been recorded yet."
            else "Listening time for this month",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RecordMetric(
                label = "PLAYS",
                value = totalPlays.toString().padStart(2, '0'),
                modifier = Modifier.weight(1f)
            )
            RecordMetric(
                label = "TRACKS",
                value = uniqueSongs.toString().padStart(2, '0'),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecordMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = IbmPlexMonoFontFamily)
        )
    }
}

private fun formatListeningTime(minutes: Long): String = when {
    minutes >= 60 -> "%02dH %02dM".format(Locale.US, minutes / 60, minutes % 60)
    else -> "%02d MIN".format(Locale.US, minutes)
}

private fun formatRecordedMinutes(listeningMs: Long): String? = when {
    listeningMs < 1_000L -> null
    listeningMs < 60_000L -> "< 1 MIN"
    else -> "${listeningMs / 60_000} MIN"
}

@Composable
private fun RankedCard(
    rank: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    art: @Composable (Modifier) -> Unit,
    width: Dp = 150.dp
) {
    Column(modifier = Modifier.width(width).tapScale(onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            art(Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    "$rank",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatSongRow(song: Song, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().tapScale(onClick).padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (count > 1) {
            Text(
                "$count plays",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MostPlayedTracksScreen(
    songs: List<SongStat>,
    monthName: String,
    onBack: () -> Unit,
    onPlay: (SongStat) -> Unit,
    listBottomPadding: Dp
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Most played", style = MaterialTheme.typography.titleLarge)
                Text(
                    monthName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = listBottomPadding)) {
            itemsIndexed(songs, key = { _, stat -> stat.song.id }) { index, stat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp).padding(start = 16.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        StatSongRow(song = stat.song, count = stat.playCount, onClick = { onPlay(stat) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneRow(milestone: Milestone) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_award),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            val label = when (milestone.type) {
                MilestoneType.MINUTES_PLAYED -> "Listening time"
                MilestoneType.SONGS_PLAYED -> "Tracks completed"
            }
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Unlocked this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            milestone.thresholdValue.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

private fun buildListeningRecordPoster(
    month: YearMonth,
    listeningMinutes: Long,
    totalPlays: Int,
    uniqueSongs: Int,
    topArtists: List<String>
): Bitmap {
    val size = 1080
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(
                android.graphics.Color.parseColor("#E54B3E"),
                android.graphics.Color.parseColor("#351821"),
                android.graphics.Color.parseColor("#09090B")
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(36, 255, 244, 237)
    }
    canvas.drawCircle(920f, 150f, 250f, accentPaint)
    canvas.drawCircle(120f, 940f, 180f, accentPaint)

    val titlePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#F5F0EC")
        textSize = 64f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText("CASSETTECAT", 64f, 108f, Paint(titlePaint).apply { textSize = 30f; letterSpacing = 0.12f })
    canvas.drawText("Your listening recap", 64f, 178f, titlePaint)

    val yearPaint = Paint(titlePaint).apply { textSize = 178f }
    canvas.drawText(month.month.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(Locale.US), 64f, 370f, yearPaint)

    val monthPaint = Paint(titlePaint).apply {
        textSize = 48f
        color = android.graphics.Color.parseColor("#A8A29A")
        typeface = Typeface.DEFAULT
    }
    canvas.drawText(month.year.toString(), 70f, 438f, monthPaint)

    val statPaint = Paint(titlePaint).apply { textSize = 56f }
    canvas.drawText("${formatListeningTime(listeningMinutes)}", 64f, 620f, statPaint)
    canvas.drawText("listened", 64f, 675f, Paint(monthPaint).apply { textSize = 36f })
    canvas.drawText("$totalPlays plays  ·  $uniqueSongs tracks", 64f, 770f, Paint(titlePaint).apply { textSize = 38f })

    if (topArtists.isNotEmpty()) {
        val artistsPaint = Paint(monthPaint).apply { textSize = 34f }
        canvas.drawText("ON REPEAT", 64f, 870f, Paint(monthPaint).apply { textSize = 26f; letterSpacing = 0.12f })
        canvas.drawText(topArtists.take(3).joinToString("  •  "), 64f, 930f, artistsPaint)
    }

    return bitmap
}

private fun shareListeningRecordPoster(context: android.content.Context, bitmap: Bitmap, month: YearMonth) {
    val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(dir, "listening_record_$month.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
