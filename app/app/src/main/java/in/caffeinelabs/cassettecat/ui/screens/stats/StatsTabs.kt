package `in`.caffeinelabs.cassettecat.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.stats.Listen
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.data.stats.monthKey
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.screens.library.splitArtists
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.time.Instant
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/** The Listening Record's tabs, the same as the computer's. */
internal enum class StatsTab(val labelRes: Int) {
    OVERVIEW(AppR.string.stats_tab_overview),
    TRACKS(AppR.string.stats_tab_tracks),
    ARTISTS(AppR.string.stats_tab_artists),
    ALBUMS(AppR.string.stats_tab_albums),
    HISTORY(AppR.string.stats_tab_history),
    REWIND(AppR.string.stats_tab_rewind)
}

internal data class PeriodRecord(val stats: MonthlyStats, val computed: MonthComputed)

/**
 * Totals and full rankings for the months in [months], with [listens] from the same months. Plays count songs in the
 * library by their id and songs that aren't by their listens; both rank by plays, then listening time, like the computer.
 */
internal fun periodRecord(months: Collection<MonthlyStats>, listens: List<Listen>, songsById: Map<String, Song>): PeriodRecord {
    val counts = mutableMapOf<String, Int>()
    val listeningMs = mutableMapOf<String, Long>()
    for (month in months) {
        month.songPlayCounts.forEach { (id, count) -> counts[id] = (counts[id] ?: 0) + count }
        month.songListeningMs.forEach { (id, ms) -> listeningMs[id] = (listeningMs[id] ?: 0L) + ms }
    }
    val stats = MonthlyStats(counts, months.sumOf { it.listeningMs }, listeningMs)

    val playedSongs = counts.mapNotNull { (id, count) -> songsById[id]?.let { SongStat(it, count, listeningMs[id] ?: 0L) } }
    val listensOutsideLibrary = listens.filter { it.songId == null || it.songId !in songsById }

    val topArtists = (playedSongs.flatMap { stat ->
        stat.song.artist.splitArtists().map { ArtistStat(it, stat.playCount, stat.listeningMs) }
    } + listensOutsideLibrary.flatMap { listen -> listen.artist.splitArtists().map { ArtistStat(it, if (listen.counted) 1 else 0, listen.ms) } })
        .groupBy { it.artist }
        .map { (artist, entries) -> ArtistStat(artist, entries.sumOf { it.playCount }, entries.sumOf { it.listeningMs }) }
        .filter { it.playCount > 0 }
        .sortedWith(compareByDescending<ArtistStat> { it.playCount }.thenByDescending { it.listeningMs })

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
        .sortedWith(compareByDescending<AlbumStat> { it.playCount }.thenByDescending { it.listeningMs })

    val topSongs = playedSongs.sortedWith(compareByDescending<SongStat> { it.playCount }.thenByDescending { it.listeningMs })

    val topGenres = (playedSongs.mapNotNull { stat -> stat.song.genres.firstOrNull()?.let { GenreStat(it, stat.playCount, stat.listeningMs) } } +
        listensOutsideLibrary.filter { it.genre.isNotBlank() }.map { GenreStat(it.genre, if (it.counted) 1 else 0, it.ms) })
        .groupBy { it.genre.trim().lowercase() }
        .map { (_, entries) -> GenreStat(entries.first().genre.trim(), entries.sumOf { it.playCount }, entries.sumOf { it.listeningMs }) }
        .filter { it.playCount > 0 }
        .sortedWith(compareByDescending<GenreStat> { it.playCount }.thenByDescending { it.listeningMs })

    val history = listens.filter { it.counted }.sortedByDescending { it.at }
    return PeriodRecord(stats, MonthComputed(topArtists, topAlbums, topSongs, topGenres, history))
}

/** The short lists the Overview and Rewind show, before "View all". */
internal fun MonthComputed.summary() = copy(
    topArtists = topArtists.take(10),
    topAlbums = topAlbums.take(10),
    topGenres = topGenres.take(5),
    recentListens = recentListens.take(20)
)

/** Listening time per month of [year], as bars; [selected] is highlighted and tapping a month picks it. */
@Composable
internal fun MonthlyListeningChart(
    year: Int,
    monthlyStats: Map<String, MonthlyStats>,
    selected: YearMonth?,
    onSelect: ((YearMonth) -> Unit)?
) {
    val locale = LocalLocale.current.platformLocale
    val months = Month.entries.map { YearMonth.of(year, it) }
    val minutes = months.map { (monthlyStats[it.toString()]?.listeningMs ?: 0L) / 60_000 }
    val most = minutes.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            stringResource(AppR.string.stats_listening_by_month, year),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEachIndexed { index, month ->
                val hasListening = minutes[index] > 0
                val isSelected = month == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(if (onSelect != null && hasListening) Modifier.tapScale { onSelect(month) } else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .fillMaxHeight(if (hasListening) (minutes[index].toFloat() / most).coerceAtLeast(0.04f) else 0.02f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.tertiary
                                    hasListening -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        month.month.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** A numbered row with artwork, for the full Top Tracks, Artists and Albums lists. */
@Composable
internal fun StatRankRow(
    rank: Int,
    title: String,
    detail: String,
    playCount: Int,
    listeningMs: Long,
    onClick: () -> Unit,
    art: @Composable (Modifier) -> Unit,
    artShape: RoundedCornerShape = RoundedCornerShape(6.dp)
) {
    Row(
        modifier = Modifier.fillMaxWidth().tapScale(onClick).padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rank.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = IbmPlexMonoFontFamily),
            color = if (rank <= 3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Box(modifier = Modifier.size(48.dp).clip(artShape)) { art(Modifier.fillMaxSize()) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                pluralStringResource(AppR.plurals.stats_plays, playCount, playCount),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            formatRecordedMinutes(listeningMs)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun TopTracksTab(songs: List<SongStat>, onPlay: (SongStat) -> Unit, listBottomPadding: Dp) {
    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = listBottomPadding + 24.dp)) {
        itemsIndexed(songs, key = { _, stat -> stat.song.id }) { index, stat ->
            StatRankRow(
                rank = index + 1,
                title = stat.song.title,
                detail = stat.song.artist,
                playCount = stat.playCount,
                listeningMs = stat.listeningMs,
                onClick = { onPlay(stat) },
                art = { modifier -> AlbumArt(song = stat.song, modifier = modifier) }
            )
        }
    }
}

@Composable
internal fun TopArtistsTab(artists: List<ArtistStat>, onOpen: (String) -> Unit, listBottomPadding: Dp) {
    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = listBottomPadding + 24.dp)) {
        itemsIndexed(artists, key = { _, stat -> "artist:${stat.artist}" }) { index, stat ->
            StatRankRow(
                rank = index + 1,
                title = stat.artist,
                detail = "",
                playCount = stat.playCount,
                listeningMs = stat.listeningMs,
                onClick = { onOpen(stat.artist) },
                art = { modifier -> ArtistImage(artist = stat.artist, modifier = modifier) },
                artShape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
internal fun TopAlbumsTab(albums: List<AlbumStat>, onOpen: (String) -> Unit, listBottomPadding: Dp) {
    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = listBottomPadding + 24.dp)) {
        itemsIndexed(albums, key = { _, stat -> "album:${stat.albumId}" }) { index, stat ->
            StatRankRow(
                rank = index + 1,
                title = stat.album,
                detail = stat.artSong.artist,
                playCount = stat.playCount,
                listeningMs = stat.listeningMs,
                onClick = { onOpen(stat.albumId) },
                art = { modifier -> AlbumArt(song = stat.artSong, modifier = modifier) }
            )
        }
    }
}

@Composable
internal fun HistoryTab(listens: List<Listen>, onPlay: (Listen) -> Unit, listBottomPadding: Dp) {
    val locale = LocalLocale.current.platformLocale
    val format = DateTimeFormatter.ofPattern("MMM d · HH:mm", locale)
    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = listBottomPadding + 24.dp)) {
        var lastMonth: String? = null
        listens.forEachIndexed { index, listen ->
            if (listen.monthKey != lastMonth) {
                lastMonth = listen.monthKey
                val month = YearMonth.parse(listen.monthKey)
                item(key = "month:${listen.monthKey}") {
                    SectionHeader(month.month.getDisplayName(TextStyle.FULL, locale) + " " + month.year)
                }
            }
            item(key = "listen:$index:${listen.at}") {
                StatTextRow(
                    title = listen.title,
                    detail = listen.artist,
                    trailing = format.format(Instant.ofEpochMilli(listen.at).atZone(ZoneId.systemDefault())),
                    onClick = { onPlay(listen) }
                )
            }
        }
    }
}
