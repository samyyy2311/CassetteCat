package `in`.caffeinelabs.cassettecat.ui.screens.stats

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.stats.Milestone
import `in`.caffeinelabs.cassettecat.data.stats.MilestoneType
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal fun formatListeningTime(minutes: Long): String = when {
    minutes >= 60 -> "%02dH %02dM".format(Locale.US, minutes / 60, minutes % 60)
    else -> "%02d MIN".format(Locale.US, minutes)
}

internal fun formatRecordedMinutes(listeningMs: Long): String? = when {
    listeningMs < 1_000L -> null
    listeningMs < 60_000L -> "< 1 MIN"
    else -> "${listeningMs / 60_000} MIN"
}

internal fun isSameMonth(epochMs: Long, month: YearMonth): Boolean =
    YearMonth.from(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())) == month

@Composable
internal fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
internal fun YearSelector(years: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
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
internal fun MonthTabs(
    months: List<YearMonth>,
    selected: YearMonth?,
    isRewindSelected: Boolean,
    onSelectMonth: (YearMonth) -> Unit,
    onSelectRewind: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (months.size > 1) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.tapScale(onSelectRewind)
                ) {
                    Text(
                        "REWIND",
                        style = if (isRewindSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        color = if (isRewindSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .background(
                                if (isRewindSelected) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
        items(months.sorted()) { month ->
            val isSelected = !isRewindSelected && month == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.tapScale { onSelectMonth(month) }
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
internal fun ListeningRecordReadout(
    month: YearMonth?,
    year: Int,
    isRewind: Boolean,
    listeningMinutes: Long,
    totalPlays: Int,
    uniqueSongs: Int
) {
    val subtitle = if (isRewind) "CASSETTE REWIND // ANNUAL RECAP $year" else "RECORDED // ${month?.month?.getDisplayName(TextStyle.SHORT, Locale.US)?.uppercase(Locale.US) ?: ""} $year"
    val description = if (listeningMinutes == 0L) {
        "No listening time has been recorded yet."
    } else if (isRewind) {
        "Total listening time for $year"
    } else {
        "Listening time for this month"
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            subtitle,
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
            description,
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
internal fun RecordMetric(label: String, value: String, modifier: Modifier = Modifier) {
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

@Composable
internal fun RankedCard(
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
internal fun StatSongRow(song: Song, count: Int, onClick: () -> Unit) {
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
internal fun MostPlayedTracksScreen(
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
internal fun MilestoneRow(milestone: Milestone) {
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

internal fun androidx.compose.foundation.lazy.LazyListScope.statsSections(
    computed: MonthComputed,
    month: YearMonth,
    monthMilestones: List<Milestone>,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onPlayTrack: (SongStat) -> Unit,
    onViewAllMostPlayed: () -> Unit,
    onSavePlaylist: () -> Unit
) {
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
                        art = { artModifier -> `in`.caffeinelabs.cassettecat.ui.components.ArtistImage(artist = artist.artist, modifier = artModifier) }
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
                onClick = { onPlayTrack(stat) }
            )
        }
        item {
            if (computed.topSongs.size > 5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tapScale(onViewAllMostPlayed)
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
                    .tapScale(onSavePlaylist)
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
