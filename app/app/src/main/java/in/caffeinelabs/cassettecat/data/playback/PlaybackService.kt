package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Intent
import android.net.Uri
import android.app.PendingIntent
import `in`.caffeinelabs.cassettecat.MainActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.data.download.DownloadCache
import `in`.caffeinelabs.cassettecat.data.download.StreamCacheKeyFactory

// MediaSessionService auto-promotes to foreground and posts the notification via its
// built-in DefaultMediaNotificationProvider, no manual startForeground() needed. The
// default MediaSession.Callback already forwards play/pause/seek/skip, no custom callback needed.
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Reads through the same disk cache SongDownloadService writes completed downloads
        // to, keyed by StreamCacheKeyFactory so a downloaded song plays from disk even
        // though its stream URL differs from the one that was used to download it. Local
        // content:// playback is routed around the cache entirely (see StreamOnlyCacheDataSource)
        // so it doesn't fill the downloads cache with redundant copies of files already on
        // disk, competing with real downloads for its size-capped budget.
        val directFactory = DefaultDataSource.Factory(this)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(DownloadCache.get(this))
            .setCacheKeyFactory(StreamCacheKeyFactory)
            .setUpstreamDataSourceFactory(directFactory)
        val routedFactory = DataSource.Factory {
            StreamOnlyCacheDataSource(cacheDataSourceFactory.createDataSource(), directFactory.createDataSource())
        }
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(routedFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                mediaSession?.setCustomLayout(listOf(shuffleCommandButton(shuffleModeEnabled)))
            }
        })
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            // The media notification/card delegates its body tap to this explicit activity.
            .setSessionActivity(sessionActivity)
            .setCustomLayout(listOf(shuffleCommandButton(player.shuffleModeEnabled)))
            .build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.app_name)
                .build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // CassetteCat follows the user's explicit Recents dismissal: do not retain a hidden
        // foreground player/notification after its task has been cleared.
        mediaSession?.player?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

private fun shuffleCommandButton(enabled: Boolean): CommandButton =
    CommandButton.Builder(if (enabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF)
        .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
        .setDisplayName("Shuffle")
        .build()

// Dispatches http(s) requests through the cache, everything else (local content:// URIs)
// straight through, so local playback never writes into the downloads cache.
private class StreamOnlyCacheDataSource(
    private val cached: DataSource,
    private val direct: DataSource
) : DataSource {
    private var active: DataSource = direct

    override fun open(dataSpec: DataSpec): Long {
        active = if (dataSpec.uri.scheme == "http" || dataSpec.uri.scheme == "https") cached else direct
        return active.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = active.read(buffer, offset, length)

    override fun addTransferListener(transferListener: TransferListener) {
        cached.addTransferListener(transferListener)
        direct.addTransferListener(transferListener)
    }

    override fun getUri(): Uri? = active.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active.responseHeaders

    override fun close() {
        active.close()
    }
}
