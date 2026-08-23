package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.TrackRowDensity
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.DownloadStatusIcon
import `in`.caffeinelabs.cassettecat.ui.components.PlaylistCoverArt
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import `in`.caffeinelabs.cassettecat.ui.util.tapScaleSelectable

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
internal fun RowScope.SongListRowContent(song: Song) {
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val isCompact = preferences.trackRowDensity == TrackRowDensity.COMPACT
    val artSize = if (isCompact) 40.dp else 48.dp

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

    Box(modifier = Modifier.size(artSize).clip(RoundedCornerShape(6.dp))) {
        AlbumArt(song = song, modifier = Modifier.fillMaxSize())
    }
    Spacer(Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
        Text(
            song.title,
            style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (preferences.showAudioQualityBadge && audioFormat != null) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        audioFormat,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                        color = if (audioFormat == "FLAC") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    DownloadStatusIcon(song = song, modifier = Modifier.padding(start = 8.dp))
}

@Composable
fun LibrarySongRow(song: Song, onClick: () -> Unit) {
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
            SongListRowContent(song)
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 64.dp)
        )
    }
}

@Composable
fun SongRow(song: Song, onClick: () -> Unit) = LibrarySongRow(song, onClick)

@Composable
internal fun SelectableSongRow(
    song: Song,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapScaleSelectable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 24.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Icon(
                    painter = painterResource(
                        if (selected) R.drawable.lucide_ic_square_check_big else R.drawable.lucide_ic_square
                    ),
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(16.dp))
            }
            SongListRowContent(song)
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = if (selectionMode) 96.dp else 64.dp)
        )
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
            if (selected) {
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
internal fun AlbumCard(group: AlbumGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            AlbumArt(song = group.songs.first(), modifier = Modifier.fillMaxSize())
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 36.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(group.album, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            group.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ArtistCard(group: ArtistGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            ArtistImage(artist = group.artist, modifier = Modifier.fillMaxSize())
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 36.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(group.artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun GenreCard(group: GenreGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    val rule = genreRuleFor(group.genre)
    val accent = rule?.color ?: MaterialTheme.colorScheme.tertiary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        accent.copy(alpha = 0.22f)
                    )
                )
            )
            .tapScale(onClick)
    ) {
        Icon(
            painter = painterResource(rule?.iconRes ?: R.drawable.lucide_ic_cassette_tape),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.align(Alignment.TopStart).padding(18.dp).size(34.dp)
        )
        TransportButton(
            iconRes = R.drawable.lucide_ic_play,
            size = 40.dp,
            tint = accent,
            accented = true,
            onClick = onPlay,
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp, 0.dp, 62.dp, 18.dp)) {
            Text(
                group.genre,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
            )
        }
    }
}

@Composable
internal fun CollectionListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    artwork: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tapScale(onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Spacer(Modifier.width(8.dp))
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 36.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = onPlay
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
            modifier = Modifier.padding(start = 96.dp)
        )
    }
}

@Composable
internal fun ArtistListRow(group: ArtistGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    CollectionListRow(
        title = group.artist,
        subtitle = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
        onClick = onClick,
        onPlay = onPlay
    ) {
        ArtistImage(artist = group.artist, modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun AlbumListRow(group: AlbumGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    CollectionListRow(
        title = group.album,
        subtitle = "${group.artist} · ${if (group.songs.size == 1) "1 song" else "${group.songs.size} songs"}",
        onClick = onClick,
        onPlay = onPlay
    ) {
        AlbumArt(song = group.songs.first(), modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun GenreListRow(group: GenreGroup, onClick: () -> Unit, onPlay: () -> Unit) {
    val rule = genreRuleFor(group.genre)
    CollectionListRow(
        title = group.genre,
        subtitle = if (group.songs.size == 1) "1 song" else "${group.songs.size} songs",
        onClick = onClick,
        onPlay = onPlay
    ) {
        Icon(
            painter = painterResource(rule?.iconRes ?: R.drawable.lucide_ic_cassette_tape),
            contentDescription = null,
            tint = rule?.color ?: MaterialTheme.colorScheme.tertiary,
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
                modifier = Modifier.size(52.dp)
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
        Text("Liked Songs", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        Text(
            if (songs.size == 1) "1 song" else "${songs.size} songs",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun PlaylistCard(playlist: Playlist, songs: List<Song>, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().tapScale(onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
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
        }
        Spacer(Modifier.height(8.dp))
        Text(playlist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = listBottomPadding + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "liked-songs") {
            LikedSongsCard(
                songs = likedSongs,
                onClick = onOpenLikedSongs,
                onPlay = { if (likedSongs.isNotEmpty()) onPlay(likedSongs) }
            )
        }
        items(SmartPlaylistType.entries.toTypedArray(), key = { "smart-${it.id}" }) { type ->
            SmartPlaylistCard(
                type = type,
                onClick = { onOpenSmartPlaylist(type) }
            )
        }
        items(playlists, key = { it.id }) { playlist ->
            val songs = remember(playlist.songIds, allSongs) {
                playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
            }
            PlaylistCard(
                playlist = playlist,
                songs = songs,
                onClick = { onClick(playlist.id) },
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
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(type.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(type.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            type.description,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
    ) {
        item(key = "liked-songs") {
            CollectionListRow(
                title = "Liked Songs",
                subtitle = if (likedSongs.size == 1) "1 song" else "${likedSongs.size} songs",
                onClick = onOpenLikedSongs,
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
                onClick = { onOpenSmartPlaylist(type) },
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
                onClick = { onClick(playlist.id) },
                onPlay = { if (songs.isNotEmpty()) onPlay(songs) }
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
