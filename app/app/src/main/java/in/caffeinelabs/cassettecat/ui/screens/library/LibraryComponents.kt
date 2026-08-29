package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.PlaylistSuggestion
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.TrackRowDensity
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.LocalAppPreferences
import `in`.caffeinelabs.cassettecat.ui.components.DownloadStatusIcon
import `in`.caffeinelabs.cassettecat.ui.components.PlaylistCoverArt
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import `in`.caffeinelabs.cassettecat.ui.util.tapScaleSelectable

internal fun formatPlaylistDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
}

@Composable
fun rememberSkeletonColor(): Color {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
}

@Composable
fun SongRowSkeleton() {
    val color = rememberSkeletonColor()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(color))
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Box(Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

@Composable
internal fun GridCardSkeleton() {
    val color = rememberSkeletonColor()
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(color))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(70.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
internal fun RowScope.SongListRowContent(
    song: Song,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onMoreClick: (() -> Unit)? = null
) {
    val preferences = LocalAppPreferences.current
    val isCompact = preferences.trackRowDensity == TrackRowDensity.COMPACT
    val artSize = if (isCompact) 42.dp else 48.dp

    val audioFormat = remember(song.filePath, song.source) {
        val ext = song.filePath?.substringAfterLast('.', "")?.uppercase().orEmpty()
        when (ext) {
            "FLAC", "WAV", "ALAC" -> "FLAC"
            "MP3" -> "MP3"
            "M4A", "AAC" -> "AAC"
            "OGG", "OPUS" -> "OPUS"
            else -> if (song.source != MusicSource.Local) "STREAM" else null
        }
    }

    val durationText = remember(song.durationMs) {
        if (song.durationMs > 0) {
            val totalSeconds = song.durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "%d:%02d".format(minutes, seconds)
        } else ""
    }

    Box(
        modifier = Modifier
            .size(artSize)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        if (isCurrentSong) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.lucide_ic_audio_lines else R.drawable.lucide_ic_play),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    Spacer(Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = song.title,
            style = (if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge).copy(
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isCurrentSong) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (song.album.isNotEmpty() && song.album != song.title) "${song.artist} • ${song.album}" else song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    // Trailing metadata section (Format pill, Duration, Download icon, More button)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        if (preferences.showAudioQualityBadge && audioFormat != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = audioFormat,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 9.sp
                    ),
                    color = if (audioFormat == "FLAC") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        if (durationText.isNotEmpty()) {
            Text(
                text = durationText,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = if (isCurrentSong) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DownloadStatusIcon(song = song, modifier = Modifier.padding(start = 6.dp))

        if (onMoreClick != null) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_ellipsis_vertical,
                contentDescription = "Song options",
                onClick = onMoreClick,
                modifier = Modifier.padding(start = 4.dp).size(30.dp)
            )
        }
    }
}

@Composable
fun LibrarySongRow(
    song: Song,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onMoreClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongListRowContent(
                song = song,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying,
                onMoreClick = onMoreClick
            )
        }
    }
}

@Composable
fun SongRow(
    song: Song,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onMoreClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = LibrarySongRow(
    song = song,
    isCurrentSong = isCurrentSong,
    isPlaying = isPlaying,
    onMoreClick = onMoreClick,
    onClick = onClick
)

@Composable
internal fun SelectionCheckboxIcon(selected: Boolean, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(
            if (selected) R.drawable.lucide_ic_square_check_big else R.drawable.lucide_ic_square
        ),
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
internal fun BoxScope.SelectionOverlay(selected: Boolean) {
    if (!selected) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f))
    )
    Icon(
        painter = painterResource(R.drawable.lucide_ic_square_check_big),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onTertiary,
        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp)
    )
}

@Composable
internal fun SelectableSongRow(
    song: Song,
    selected: Boolean,
    selectionMode: Boolean,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onMoreClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tapScaleSelectable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                SelectionCheckboxIcon(selected)
                Spacer(Modifier.width(14.dp))
            }
            SongListRowContent(
                song = song,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying,
                onMoreClick = onMoreClick
            )
        }
    }
}

@Composable
internal fun SongGridCard(
    song: Song,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapScaleSelectable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            SelectionOverlay(selected)
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
internal fun AlbumCard(
    group: AlbumGroup,
    onClick: () -> Unit,
    selected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val year = group.songs.firstOrNull()?.releaseYear
    Column(modifier = Modifier.fillMaxWidth().tapScaleSelectable(onClick, onLongClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
        ) {
            AlbumArt(song = group.songs.first(), modifier = Modifier.fillMaxSize())
            if (year != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        year.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 9.sp),
                        color = Color.White
                    )
                }
            }
            SelectionOverlay(selected)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            group.album,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            group.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
internal fun ArtistCard(
    group: ArtistGroup,
    onClick: () -> Unit,
    selected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().tapScaleSelectable(onClick, onLongClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            ArtistImage(artist = group.artist, modifier = Modifier.fillMaxSize())
            SelectionOverlay(selected)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            group.artist,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
internal fun GenreCard(
    group: GenreGroup,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    selected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val rule = genreRuleFor(group.genre)
    val color = rule.color
    val iconRes = rule.iconRes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = 0.72f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .tapScaleSelectable(onClick, onLongClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.72f)
        ) {
            Text(
                text = group.genre,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = Color.White.copy(alpha = 0.82f)
            )
        }

        Icon(
            painter = painterResource(iconRes),
            contentDescription = "Play",
            tint = Color.White.copy(alpha = 0.88f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(34.dp)
                .tapScale(onPlay)
        )

        SelectionOverlay(selected)
    }
}

@Composable
internal fun FolderCard(
    group: FolderGroup,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    selected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .tapScaleSelectable(onClick, onLongClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.72f)
        ) {
            Text(
                text = group.folderName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            painter = painterResource(R.drawable.lucide_ic_folder),
            contentDescription = "Play",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(34.dp)
                .tapScale(onPlay)
        )

        SelectionOverlay(selected)
    }
}

@Composable
internal fun CollectionListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    artwork: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onLongClick != null) it.tapScaleSelectable(onClick, onLongClick) else it.tapScale(onClick) }
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                SelectionCheckboxIcon(selected)
                Spacer(Modifier.width(16.dp))
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                artwork()
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onPlay != null) {
                Spacer(Modifier.width(8.dp))
                TransportButton(
                    iconRes = R.drawable.lucide_ic_play,
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    accented = true,
                    onClick = onPlay
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
            modifier = Modifier.padding(start = 96.dp)
        )
    }
}

@Composable
internal fun ArtistListRow(
    group: ArtistGroup,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false
) {
    CollectionListRow(
        title = group.artist,
        subtitle = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        selectionMode = selectionMode
    ) {
        ArtistImage(artist = group.artist, modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun AlbumListRow(
    group: AlbumGroup,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false
) {
    CollectionListRow(
        title = group.album,
        subtitle = "${group.artist} · ${if (group.songs.size == 1) "1 song" else "${group.songs.size} songs"}",
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        selectionMode = selectionMode
    ) {
        AlbumArt(song = group.songs.first(), modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun GenreListRow(
    group: GenreGroup,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false
) {
    val rule = genreRuleFor(group.genre)
    CollectionListRow(
        title = group.genre,
        subtitle = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
        onClick = onClick,
        onPlay = onPlay,
        onLongClick = onLongClick,
        selected = selected,
        selectionMode = selectionMode
    ) {
        Icon(
            painter = painterResource(rule.iconRes),
            contentDescription = null,
            tint = rule.color,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
internal fun FolderListRow(
    group: FolderGroup,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false
) {
    CollectionListRow(
        title = group.folderName,
        subtitle = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
        onClick = onClick,
        onPlay = onPlay,
        onLongClick = onLongClick,
        selected = selected,
        selectionMode = selectionMode
    ) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_folder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
internal fun LikedSongsCard(songs: List<Song>, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_heart),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(44.dp)
            )

            if (songs.isNotEmpty()) {
                TransportButton(
                    iconRes = R.drawable.lucide_ic_play,
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    accented = true,
                    onClick = onPlay,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Liked Songs", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
        Text(
            if (songs.size == 1) "1 song" else "${songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun PlaylistCard(
    playlist: Playlist,
    songs: List<Song>,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    selected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().tapScaleSelectable(onClick, onLongClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            PlaylistCoverArt(playlist = playlist, fallbackSong = songs.firstOrNull(), modifier = Modifier.fillMaxSize())
            if (songs.isNotEmpty()) {
                TransportButton(
                    iconRes = R.drawable.lucide_ic_play,
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    accented = true,
                    onClick = onPlay,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                )
            }
            SelectionOverlay(selected)
        }
        Spacer(Modifier.height(8.dp))
        Text(playlist.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (songs.size == 1) "1 song" else "${songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun PlaylistGrid(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    likedSongs: List<Song>,
    listBottomPadding: Dp,
    onClick: (String) -> Unit,
    onPlay: (List<Song>) -> Unit,
    onOpenLikedSongs: () -> Unit,
    onOpenSmartPlaylist: (SmartPlaylistType) -> Unit,
    suggestions: List<PlaylistSuggestion> = emptyList(),
    onOpenSuggestion: (PlaylistSuggestion) -> Unit = {},
    selectedIds: Set<String> = emptySet(),
    selectionMode: Boolean = false,
    onToggleSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (suggestions.isNotEmpty() && !selectionMode) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }, key = "suggested-mixtapes-shelf") {
                SuggestedMixtapesShelf(
                    suggestions = suggestions,
                    onOpenSuggestion = onOpenSuggestion
                )
            }
        }
        item(key = "liked-songs") {
            LikedSongsCard(
                songs = likedSongs,
                onClick = { if (!selectionMode) onOpenLikedSongs() },
                onPlay = { if (likedSongs.isNotEmpty()) onPlay(likedSongs) }
            )
        }
        items(SmartPlaylistType.entries.toTypedArray(), key = { "smart-${it.id}" }) { type ->
            SmartPlaylistCard(
                type = type,
                onClick = { if (!selectionMode) onOpenSmartPlaylist(type) }
            )
        }
        items(playlists, key = { it.id }) { playlist ->
            val songs = remember(playlist.songIds, allSongs) {
                playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
            }
            PlaylistCard(
                playlist = playlist,
                songs = songs,
                selected = playlist.id in selectedIds,
                onClick = { if (selectionMode) onToggleSelect(playlist.id) else onClick(playlist.id) },
                onLongClick = { onToggleSelect(playlist.id) },
                onPlay = { if (songs.isNotEmpty()) onPlay(songs) }
            )
        }
    }
}

@Composable
internal fun SmartPlaylistCard(
    type: SmartPlaylistType,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = type.gradient,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(type.iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(type.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            type.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun PlaylistList(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    likedSongs: List<Song>,
    state: androidx.compose.foundation.lazy.LazyListState,
    listBottomPadding: Dp,
    onClick: (String) -> Unit,
    onPlay: (List<Song>) -> Unit,
    onOpenLikedSongs: () -> Unit,
    onOpenSmartPlaylist: (SmartPlaylistType) -> Unit,
    suggestions: List<PlaylistSuggestion> = emptyList(),
    onOpenSuggestion: (PlaylistSuggestion) -> Unit = {},
    selectedIds: Set<String> = emptySet(),
    selectionMode: Boolean = false,
    onToggleSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
    ) {
        if (suggestions.isNotEmpty() && !selectionMode) {
            item(key = "suggested-mixtapes-shelf") {
                SuggestedMixtapesShelf(
                    suggestions = suggestions,
                    onOpenSuggestion = onOpenSuggestion,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        item(key = "liked-songs") {
            CollectionListRow(
                title = "Liked Songs",
                subtitle = if (likedSongs.size == 1) "1 song" else "${likedSongs.size} songs",
                onClick = { if (!selectionMode) onOpenLikedSongs() },
                onPlay = { if (likedSongs.isNotEmpty()) onPlay(likedSongs) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_heart),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        items(SmartPlaylistType.entries.toTypedArray(), key = { "smart-${it.id}" }) { type ->
            CollectionListRow(
                title = type.title,
                subtitle = type.description,
                onClick = { if (!selectionMode) onOpenSmartPlaylist(type) },
                onPlay = { onOpenSmartPlaylist(type) }
            ) {
                Icon(
                    painter = painterResource(type.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        items(playlists, key = { it.id }) { playlist ->
            val songs = remember(playlist.songIds, allSongs) {
                playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
            }
            CollectionListRow(
                title = playlist.name,
                subtitle = if (songs.size == 1) "1 song" else "${songs.size} songs",
                onClick = { if (selectionMode) onToggleSelect(playlist.id) else onClick(playlist.id) },
                onPlay = { if (songs.isNotEmpty()) onPlay(songs) },
                onLongClick = { onToggleSelect(playlist.id) },
                selected = playlist.id in selectedIds,
                selectionMode = selectionMode
            ) {
                PlaylistCoverArt(
                    playlist = playlist,
                    fallbackSong = songs.firstOrNull(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
internal fun SourceWarningBanner(warnings: List<String>) {
    var dismissed by remember(warnings) { mutableStateOf(false) }
    if (dismissed) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_triangle_alert),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            warnings.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_x),
            contentDescription = "Dismiss",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = hapticClick { dismissed = true })
        )
    }
}

@Composable
internal fun SuggestedMixtapesShelf(
    suggestions: List<PlaylistSuggestion>,
    onOpenSuggestion: (PlaylistSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_sparkles),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "SUGGESTED MIXTAPES",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(suggestions, key = { it.id }) { suggestion ->
                SuggestedMixtapeChip(
                    suggestion = suggestion,
                    onClick = { onOpenSuggestion(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun SuggestedMixtapeChip(
    suggestion: PlaylistSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .tapScale(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(suggestion.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                suggestion.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
            Text(
                "${suggestion.songs.size} tracks · ${suggestion.category.label}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

