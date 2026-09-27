package `in`.caffeinelabs.cassettecat.data.device

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import `in`.caffeinelabs.cassettecat.data.library.Song
import kotlinx.coroutines.flow.combine

// Volume keys move the computer's volume in 5% steps.
private const val VOLUME_STEPS = 20

/**
 * The computer this phone controls, as a player for its own media session: the notification, lock screen,
 * headset buttons and volume keys then act on the computer. [onCommand] runs after each command is sent.
 */
@UnstableApi
internal class DesktopSessionPlayer(
    private val desktop: DesktopRemoteRepository,
    private val onCommand: () -> Unit
) : SimpleBasePlayer(Looper.getMainLooper()) {

    private val commands = Player.Commands.Builder().addAll(
        Player.COMMAND_PLAY_PAUSE,
        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_MEDIA_ITEM,
        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
        Player.COMMAND_GET_TIMELINE,
        Player.COMMAND_GET_METADATA,
        Player.COMMAND_GET_DEVICE_VOLUME,
        Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
        Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS
    ).build()

    private val deviceInfo = DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE).setMaxVolume(VOLUME_STEPS).build()

    /** Keeps the session showing what the computer plays, until cancelled. */
    suspend fun follow() = combine(desktop.controlledState, desktop.status) { _, _ -> }.collect { invalidateState() }

    override fun getState(): State {
        val remote = desktop.controlledState.value
        val builder = State.Builder()
            .setAvailableCommands(commands)
            .setDeviceInfo(deviceInfo)
            .setDeviceVolume((desktop.status.value?.volumePercent ?: 0) * VOLUME_STEPS / 100)
        val current = remote?.currentSong ?: return builder.setPlaybackState(Player.STATE_IDLE).build()
        // The up-next songs follow the current one so next is available and the queue can be picked from.
        val playlist = (listOf(current) + remote.upNext).mapIndexed { index, song -> mediaItemData(index, song) }
        return builder
            .setPlaylist(playlist)
            .setCurrentMediaItemIndex(0)
            .setPlaybackState(Player.STATE_READY)
            .setPlayWhenReady(remote.isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .setShuffleModeEnabled(remote.isShuffleEnabled)
            .setRepeatMode(remote.repeatMode)
            .setContentPositionMs(
                PositionSupplier.getExtrapolating(desktop.positionMs.value, if (remote.isPlaying) 1f else 0f)
            )
            .build()
    }

    private fun mediaItemData(index: Int, song: Song) = MediaItemData.Builder(index)
        .setMediaItem(
            MediaItem.Builder()
                .setMediaId(song.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.artUri)
                        .build()
                )
                .build()
        )
        .setDurationUs(if (song.durationMs > 0) song.durationMs * 1000 else C.TIME_UNSET)
        .setIsSeekable(true)
        .build()

    private fun sent(send: () -> Unit): ListenableFuture<*> {
        send()
        onCommand()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean) =
        sent { desktop.sendAction(if (playWhenReady) "play" else "pause") }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int) = sent {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> desktop.sendAction("next")
            Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> desktop.sendAction("previous")
            Player.COMMAND_SEEK_TO_MEDIA_ITEM ->
                desktop.controlledState.value?.upNext?.getOrNull(mediaItemIndex - 1)?.let(desktop::playFromQueue)
            else -> desktop.seek(positionMs)
        }
    }

    override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int) =
        sent { desktop.setVolume(deviceVolume * 100 / VOLUME_STEPS) }

    override fun handleIncreaseDeviceVolume(flags: Int) = handleSetDeviceVolume((deviceVolume + 1).coerceAtMost(VOLUME_STEPS), flags)

    override fun handleDecreaseDeviceVolume(flags: Int) = handleSetDeviceVolume((deviceVolume - 1).coerceAtLeast(0), flags)
}
