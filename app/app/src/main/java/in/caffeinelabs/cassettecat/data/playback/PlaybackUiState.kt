@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.data.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import `in`.caffeinelabs.cassettecat.data.library.Song

data class AudioTrackFormat(
    val label: String,
    val badgeLabel: String,
    val codecName: String,
    val sampleRateHz: Int = 0,
    val bitDepth: Int = 0,
    val bitrateKbps: Int = 0,
    val isLossless: Boolean = false,
    val isHiRes: Boolean = false
)

data class PlaybackUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    // Drives the play/pause button: user intent, true the instant play() is called and
    // held through buffering, unlike isPlaying which can gap briefly between skip and
    // STATE_BUFFERING actually being reported.
    val playWhenReady: Boolean = false,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET,
    val upNext: List<Song> = emptyList(),
    val previousInQueue: Song? = null,
    // Most-recently-played first, capped in PlaybackRepository.
    val history: List<Song> = emptyList(),
    // Read off the current track's embedded tags (ID3 USLT / Vorbis LYRICS), see extractLyrics().
    val currentLyrics: String? = null,
    val audioFormat: AudioTrackFormat? = null
)
