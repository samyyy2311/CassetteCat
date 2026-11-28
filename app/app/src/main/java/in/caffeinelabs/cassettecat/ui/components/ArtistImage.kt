package `in`.caffeinelabs.cassettecat.ui.components

import android.content.Context
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
import `in`.caffeinelabs.cassettecat.data.library.ArtistImageLoader
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import kotlinx.coroutines.flow.first
import java.io.File

// Shared LRU cache across composable callers.
@Suppress("StaticFieldLeak")
private object ArtistImageLoaderHolder {
    @Volatile var loaded: ArtistImageLoader? = null

    fun loader(context: Context): ArtistImageLoader =
        loaded ?: synchronized(this) {
            loaded ?: ArtistImageLoader(File(context.applicationContext.cacheDir, "artist_images")).also { loaded = it }
        }
    @Volatile private var settingsRepo: ServiceSettingsRepository? = null

    fun settingsRepository(context: Context): ServiceSettingsRepository =
        settingsRepo ?: synchronized(this) {
            settingsRepo ?: ServiceSettingsRepository(context.applicationContext).also { settingsRepo = it }
        }
}

fun trimArtistImageCaches(level: Int) {
    ArtistImageLoaderHolder.loaded?.trimCaches(level)
}

@Composable
fun ArtistImage(
    artist: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnail: Boolean = true
) {
    val context = LocalContext.current
    val loader = remember { ArtistImageLoaderHolder.loader(context) }
    var bitmap by remember(artist, thumbnail) { mutableStateOf(loader.peek(artist, thumbnail)) }
    LaunchedEffect(artist, thumbnail) {
        if (bitmap == null) {
            val settings = ArtistImageLoaderHolder.settingsRepository(context).settings.first()
            bitmap = loader.load(
                artist,
                settings.isEnabled(ExternalService.DEEZER),
                settings.isEnabled(ExternalService.AUDIODB),
                thumbnail
            )
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_user),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
