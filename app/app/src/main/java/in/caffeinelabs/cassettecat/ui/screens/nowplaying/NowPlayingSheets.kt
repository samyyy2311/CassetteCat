package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomRole
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.listeningroom.NearbyListeningRoom
import `in`.caffeinelabs.cassettecat.data.listeningroom.statusSubtitle
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.ArtistImage
import `in`.caffeinelabs.cassettecat.ui.components.PlaylistCoverArt
import `in`.caffeinelabs.cassettecat.ui.screens.library.splitArtists
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.rememberConnectedBluetoothDevice
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlin.math.abs
import kotlin.math.roundToInt

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
    listeningRoomState: ListeningRoomState,
    playbackSpeed: Float,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onShareFile: () -> Unit,
    onAddToQueue: () -> Unit,
    onStartInstantMix: () -> Unit,
    onDownload: () -> Unit,
    onOpenCredits: () -> Unit,
    onOpenOutputPicker: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenTagEditor: () -> Unit = {},
    onOpenListeningRoom: () -> Unit,
    onOpenPlaybackSpeed: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenDriveMode: () -> Unit = {},
    onSearchCoverOnline: () -> Unit = {},
    onOpenArtworkViewer: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val btDevice = rememberConnectedBluetoothDevice()
    val isBtConnected = btDevice != null
    val outputLabel = if (isBtConnected) {
        btDevice.productName.toString().ifEmpty { stringResource(AppR.string.now_playing_output_bluetooth) }
    } else {
        stringResource(AppR.string.now_playing_output_phone_speaker)
    }
    val outputIcon = if (isBtConnected) R.drawable.lucide_ic_bluetooth else R.drawable.lucide_ic_speaker

    val sleepTimerSubtitle = if (sleepTimerEndMs != null) {
        val remainingMin = ((sleepTimerEndMs - SystemClock.elapsedRealtime()) / 60_000L).coerceAtLeast(0).toInt()
        if (remainingMin < 1) {
            stringResource(AppR.string.now_playing_timer_under_minute)
        } else {
            pluralStringResource(AppR.plurals.now_playing_timer_remaining, remainingMin, remainingMin)
        }
    } else {
        stringResource(AppR.string.now_playing_off)
    }

    val sourceBadge = when (song.source) {
        MusicSource.Local -> stringResource(AppR.string.now_playing_source_local)
        MusicSource.Subsonic -> stringResource(AppR.string.source_subsonic)
        MusicSource.Jellyfin -> stringResource(AppR.string.source_jellyfin)
        MusicSource.Radio -> if (song.bitrateKbps > 0) {
            stringResource(AppR.string.now_playing_source_radio_bitrate, song.bitrateKbps)
        } else {
            stringResource(AppR.string.source_radio)
        }
        MusicSource.ListeningRoomHost -> stringResource(AppR.string.source_room)
    }

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .tapScale {
                            onOpenArtworkViewer()
                            onDismiss()
                        }
                ) {
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = sourceBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    iconRes = R.drawable.lucide_ic_heart,
                    label = stringResource(
                        if (isFavorite) AppR.string.now_playing_liked else AppR.string.now_playing_favorite
                    ),
                    accented = isFavorite,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleFavorite
                )
                QuickActionButton(
                    iconRes = R.drawable.lucide_ic_share_2,
                    label = stringResource(AppR.string.now_playing_share),
                    accented = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onShare()
                        onDismiss()
                    }
                )
                if (song.source == MusicSource.Radio) {
                    QuickActionButton(
                        iconRes = R.drawable.lucide_ic_sliders_horizontal,
                        label = stringResource(AppR.string.now_playing_equalizer),
                        accented = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onOpenEqualizer()
                            onDismiss()
                        }
                    )
                    QuickActionButton(
                        iconRes = R.drawable.lucide_ic_timer,
                        label = stringResource(AppR.string.now_playing_sleep),
                        accented = sleepTimerEndMs != null,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onOpenSleepTimer()
                            onDismiss()
                        }
                    )
                } else {
                    QuickActionButton(
                        iconRes = R.drawable.lucide_ic_list_plus,
                        label = stringResource(AppR.string.now_playing_up_next),
                        accented = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onAddToQueue()
                            onDismiss()
                        }
                    )
                    if (song.source != MusicSource.ListeningRoomHost) {
                        QuickActionButton(
                            iconRes = R.drawable.lucide_ic_audio_lines,
                            label = stringResource(AppR.string.now_playing_mix),
                            accented = false,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onStartInstantMix()
                                onDismiss()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            if (song.source != MusicSource.ListeningRoomHost && song.source != MusicSource.Radio) {
                SongActionRow(
                    iconRes = R.drawable.lucide_ic_file_music,
                    label = stringResource(AppR.string.now_playing_share_song_file),
                    subtitle = stringResource(AppR.string.now_playing_send_original_audio),
                    onClick = {
                        onShareFile()
                        onDismiss()
                    }
                )
            }
            if (song.source != MusicSource.ListeningRoomHost && song.source != MusicSource.Radio) {
                SongActionRow(
                    iconRes = R.drawable.lucide_ic_pencil,
                    label = stringResource(AppR.string.now_playing_edit_details),
                    subtitle = stringResource(AppR.string.now_playing_edit_details_description),
                    onClick = {
                        onOpenTagEditor()
                        onDismiss()
                    }
                )
            }
            if (song.source != MusicSource.Local && song.source != MusicSource.ListeningRoomHost && song.source != MusicSource.Radio) {
                SongActionRow(
                    iconRes = R.drawable.lucide_ic_download,
                    label = stringResource(AppR.string.now_playing_download),
                    subtitle = stringResource(AppR.string.now_playing_download_description),
                    onClick = {
                        onDownload()
                        onDismiss()
                    }
                )
            }
            SongActionRow(
                iconRes = R.drawable.lucide_ic_book_open,
                label = stringResource(AppR.string.now_playing_credits_details),
                subtitle = stringResource(AppR.string.now_playing_credits_details_description),
                hasChevron = true,
                onClick = {
                    onOpenCredits()
                    onDismiss()
                }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_maximize_2,
                label = stringResource(AppR.string.now_playing_view_artwork),
                subtitle = stringResource(AppR.string.now_playing_view_artwork_description),
                onClick = {
                    onOpenArtworkViewer()
                    onDismiss()
                }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_image,
                label = stringResource(AppR.string.now_playing_search_cover),
                subtitle = stringResource(AppR.string.now_playing_search_cover_description),
                onClick = {
                    onSearchCoverOnline()
                    onDismiss()
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            SongActionRow(
                iconRes = outputIcon,
                label = stringResource(AppR.string.now_playing_audio_output),
                badgeText = outputLabel,
                accented = isBtConnected,
                hasChevron = true,
                onClick = {
                    onOpenOutputPicker()
                    onDismiss()
                }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_sliders_horizontal,
                label = stringResource(AppR.string.now_playing_equalizer),
                badgeText = stringResource(AppR.string.now_playing_eq_badge),
                hasChevron = true,
                onClick = {
                    onOpenEqualizer()
                    onDismiss()
                }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_gauge,
                label = stringResource(AppR.string.now_playing_playback_speed),
                badgeText = if (playbackSpeed == 1f) {
                    stringResource(AppR.string.now_playing_speed_normal_badge)
                } else {
                    stringResource(AppR.string.now_playing_speed_value, playbackSpeed.toString())
                },
                accented = playbackSpeed != 1f,
                hasChevron = true,
                onClick = {
                    onOpenPlaybackSpeed()
                    onDismiss()
                }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_users,
                label = stringResource(AppR.string.now_playing_listening_room),
                badgeText = if (listeningRoomState.role != ListeningRoomRole.NONE) {
                    stringResource(AppR.string.now_playing_active)
                } else {
                    null
                },
                subtitle = if (listeningRoomState.role == ListeningRoomRole.NONE) {
                    stringResource(AppR.string.now_playing_wifi_audio_sharing)
                } else {
                    listeningRoomState.statusSubtitle()
                },
                accented = listeningRoomState.role != ListeningRoomRole.NONE,
                hasChevron = true,
                onClick = {
                    onOpenListeningRoom()
                    onDismiss()
                }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_moon,
                label = stringResource(AppR.string.now_playing_sleep_timer),
                badgeText = sleepTimerSubtitle,
                accented = sleepTimerEndMs != null,
                hasChevron = true,
                onClick = {
                    onOpenSleepTimer()
                    onDismiss()
                }
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_car,
                label = stringResource(AppR.string.drive_mode_title),
                subtitle = stringResource(AppR.string.now_playing_drive_mode_description),
                hasChevron = true,
                onClick = {
                    onOpenDriveMode()
                    onDismiss()
                }
            )
        }
    }
}

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

@Composable
internal fun PlaybackSpeedSheet(
    currentSpeed: Float,
    currentPitch: Float = 1f,
    onSelectSpeed: (Float) -> Unit,
    onSelectPitch: (Float) -> Unit = {},
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                stringResource(AppR.string.playback_tuning_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            Text(
                stringResource(AppR.string.playback_speed_section),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PLAYBACK_SPEEDS) { speed ->
                    val isSelected = abs(speed - currentSpeed) < 0.01f
                    val bg = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    val fg = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(bg)
                            .border(if (isSelected) 1.dp else 0.5.dp, borderColor, RoundedCornerShape(100.dp))
                            .tapScale { onSelectSpeed(speed) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (speed == 1f) {
                                stringResource(AppR.string.playback_speed_normal)
                            } else {
                                stringResource(AppR.string.playback_speed_value, speed.toString())
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = fg
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(AppR.string.playback_pitch_section),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val semitones = (12 * (kotlin.math.ln(currentPitch.toDouble()) / kotlin.math.ln(2.0))).roundToInt()
                    Text(
                        stringResource(AppR.string.playback_pitch_multiplier),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (semitones == 0) {
                            stringResource(AppR.string.playback_pitch_normal)
                        } else {
                            pluralStringResource(
                                AppR.plurals.playback_pitch_semitones,
                                abs(semitones),
                                semitones,
                                currentPitch
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                        color = if (semitones != 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectPitch(1.0f) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = currentPitch,
                    onValueChange = onSelectPitch,
                    valueRange = 0.75f..1.25f,
                    steps = 10
                )
            }
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
    onJoinManual: (String) -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (state.role == ListeningRoomRole.NONE) onFindNearby()
    }
    val listeningRoomLabel = stringResource(AppR.string.now_playing_listening_room)
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(
                listeningRoomLabel,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Text(
                stringResource(AppR.string.listening_room_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            when (state.role) {
                ListeningRoomRole.HOST -> {
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_users,
                        label = state.roomName ?: listeningRoomLabel,
                        subtitle = pluralStringResource(
                            AppR.plurals.listening_room_host_status,
                            state.participantCount,
                            state.participantCount
                        ),
                        accented = true,
                        onClick = {}
                    )
                    state.roomCode?.let { code ->
                        Text(
                            stringResource(AppR.string.listening_room_code, code),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    state.hostAddress?.let { address ->
                        Text(
                            stringResource(AppR.string.listening_room_manual_address, address),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_x,
                        label = stringResource(AppR.string.listening_room_end),
                        subtitle = stringResource(AppR.string.listening_room_end_description),
                        accented = false,
                        onClick = {
                            onLeave()
                            onDismiss()
                        }
                    )
                }

                ListeningRoomRole.GUEST -> {
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_users,
                        label = state.roomName ?: listeningRoomLabel,
                        subtitle = stringResource(AppR.string.listening_room_following_host),
                        accented = true,
                        onClick = {}
                    )
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_x,
                        label = stringResource(AppR.string.listening_room_leave),
                        subtitle = stringResource(AppR.string.listening_room_leave_description),
                        accented = false,
                        onClick = {
                            onLeave()
                            onDismiss()
                        }
                    )
                }

                ListeningRoomRole.NONE -> {
                    SongActionRow(
                        iconRes = R.drawable.lucide_ic_users,
                        label = stringResource(AppR.string.listening_room_start),
                        subtitle = stringResource(AppR.string.listening_room_start_description),
                        accented = true,
                        onClick = onStart
                    )
                    state.notice?.let { notice ->
                        Text(
                            notice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    if (state.nearbyRooms.isNotEmpty()) {
                        Text(
                            stringResource(AppR.string.listening_room_nearby),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                        state.nearbyRooms.forEach { room ->
                            SongActionRow(
                                iconRes = R.drawable.lucide_ic_wifi,
                                label = room.name,
                                subtitle = stringResource(AppR.string.listening_room_join_wifi),
                                accented = false,
                                onClick = { onJoin(room) }
                            )
                        }
                    } else if (state.notice == null) {
                        Text(
                            stringResource(AppR.string.listening_room_searching),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    Text(
                        stringResource(AppR.string.listening_room_join_address),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp)
                    )
                    Text(
                        stringResource(AppR.string.listening_room_join_address_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                    )
                    var manualAddress by remember { mutableStateOf("") }
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        OutlinedTextField(
                            value = manualAddress,
                            onValueChange = { manualAddress = it },
                            placeholder = { Text("192.168.1.1:12345#ABC123") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onJoinManual(manualAddress) },
                            enabled = manualAddress.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(AppR.string.listening_room_join))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongCreditsSheet(song: Song, onDismiss: () -> Unit) {
    val notSupplied = stringResource(AppR.string.credits_not_supplied)
    val source = when (song.source) {
        MusicSource.Local -> stringResource(AppR.string.credits_source_local)
        MusicSource.Subsonic -> stringResource(AppR.string.credits_source_subsonic)
        MusicSource.Jellyfin -> stringResource(AppR.string.credits_source_jellyfin)
        MusicSource.ListeningRoomHost -> stringResource(AppR.string.credits_source_room)
        MusicSource.Radio -> stringResource(AppR.string.credits_source_radio)
    }
    val genre = song.genres.filter { it.isNotBlank() }.joinToString(" · ").ifBlank { notSupplied }
    val release = song.album.ifBlank { stringResource(AppR.string.credits_unknown_release) }

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) {
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
            Text(
                stringResource(AppR.string.now_playing_credits_details),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            CreditDetailRow(stringResource(AppR.string.credits_performing_artist), song.artist)
            CreditDetailRow(stringResource(AppR.string.credits_release), release)
            CreditDetailRow(stringResource(AppR.string.credits_year), song.releaseYear?.toString() ?: notSupplied)
            CreditDetailRow(stringResource(AppR.string.credits_genre), genre)
            CreditDetailRow(stringResource(AppR.string.credits_source), source)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Text(
                stringResource(AppR.string.credits_missing_fields),
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
    val btDevice = rememberConnectedBluetoothDevice()
    val isBtConnected = btDevice != null
    val btName = btDevice?.productName?.toString()?.ifEmpty {
        stringResource(AppR.string.audio_output_bluetooth_audio)
    } ?: stringResource(AppR.string.audio_output_bluetooth_device)

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(
                stringResource(AppR.string.now_playing_audio_output),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            SongActionRow(
                iconRes = R.drawable.lucide_ic_smartphone,
                label = stringResource(AppR.string.now_playing_output_phone_speaker),
                subtitle = if (!isBtConnected) {
                    stringResource(AppR.string.audio_output_active)
                } else {
                    stringResource(AppR.string.audio_output_phone_speaker_description)
                },
                accented = !isBtConnected,
                onClick = onDismiss
            )
            if (isBtConnected) {
                SongActionRow(
                    iconRes = R.drawable.lucide_ic_bluetooth,
                    label = btName,
                    subtitle = stringResource(AppR.string.audio_output_active),
                    accented = true,
                    onClick = onDismiss
                )
            }
            SongActionRow(
                iconRes = R.drawable.lucide_ic_bluetooth,
                label = if (isBtConnected) {
                    stringResource(AppR.string.audio_output_bluetooth_settings)
                } else {
                    stringResource(AppR.string.audio_output_connect_bluetooth)
                },
                subtitle = if (isBtConnected) {
                    stringResource(AppR.string.audio_output_manage_bluetooth)
                } else {
                    stringResource(AppR.string.audio_output_connect_bluetooth_description)
                },
                accented = false,
                onClick = onOpenBluetoothSettings
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
    onSearchCoverOnline: () -> Unit = {},
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
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            if (song.source != MusicSource.Radio) {
                GoToMusicDetailRow(
                    title = stringResource(AppR.string.go_to_artist),
                    subtitle = primaryArtist,
                    onClick = { onNavigateToArtist(primaryArtist) }
                ) {
                    ArtistImage(artist = primaryArtist, modifier = Modifier.fillMaxSize())
                }
                GoToMusicDetailRow(
                    title = stringResource(AppR.string.go_to_album),
                    subtitle = song.album,
                    onClick = { onNavigateToAlbum(song.albumId) }
                ) {
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
                }
                GoToMusicDetailRow(
                    title = stringResource(AppR.string.now_playing_search_cover_short),
                    subtitle = stringResource(AppR.string.now_playing_search_cover_description),
                    onClick = {
                        onSearchCoverOnline()
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_image),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            GoToMusicDetailRow(
                title = stringResource(AppR.string.go_to_playlist),
                subtitle = matchingPlaylist?.name ?: if (playlists.isEmpty()) {
                    stringResource(AppR.string.go_to_no_playlists)
                } else {
                    stringResource(AppR.string.go_to_choose_playlist)
                },
                onClick = {
                    if (matchingPlaylist != null) {
                        onNavigateToPlaylist(matchingPlaylist.id)
                    } else {
                        onOpenPlaylistPicker()
                    }
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
                stringResource(AppR.string.go_to_playlist),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            if (playlists.isEmpty()) {
                Text(
                    stringResource(AppR.string.go_to_no_playlists_sentence),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            } else {
                playlists.forEach { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tapScale { onSelect(playlist.id) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))) {
                            PlaylistCoverArt(
                                playlist = playlist,
                                fallbackSong = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                pluralStringResource(
                                    AppR.plurals.playlist_song_count,
                                    playlist.songIds.size,
                                    playlist.songIds.size
                                ),
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
private fun QuickActionButton(
    iconRes: Int,
    label: String,
    accented: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (accented) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (accented) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
            .tapScale(onClick)
            .height(58.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun SongActionRow(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    badgeText: String? = null,
    accented: Boolean = false,
    hasChevron: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (badgeText != null) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (accented) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        if (hasChevron) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.lucide_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private val SLEEP_TIMER_DURATIONS_MIN = listOf(5, 10, 15, 30, 45, 60, 90, 120)

@Composable
internal fun SleepTimerPickerSheet(
    currentEndMs: Long?,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEndOfTrack = currentEndMs == -1L
    val remainingMinutes = if (currentEndMs != null && currentEndMs > 0L) {
        ((currentEndMs - SystemClock.elapsedRealtime()) / 60_000L).coerceAtLeast(0).toInt()
    } else {
        null
    }

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(AppR.string.now_playing_sleep_timer),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (currentEndMs != null) {
                    Text(
                        stringResource(AppR.string.sleep_timer_turn_off),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (currentEndMs != null) {
                val statusText = if (isEndOfTrack) {
                    stringResource(AppR.string.sleep_timer_end_status)
                } else {
                    val minutes = remainingMinutes ?: 0
                    pluralStringResource(AppR.plurals.sleep_timer_minutes_remaining, minutes, minutes)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isEndOfTrack) R.drawable.lucide_ic_disc_3 else R.drawable.lucide_ic_moon
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isEndOfTrack) MaterialTheme.colorScheme.surfaceContainerHighest
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                    .border(
                        1.dp,
                        if (isEndOfTrack) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .tapScale { onSelect(-1L) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_disc_3),
                    contentDescription = null,
                    tint = if (isEndOfTrack) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(AppR.string.sleep_timer_end_current),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isEndOfTrack) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isEndOfTrack) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(AppR.string.sleep_timer_end_current_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isEndOfTrack) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_check),
                        contentDescription = stringResource(AppR.string.sleep_timer_selected),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(AppR.string.sleep_timer_duration_section),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(SLEEP_TIMER_DURATIONS_MIN) { minutes ->
                    val isSelected = remainingMinutes == minutes
                    val bg = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    val border = if (isSelected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    }
                    val textTint = if (isSelected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(bg)
                            .border(1.dp, border, RoundedCornerShape(100.dp))
                            .tapScale { onSelect(minutes * 60_000L) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(
                                AppR.plurals.sleep_timer_duration_minutes,
                                minutes,
                                minutes
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = textTint
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FullScreenArtworkSheet(
    song: Song,
    onSearchCoverOnline: () -> Unit,
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                AlbumArt(song = song, modifier = Modifier.fillMaxSize(), thumbnail = false)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = song.album,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (song.releaseYear != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = song.releaseYear.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .tapScale {
                        onDismiss()
                        onSearchCoverOnline()
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_image),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(AppR.string.now_playing_search_cover_short),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
