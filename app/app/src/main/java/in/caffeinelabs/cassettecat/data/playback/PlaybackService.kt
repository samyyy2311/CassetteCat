@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.data.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.core.graphics.scale
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import `in`.caffeinelabs.cassettecat.MainActivity
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.data.download.DownloadCache
import `in`.caffeinelabs.cassettecat.data.download.StreamCacheKeyFactory
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.widget.CassetteWidgetProvider
import `in`.caffeinelabs.cassettecat.ui.widget.PlaybackTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@UnstableApi
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var libraryTree: MediaLibraryTree
    private var currentFavoriteIds: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        favoritesRepository = FavoritesRepository(this)
        libraryTree = MediaLibraryTree(this)

        val directFactory = DefaultDataSource.Factory(this)
        // Read-only: DownloadManager is the only intended writer to this cache; a concurrent
        // write from playback stalls the download instead of erroring.
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(DownloadCache.get(this))
            .setCacheKeyFactory(StreamCacheKeyFactory)
            .setUpstreamDataSourceFactory(directFactory)
            .setCacheWriteDataSinkFactory(null)
        val routedFactory = DataSource.Factory {
            StreamOnlyCacheDataSource(cacheDataSourceFactory.createDataSource(), directFactory.createDataSource())
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(routedFactory))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                        500,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                    )
                    .build()
            )
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
                updateNotificationLayout(player)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                updateNotificationLayout(player)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncWidgetState(player)
                updateNotificationLayout(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncWidgetState(player)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncWidgetState(player)
            }
        })

        val appPreferencesRepository = AppPreferencesRepository(this)

        serviceScope.launch {
            favoritesRepository.favoriteIds.collect { ids ->
                currentFavoriteIds = ids
                mediaSession?.player?.let { p -> updateNotificationLayout(p) }
            }
        }

        serviceScope.launch {
            appPreferencesRepository.preferences.collect { prefs ->
                player.setHandleAudioBecomingNoisy(prefs.pauseOnHeadphoneDisconnect)
                val maxChannels = if (prefs.monoAudio) 1 else Int.MAX_VALUE
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setMaxAudioChannelCount(maxChannels)
                    .build()
                player.skipSilenceEnabled = prefs.skipSilenceEnabled
                player.pauseAtEndOfMediaItems = !prefs.gaplessPlayback
            }
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaLibrarySession.Builder(this, player, CustomMediaLibrarySessionCallback())
            .setSessionActivity(sessionActivity)
            .setCustomLayout(buildCustomLayout(player, false))
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.app_name)
                .build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            val player = mediaSession?.player
            when (action) {
                ACTION_WIDGET_PLAY_PAUSE -> {
                    if (player != null) {
                        if (player.isPlaying) player.pause() else player.play()
                    }
                }
                ACTION_WIDGET_NEXT -> player?.seekToNextMediaItem()
                ACTION_WIDGET_PREV -> player?.seekToPreviousMediaItem()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotificationLayout(player: Player) {
        val currentMediaId = player.currentMediaItem?.mediaId
        val isFav = currentMediaId != null && currentMediaId in currentFavoriteIds
        mediaSession?.setCustomLayout(buildCustomLayout(player, isFav))
    }

    private fun buildCustomLayout(player: Player, isFavorite: Boolean): List<CommandButton> {
        val favoriteIconRes = if (isFavorite) {
            R.drawable.ic_notification_heart_filled
        } else {
            R.drawable.ic_notification_heart_outline
        }
        val favoriteButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(favoriteIconRes)
            .setSessionCommand(SessionCommand(ACTION_CUSTOM_FAVORITE, Bundle.EMPTY))
            .setDisplayName(if (isFavorite) "Unfavorite" else "Favorite")
            .build()

        val shuffleButton = CommandButton.Builder(
            if (player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF
        )
            .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
            .setDisplayName(if (player.shuffleModeEnabled) "Shuffle on" else "Shuffle off")
            .build()

        val repeatIcon = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
            Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
            else -> CommandButton.ICON_REPEAT_OFF
        }
        val repeatButton = CommandButton.Builder(repeatIcon)
            .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
            .setDisplayName(
                when (player.repeatMode) {
                    Player.REPEAT_MODE_ONE -> "Repeat one"
                    Player.REPEAT_MODE_ALL -> "Repeat all"
                    else -> "Repeat off"
                }
            )
            .build()

        return listOf(favoriteButton, shuffleButton, repeatButton)
    }

    private inner class CustomMediaLibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_CUSTOM_FAVORITE, Bundle.EMPTY))
                .build()
            val currentMediaId = session.player.currentMediaItem?.mediaId
            val isFav = currentMediaId != null && currentMediaId in currentFavoriteIds
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(availableSessionCommands)
                .setCustomLayout(buildCustomLayout(session.player, isFav))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_CUSTOM_FAVORITE) {
                val currentMediaId = session.player.currentMediaItem?.mediaId
                if (currentMediaId != null) {
                    val isFav = currentMediaId in currentFavoriteIds
                    serviceScope.launch {
                        favoritesRepository.setFavorite(currentMediaId, !isFav)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val player = mediaSession.player
            val currentItem = player.currentMediaItem
            return if (currentItem != null) {
                Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        listOf(currentItem),
                        player.currentMediaItemIndex,
                        player.currentPosition
                    )
                )
            } else {
                super.onPlaybackResumption(mediaSession, controller)
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            serviceScope.launch {
                runCatching {
                    mediaItems.map { item ->
                        if (item.localConfiguration != null) item else libraryTree.item(item.mediaId) ?: item
                    }
                }.onSuccess { future.set(it) }.onFailure { future.setException(it) }
            }
            return future
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(libraryTree.rootItem, params))

        @Suppress("WrongConstant")
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch {
                runCatching { libraryTree.item(mediaId) }
                    .onSuccess { item ->
                        future.set(
                            if (item != null) LibraryResult.ofItem(item, null) else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                        )
                    }
                    .onFailure { future.setException(it) }
            }
            return future
        }

        @Suppress("WrongConstant")
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                runCatching { libraryTree.children(parentId) }
                    .onSuccess { children ->
                        future.set(
                            if (children != null) {
                                val start = (page * pageSize).coerceIn(0, children.size)
                                val end = (start + pageSize).coerceIn(start, children.size)
                                LibraryResult.ofItemList(ImmutableList.copyOf(children.subList(start, end)), params)
                            } else {
                                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                            }
                        )
                    }
                    .onFailure { future.setException(it) }
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            val future = SettableFuture.create<LibraryResult<Void>>()
            serviceScope.launch {
                runCatching { libraryTree.search(query) }
                    .onSuccess { results ->
                        session.notifySearchResultChanged(browser, query, results.size, params)
                        future.set(LibraryResult.ofVoid(params))
                    }
                    .onFailure { future.setException(it) }
            }
            return future
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                runCatching { libraryTree.search(query) }
                    .onSuccess { results ->
                        val start = (page * pageSize).coerceIn(0, results.size)
                        val end = (start + pageSize).coerceIn(start, results.size)
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(results.subList(start, end)), params))
                    }
                    .onFailure { future.setException(it) }
            }
            return future
        }
    }

    private fun syncWidgetState(player: Player) {
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString()
        val artist = metadata.artist?.toString()
        val isPlaying = player.isPlaying
        val artworkData = metadata.artworkData
        serviceScope.launch(Dispatchers.Default) {
            val artBitmap = artworkData?.let { data ->
                runCatching {
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val decoded = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                    decoded?.scale(120, 120)
                }.getOrNull()
            }
            runCatching {
                CassetteWidgetProvider.updateAllWidgets(
                    context = this@PlaybackService,
                    title = title,
                    artist = artist,
                    isPlaying = isPlaying,
                    artBitmap = artBitmap
                )
            }
            runCatching {
                TileService.requestListeningState(this@PlaybackService, ComponentName(this@PlaybackService, PlaybackTileService::class.java))
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_CUSTOM_FAVORITE = "in.caffeinelabs.cassettecat.action.CUSTOM_FAVORITE"
        const val ACTION_WIDGET_PLAY_PAUSE = "in.caffeinelabs.cassettecat.action.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "in.caffeinelabs.cassettecat.action.WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "in.caffeinelabs.cassettecat.action.WIDGET_PREV"
    }
}

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
