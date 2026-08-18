package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomRole
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.listeningroom.NearbyListeningRoom
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.PlaylistCoverArt
import `in`.caffeinelabs.cassettecat.ui.screens.library.splitArtists
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullOpenBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingActionsSheet(
    song: Song,
    isFavorite: Boolean,
    sleepTimerEndMs: Long?,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onOpenCredits: () -> Unit,
    onOpenOutputPicker: () -> Unit,
    onOpenListeningRoom: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenTagEditor: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sleepTimerSubtitle = if (sleepTimerEndMs != null) {
        val remainingMin = ((sleepTimerEndMs - SystemClock.elapsedRealtime()) / 60_000L).coerceAtLeast(0)
        if (remainingMin < 1) "Less than 1 min remaining" else "$remainingMin min remaining"
    } else {
        "Off"
    }

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))) {
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(16.dp))
                Column {
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
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_heart,
                label = if (isFavorite) "Unfavorite" else "Favorite",
                accented = isFavorite,
                onClick = { onToggleFavorite(); onDismiss() }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_share_2,
                label = "Share",
                accented = false,
                onClick = { onShare(); onDismiss() }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_list_plus,
                label = "Add to Up Next",
                subtitle = "Plays after the current queue",
                accented = false,
                onClick = { onAddToQueue(); onDismiss() }
            )
            if (song.source != MusicSource.Local) {
                SongActionRow(
                    iconRes = R.drawable.lucide_ic_download,
                    label = "Download",
                    accented = false,
                    onClick = { onDownload(); onDismiss() }
                )
            }
            if (song.source == MusicSource.Local && onOpenTagEditor != null) {
                SongActionRow(
                    iconRes = R.drawable.lucide_ic_sliders_horizontal,
                    label = "Edit Track Metadata",
                    subtitle = "Modify title, artist, album, and year tags",
                    accented = false,
                    onClick = { onOpenTagEditor(); onDismiss() }
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_book_open,
                label = "Credits & details",
                subtitle = "Release and source metadata",
                accented = false,
                onClick = { onOpenCredits(); onDismiss() }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_speaker,
                label = "Audio output",
                subtitle = "This phone",
                accented = false,
                onClick = { onOpenOutputPicker(); onDismiss() }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_radio,
                label = "Listening Room",
                subtitle = "Share playback on this Wi-Fi",
                accented = false,
                onClick = { onOpenListeningRoom(); onDismiss() }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_moon,
                label = "Sleep Timer",
                subtitle = sleepTimerSubtitle,
                accented = sleepTimerEndMs != null,
                onClick = { onOpenSleepTimer(); onDismiss() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListeningRoomSheet(
    state: ListeningRoomState,
    onStart: () -> Unit,
    onFindNearby: () -> Unit,
    onJoin: (NearbyListeningRoom) -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (state.role == ListeningRoomRole.NONE) onFindNearby()
    }
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(
                "Listening Room",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Text(
                "Share playback and the queue only with people on this Wi-Fi. No account, cloud service, analytics, or listening history is sent anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            when (state.role) {
                ListeningRoomRole.HOST -> {
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_radio,
                        label = state.roomName ?: "Listening Room",
                        subtitle = "Room controls are on this phone • ${state.participantCount} connected",
                        accented = true,
                        onClick = {}
                    )
                    state.roomCode?.let { code ->
                        Text(
                            "Room code: $code",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_x,
                        label = "End nearby room",
                        subtitle = "Disconnect everyone immediately",
                        accented = false,
                        onClick = { onLeave(); onDismiss() }
                    )
                }
                ListeningRoomRole.GUEST -> {
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_radio,
                        label = state.roomName ?: "Listening Room",
                        subtitle = "Following the host's playback and queue",
                        accented = true,
                        onClick = {}
                    )
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_x,
                        label = "Leave room",
                        subtitle = "Return to your own playback controls",
                        accented = false,
                        onClick = { onLeave(); onDismiss() }
                    )
                }
                ListeningRoomRole.NONE -> {
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_radio,
                        label = "Start a nearby room",
                        subtitle = "Host playback for people on this Wi-Fi",
                        accented = true,
                        onClick = onStart
                    )
                    if (state.nearbyRooms.isNotEmpty()) {
                        Text(
                            "Nearby rooms",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                        state.nearbyRooms.forEach { room ->
                            SongActionRow(
                                iconRes = R.drawable.lucide_ic_wifi,
                                label = room.name,
                                subtitle = "Join on this Wi-Fi",
                                accented = false,
                                onClick = { onJoin(room) }
                            )
                        }
                    } else {
                        Text(
                            state.notice ?: "Looking for nearby rooms on this Wi-Fi…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongCreditsSheet(song: Song, onDismiss: () -> Unit) {
    val source = when (song.source) {
        MusicSource.Local -> "On-device library"
        MusicSource.Subsonic -> "Subsonic server"
        MusicSource.Jellyfin -> "Jellyfin server"
    }
    val genre = song.genres.filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Not supplied" }

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) {
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
            Text("Credits & details", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(12.dp))
            CreditDetailRow("Performing artist", song.artist)
            CreditDetailRow("Release", song.album.ifBlank { "Unknown release" })
            CreditDetailRow("Year", song.releaseYear?.toString() ?: "Not supplied")
            CreditDetailRow("Genre", genre)
            CreditDetailRow("Source", source)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            Text(
                "Writer, producer, and engineer credits are shown when the active music source supplies them. This track did not include those fields.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun CreditDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 7.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioOutputSheet(onOpenBluetoothSettings: () -> Unit, onDismiss: () -> Unit) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text("Audio output", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            SongActionRow(
                iconRes = R.drawable.lucide_ic_speaker,
                label = "This phone",
                subtitle = "Currently selected",
                accented = true,
                onClick = onDismiss
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_speaker,
                label = "Bluetooth devices",
                subtitle = "Choose or pair a playback device",
                accented = false,
                onClick = onOpenBluetoothSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScreenshotSuggestionSheet(
    song: Song,
    onShare: () -> Unit,
    onViewCredits: () -> Unit,
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text("Screenshot captured", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            Text(
                "Want to keep the song details with it?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            SongActionRow(
                iconRes = R.drawable.lucide_ic_share_2,
                label = "Share ${song.title}",
                subtitle = song.artist,
                accented = true,
                onClick = onShare
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_book_open,
                label = "View credits & details",
                accented = false,
                onClick = onViewCredits
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingGoToSheet(
    song: Song,
    playlists: List<Playlist>,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onOpenPlaylistPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryArtist = song.artist.splitArtists().firstOrNull() ?: song.artist
    val matchingPlaylist = playlists.firstOrNull { song.id in it.songIds }

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))) {
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
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
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            GoToMusicDetailRow(
                title = "Go to Artist",
                subtitle = primaryArtist,
                onClick = { onNavigateToArtist(primaryArtist) }
            ) {
                ArtistImage(artist = primaryArtist, modifier = Modifier.fillMaxSize())
            }
            GoToMusicDetailRow(
                title = "Go to Album",
                subtitle = song.album,
                onClick = { onNavigateToAlbum(song.albumId) }
            ) {
                AlbumArt(song = song, modifier = Modifier.fillMaxSize())
            }
            GoToMusicDetailRow(
                title = "Go to Playlist",
                subtitle = matchingPlaylist?.name ?: if (playlists.isEmpty()) "No saved playlists yet" else "Choose a playlist",
                onClick = {
                    if (matchingPlaylist != null) onNavigateToPlaylist(matchingPlaylist.id)
                    else onOpenPlaylistPicker()
                }
            ) {
                if (matchingPlaylist != null) {
                    PlaylistCoverArt(
                        playlist = matchingPlaylist,
                        fallbackSong = song,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_list_music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingPlaylistPicker(
    playlists: List<Playlist>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Go to Playlist",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            if (playlists.isEmpty()) {
                Text(
                    "No saved playlists yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            } else {
                playlists.forEach { playlist ->
                    Row(
                        modifier = Modifier.fillMaxWidth().tapScale { onSelect(playlist.id) }.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))) {
                            PlaylistCoverArt(playlist = playlist, fallbackSong = null, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (playlist.songIds.size == 1) "1 song" else "${playlist.songIds.size} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoToMusicDetailRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))) { artwork() }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SongActionRow(
    iconRes: Int,
    label: String,
    accented: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    val tint = if (accented) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (accented) tint else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val SLEEP_TIMER_OPTIONS_MIN = listOf(5, 10, 15, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepTimerPickerSheet(
    currentEndMs: Long?,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Sleep Timer",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            if (currentEndMs != null) {
                SongActionRow(
                    iconRes = R.drawable.lucide_ic_timer_off,
                    label = "Turn Off",
                    accented = false,
                    onClick = onCancel
                )
            }
            SLEEP_TIMER_OPTIONS_MIN.forEach { minutes ->
                SongActionRow(
                    iconRes = R.drawable.lucide_ic_moon,
                    label = "$minutes minutes",
                    accented = false,
                    onClick = { onSelect(minutes * 60_000L) }
                )
            }
        }
    }
}
