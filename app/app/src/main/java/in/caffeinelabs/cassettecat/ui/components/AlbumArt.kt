package `in`.caffeinelabs.cassettecat.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.AlbumArtLoader
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.RemoteAlbumArtLoader

// Process-wide, not per-composable: a remember{}-scoped loader wouldn't share its LRU
// cache across LazyColumn rows, causing repeated decode/network work (visible scroll jank).
private object AlbumArtLoaders {
    @Volatile private var local: AlbumArtLoader? = null
    val remote = RemoteAlbumArtLoader()

    fun local(context: Context): AlbumArtLoader =
        local ?: synchronized(this) {
            local ?: AlbumArtLoader(context.applicationContext).also { local = it }
        }
}

suspend fun prefetchAlbumArt(context: Context, song: Song?) {
    song ?: return
    when (song.source) {
        MusicSource.Local -> AlbumArtLoaders.local(context).load(song)
        MusicSource.Subsonic, MusicSource.Jellyfin, MusicSource.Radio -> song.artUri?.let { AlbumArtLoaders.remote.load(it) }
        MusicSource.ListeningRoomHost -> Unit
    }
}

suspend fun loadSongArtwork(context: Context, song: Song): Bitmap? {
    return when (song.source) {
        MusicSource.Local -> AlbumArtLoaders.local(context).load(song)
        MusicSource.Subsonic, MusicSource.Jellyfin, MusicSource.Radio -> song.artUri?.let { AlbumArtLoaders.remote.load(it) }
        MusicSource.ListeningRoomHost -> null
    }
}

@Composable
fun AlbumArt(song: Song, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(song.id) {
        mutableStateOf(
            when (song.source) {
                MusicSource.Local -> AlbumArtLoaders.local(context).peek(song)
                MusicSource.Subsonic, MusicSource.Jellyfin, MusicSource.Radio -> song.artUri?.let { AlbumArtLoaders.remote.peek(it) }
                MusicSource.ListeningRoomHost -> null
            }
        )
    }
    LaunchedEffect(song.id) {
        if (bitmap == null) {
            bitmap = when (song.source) {
                MusicSource.Local -> AlbumArtLoaders.local(context).load(song)
                MusicSource.Subsonic, MusicSource.Jellyfin, MusicSource.Radio -> song.artUri?.let { AlbumArtLoaders.remote.load(it) }
                MusicSource.ListeningRoomHost -> null
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_disc_3),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
