package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.listeningroom.ListeningRoomState
import `in`.caffeinelabs.cassettecat.data.playback.LyricLine
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.PlaylistNameSheet
import `in`.caffeinelabs.cassettecat.ui.screens.library.SongTagEditorSheet
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

internal class NowPlayingSheetState {
    var showMenu by mutableStateOf(false)
    var showGoToMenu by mutableStateOf(false)
    var showPlaylistPicker by mutableStateOf(false)
    var showSleepTimerPicker by mutableStateOf(false)
    var showCredits by mutableStateOf(false)
    var showOutputPicker by mutableStateOf(false)
    var showListeningRoom by mutableStateOf(false)
    var showPlaybackSpeed by mutableStateOf(false)
    var showScreenshotSuggestion by mutableStateOf(false)
    var showSaveQueue by mutableStateOf(false)
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
    allSongs: List<Song>,
    queueSongs: List<Song>,
    listeningRoom: ListeningRoomState,
    playbackViewModel: PlaybackViewModel,
    downloadRepository: SongDownloadRepository,
    context: Context,
    sheetState: NowPlayingSheetState,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
    onSaveQueue: (String, List<String>) -> Unit,
    syncedLyrics: List<LyricLine>? = null,
    fallbackLyrics: String? = null,
    currentPositionMs: Long = 0L,
    onActiveViewChange: (NowPlayingView) -> Unit = {},
    onNavigateToDriveMode: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    if (sheetState.showMenu) {
        song?.let { currentSong ->
            val playbackSpeed by playbackViewModel.playbackSpeed.collectAsStateWithLifecycle()
            NowPlayingActionsSheet(
                song = currentSong,
                isFavorite = isFavorite,
                sleepTimerEndMs = sleepTimerEndMs,
                listeningRoomState = listeningRoom,
                playbackSpeed = playbackSpeed,
                onToggleFavorite = { playbackViewModel.toggleFavoriteForCurrentSong() },
                onShare = { sheetState.showScreenshotSuggestion = true },
                onShareFile = { scope.launch { shareAudioFile(context, currentSong) } },
                onAddToQueue = { playbackViewModel.addToUpNext(listOf(currentSong)) },
                onStartInstantMix = { playbackViewModel.playInstantMix(currentSong, allSongs) },
                onDownload = { downloadRepository.download(currentSong) },
                onOpenCredits = { sheetState.showCredits = true },
                onOpenOutputPicker = { sheetState.showOutputPicker = true },
                onOpenEqualizer = onNavigateToEqualizer,
                onOpenTagEditor = { sheetState.showTagEditor = true },
                onOpenListeningRoom = { sheetState.showListeningRoom = true },
                onOpenPlaybackSpeed = { sheetState.showPlaybackSpeed = true },
                onOpenSleepTimer = { sheetState.showSleepTimerPicker = true },
                onOpenDriveMode = onNavigateToDriveMode,
                onDismiss = { sheetState.showMenu = false }
            )
        }
    }
    if (sheetState.showSaveQueue) {
        PlaylistNameSheet(
            title = "Save Queue as Playlist",
            initialName = "",
            onConfirm = { name ->
                onSaveQueue(name, queueSongs.map { it.id })
                sheetState.showSaveQueue = false
            },
            onDismiss = { sheetState.showSaveQueue = false }
        )
    }
    if (sheetState.showPlaybackSpeed) {
        val playbackSpeed by playbackViewModel.playbackSpeed.collectAsStateWithLifecycle()
        val playbackPitch by playbackViewModel.playbackPitch.collectAsStateWithLifecycle()
        PlaybackSpeedSheet(
            currentSpeed = playbackSpeed,
            currentPitch = playbackPitch,
            onSelectSpeed = { playbackViewModel.setPlaybackSpeed(it) },
            onSelectPitch = { playbackViewModel.setPlaybackPitch(it) },
            onDismiss = { sheetState.showPlaybackSpeed = false }
        )
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
            onJoinManual = playbackViewModel::joinListeningRoomManually,
            onLeave = playbackViewModel::leaveListeningRoom,
            onDismiss = {
                sheetState.showListeningRoom = false
                playbackViewModel.stopFindingNearbyListeningRooms()
            }
        )
    }
    if (sheetState.showScreenshotSuggestion) {
        song?.let { currentSong ->
            ScreenshotShareSheet(
                song = currentSong,
                syncedLyrics = syncedLyrics,
                fallbackLyrics = fallbackLyrics,
                currentPositionMs = currentPositionMs,
                onDismiss = { sheetState.showScreenshotSuggestion = false },
                onOpenFullLyricEditor = {
                    sheetState.showScreenshotSuggestion = false
                    onActiveViewChange(NowPlayingView.LYRICS)
                }
            )
        }
    }
    if (sheetState.showTagEditor) {
        song?.let { currentSong ->
            SongTagEditorSheet(
                song = currentSong,
                onDismiss = { sheetState.showTagEditor = false },
                onSaved = { updated -> playbackViewModel.updateSongMetadata(updated) }
            )
        }
    }
}

private suspend fun shareAudioFile(context: Context, song: Song) {
    if (song.source == MusicSource.Local) {
        launchAudioShareIntent(context, song, song.contentUri)
        return
    }

    Toast.makeText(context, "Preparing ${song.title} to share...", Toast.LENGTH_SHORT).show()
    val shareUri = withContext(Dispatchers.IO) {
        runCatching {
            val response = sharedHttpClient.newCall(Request.Builder().url(song.contentUri.toString()).build()).execute()
            if (!response.isSuccessful) return@runCatching null
            val cacheDir = File(context.cacheDir, "shared_audio").apply { mkdirs() }
            val extension = extensionForMimeType(response.header("Content-Type"))
            val file = File(cacheDir, "share_${song.id.hashCode()}.$extension")
            response.body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    if (shareUri != null) {
        launchAudioShareIntent(context, song, shareUri)
    } else {
        Toast.makeText(context, "Couldn't prepare ${song.title} to share", Toast.LENGTH_SHORT).show()
    }
}

private fun launchAudioShareIntent(context: Context, song: Song, uri: android.net.Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share ${song.title}"))
    }
}

private fun extensionForMimeType(mimeType: String?): String = when {
    mimeType == null -> "mp3"
    mimeType.contains("flac") -> "flac"
    mimeType.contains("ogg") -> "ogg"
    mimeType.contains("wav") -> "wav"
    mimeType.contains("mp4") || mimeType.contains("m4a") || mimeType.contains("aac") -> "m4a"
    else -> "mp3"
}
