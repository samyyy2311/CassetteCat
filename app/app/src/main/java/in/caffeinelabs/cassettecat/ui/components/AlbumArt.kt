package `in`.caffeinelabs.cassettecat.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.AlbumArtLoader
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.RemoteAlbumArtLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val THUMBNAIL_LOAD_DEBOUNCE_MS = 120L

// Process-wide, not per-composable: a remember{}-scoped loader wouldn't share its LRU
// cache across LazyColumn rows, causing repeated decode/network work (visible scroll jank).
@Suppress("StaticFieldLeak")
private object AlbumArtLoaders {
    @Volatile private var local: AlbumArtLoader? = null
    @Volatile private var settingsRepo: ServiceSettingsRepository? = null
    val remote = RemoteAlbumArtLoader()

    fun local(context: Context): AlbumArtLoader =
        local ?: synchronized(this) {
            local ?: AlbumArtLoader(context.applicationContext).also { local = it }
        }

    fun settingsRepository(context: Context): ServiceSettingsRepository =
        settingsRepo ?: synchronized(this) {
            settingsRepo ?: ServiceSettingsRepository(context.applicationContext).also { settingsRepo = it }
        }
}

suspend fun prefetchAlbumArt(context: Context, song: Song?, thumbnail: Boolean = false) {
    song ?: return
    val settings = AlbumArtLoaders.settingsRepository(context).settings.first()
    val isOffline = settings.offlineBlackoutMode
    val coverArtArchiveEnabled = settings.isEnabled(ExternalService.COVER_ART_ARCHIVE)
    when (song.source) {
        MusicSource.Local -> AlbumArtLoaders.local(context).load(song, coverArtArchiveEnabled, thumbnail)
        MusicSource.Subsonic, MusicSource.Jellyfin, MusicSource.Radio -> if (!isOffline) song.artUri?.let { AlbumArtLoaders.remote.load(it, thumbnail) }
        MusicSource.ListeningRoomHost -> Unit
    }
}

suspend fun loadSongArtwork(context: Context, song: Song, thumbnail: Boolean = false): Bitmap? {
    val settings = AlbumArtLoaders.settingsRepository(context).settings.first()
    val isOffline = settings.offlineBlackoutMode
    val coverArtArchiveEnabled = settings.isEnabled(ExternalService.COVER_ART_ARCHIVE)
    return when (song.source) {
        MusicSource.Local -> AlbumArtLoaders.local(context).load(song, coverArtArchiveEnabled, thumbnail)
        MusicSource.Subsonic, MusicSource.Jellyfin, MusicSource.Radio -> if (!isOffline) song.artUri?.let { AlbumArtLoaders.remote.load(it, thumbnail) } else null
        MusicSource.ListeningRoomHost -> null
    }
}

@Composable
fun AlbumArt(song: Song, modifier: Modifier = Modifier, thumbnail: Boolean = true) {
    val context = LocalContext.current

    var bitmap by remember(song.id, thumbnail) {
        mutableStateOf(
            when (song.source) {
                MusicSource.Local -> AlbumArtLoaders.local(context).peek(song, thumbnail)
                MusicSource.Subsonic, MusicSource.Jellyfin, MusicSource.Radio -> song.artUri?.let { AlbumArtLoaders.remote.peek(it, thumbnail) }
                MusicSource.ListeningRoomHost -> null
            }
        )
    }
    LaunchedEffect(song.id, thumbnail) {
        if (bitmap == null) {
            if (thumbnail) delay(THUMBNAIL_LOAD_DEBOUNCE_MS)
            val settings = AlbumArtLoaders.settingsRepository(context).settings.first()
            val isOffline = settings.offlineBlackoutMode
            val coverArtArchiveEnabled = settings.isEnabled(ExternalService.COVER_ART_ARCHIVE)
            bitmap = when (song.source) {
                MusicSource.Local -> AlbumArtLoaders.local(context).load(song, coverArtArchiveEnabled, thumbnail)
                MusicSource.Subsonic, MusicSource.Jellyfin, MusicSource.Radio -> if (!isOffline) song.artUri?.let { AlbumArtLoaders.remote.load(it, thumbnail) } else null
                MusicSource.ListeningRoomHost -> null
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Crossfade(targetState = bitmap, animationSpec = tween(100), label = "albumArtCrossfade") { current ->
            if (current != null) {
                val imageAspectRatio = if (current.height > 0) current.width.toFloat() / current.height.toFloat() else 1f
                val isNonSquare = imageAspectRatio < 0.94f || imageAspectRatio > 1.06f
                if (isNonSquare) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = current.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().blur(18.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                        )
                        Image(
                            bitmap = current.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Image(
                        bitmap = current.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                if (song.source == MusicSource.Radio) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_radio),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_disc_3),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
