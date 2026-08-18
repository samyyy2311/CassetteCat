package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel

internal class NowPlayingSheetState {
    var showMenu by mutableStateOf(false)
    var showGoToMenu by mutableStateOf(false)
    var showPlaylistPicker by mutableStateOf(false)
    var showSleepTimerPicker by mutableStateOf(false)
    var showCredits by mutableStateOf(false)
    var showOutputPicker by mutableStateOf(false)
    var showListeningRoom by mutableStateOf(false)
    var showScreenshotSuggestion by mutableStateOf(false)
    var showTagEditor by mutableStateOf(false)
}

@Composable
internal fun rememberNowPlayingSheetState(): NowPlayingSheetState = remember { NowPlayingSheetState() }

@Composable
internal fun NowPlayingScreenSheetsHost(
    song: Song?,
    isFavorite: Boolean,
    sleepTimerEndMs: Long?,
    playlists: List<Playlist>,
    listeningRoom: ListeningRoomState,
    playbackViewModel: PlaybackViewModel,
    downloadRepository: SongDownloadRepository,
    context: Context,
    sheetState: NowPlayingSheetState,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    syncedLyrics: List<LyricLine>? = null,
    fallbackLyrics: String? = null,
    currentPositionMs: Long = 0L
) {
    if (sheetState.showMenu) {
        song?.let { currentSong ->
            NowPlayingActionsSheet(
                song = currentSong,
                isFavorite = isFavorite,
                sleepTimerEndMs = sleepTimerEndMs,
                onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                onShare = { sheetState.showScreenshotSuggestion = true },
                onAddToQueue = { playbackViewModel.addToUpNext(listOf(currentSong)) },
                onDownload = { downloadRepository.download(currentSong) },
                onOpenCredits = { sheetState.showCredits = true },
                onOpenOutputPicker = { sheetState.showOutputPicker = true },
                onOpenListeningRoom = { sheetState.showListeningRoom = true },
                onOpenSleepTimer = { sheetState.showSleepTimerPicker = true },
                onOpenTagEditor = { sheetState.showTagEditor = true },
                onDismiss = { sheetState.showMenu = false }
            )
        }
    }
    if (sheetState.showTagEditor) {
        song?.let { currentSong ->
            `in`.caffeinelabs.cassettecat.ui.screens.library.TagEditorSheet(
                song = currentSong,
                onDismiss = { sheetState.showTagEditor = false }
            )
        }
    }
    if (sheetState.showGoToMenu) {
        song?.let { currentSong ->
            NowPlayingGoToSheet(
                song = currentSong,
                playlists = playlists,
                onNavigateToArtist = { artist ->
                    sheetState.showGoToMenu = false
                    onNavigateToArtist(artist)
                },
                onNavigateToAlbum = { albumId ->
                    sheetState.showGoToMenu = false
                    onNavigateToAlbum(albumId)
                },
                onNavigateToPlaylist = { playlistId ->
                    sheetState.showGoToMenu = false
                    onNavigateToPlaylist(playlistId)
                },
                onOpenPlaylistPicker = {
                    sheetState.showGoToMenu = false
                    sheetState.showPlaylistPicker = true
                },
                onDismiss = { sheetState.showGoToMenu = false }
            )
        }
    }
    if (sheetState.showPlaylistPicker) {
        NowPlayingPlaylistPicker(
            playlists = playlists,
            onSelect = { playlistId ->
                sheetState.showPlaylistPicker = false
                onNavigateToPlaylist(playlistId)
            },
            onDismiss = { sheetState.showPlaylistPicker = false }
        )
    }
    if (sheetState.showSleepTimerPicker) {
        SleepTimerPickerSheet(
            currentEndMs = sleepTimerEndMs,
            onSelect = { durationMs ->
                playbackViewModel.startSleepTimer(durationMs)
                sheetState.showSleepTimerPicker = false
            },
            onCancel = {
                playbackViewModel.cancelSleepTimer()
                sheetState.showSleepTimerPicker = false
            },
            onDismiss = { sheetState.showSleepTimerPicker = false }
        )
    }
    if (sheetState.showCredits) {
        song?.let { currentSong ->
            SongCreditsSheet(song = currentSong, onDismiss = { sheetState.showCredits = false })
        }
    }
    if (sheetState.showOutputPicker) {
        AudioOutputSheet(
            onOpenBluetoothSettings = {
                sheetState.showOutputPicker = false
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            },
            onDismiss = { sheetState.showOutputPicker = false }
        )
    }
    if (sheetState.showListeningRoom) {
        ListeningRoomSheet(
            state = listeningRoom,
            onStart = playbackViewModel::startListeningRoom,
            onFindNearby = playbackViewModel::findNearbyListeningRooms,
            onJoin = playbackViewModel::joinListeningRoom,
            onLeave = playbackViewModel::leaveListeningRoom,
            onDismiss = { sheetState.showListeningRoom = false }
        )
    }
    if (sheetState.showScreenshotSuggestion) {
        song?.let { currentSong ->
            ScreenshotShareSheet(
                song = currentSong,
                syncedLyrics = syncedLyrics,
                fallbackLyrics = fallbackLyrics,
                currentPositionMs = currentPositionMs,
                onDismiss = { sheetState.showScreenshotSuggestion = false }
            )
        }
    }
}
