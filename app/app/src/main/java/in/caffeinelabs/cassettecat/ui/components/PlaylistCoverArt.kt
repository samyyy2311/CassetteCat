package `in`.caffeinelabs.cassettecat.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.PlaylistCoverType
import `in`.caffeinelabs.cassettecat.data.library.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Covers are already capped at 512px by PlaylistCoverStorage, sized in bytes so this
// bounds actual memory rather than entry count.
private const val COVER_CACHE_BYTES = 8 * 1024 * 1024
private val coverCache = object : LruCache<String, Bitmap>(COVER_CACHE_BYTES) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}

@Composable
fun rememberLocalFileCoverBitmap(path: String?): Bitmap? {
    if (path == null) return null
    var bitmap by remember(path) { mutableStateOf(coverCache.get(path)) }
    LaunchedEffect(path) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) }?.also { coverCache.put(path, it) }
        }
    }
    return bitmap
}

// key -> drawable, shown in the cover picker and looked up when rendering a saved
// ICON cover; falls back to the generic music icon if a stored key is ever missing
// (e.g. after a future curation change)
val PLAYLIST_ICON_OPTIONS: List<Pair<String, Int>> = listOf(
    "guitar" to R.drawable.lucide_ic_guitar,
    "mic" to R.drawable.lucide_ic_mic_vocal,
    "piano" to R.drawable.lucide_ic_piano,
    "disc" to R.drawable.lucide_ic_disc,
    "headphones" to R.drawable.lucide_ic_headphones,
    "radio" to R.drawable.lucide_ic_radio,
    "cassette" to R.drawable.lucide_ic_cassette_tape,
    "speaker" to R.drawable.lucide_ic_speaker,
    "drum" to R.drawable.lucide_ic_drum,
    "waves" to R.drawable.lucide_ic_waves,
    "zap" to R.drawable.lucide_ic_zap,
    "sparkles" to R.drawable.lucide_ic_sparkles,
    "heart" to R.drawable.lucide_ic_heart,
    "star" to R.drawable.lucide_ic_star,
    "flame" to R.drawable.lucide_ic_flame,
    "moon" to R.drawable.lucide_ic_moon,
    "sun" to R.drawable.lucide_ic_sun,
    "sunrise" to R.drawable.lucide_ic_sunrise,
    "sunset" to R.drawable.lucide_ic_sunset,
    "cloud_rain" to R.drawable.lucide_ic_cloud_rain,
    "snowflake" to R.drawable.lucide_ic_snowflake,
    "umbrella" to R.drawable.lucide_ic_umbrella,
    "coffee" to R.drawable.lucide_ic_coffee,
    "beer" to R.drawable.lucide_ic_beer,
    "wine" to R.drawable.lucide_ic_wine,
    "martini" to R.drawable.lucide_ic_martini,
    "pizza" to R.drawable.lucide_ic_pizza,
    "car" to R.drawable.lucide_ic_car,
    "bike" to R.drawable.lucide_ic_bike,
    "plane" to R.drawable.lucide_ic_plane,
    "compass" to R.drawable.lucide_ic_compass,
    "map" to R.drawable.lucide_ic_map,
    "mountain" to R.drawable.lucide_ic_mountain,
    "tent" to R.drawable.lucide_ic_tent,
    "globe" to R.drawable.lucide_ic_globe,
    "dumbbell" to R.drawable.lucide_ic_dumbbell,
    "footprints" to R.drawable.lucide_ic_footprints,
    "gamepad" to R.drawable.lucide_ic_gamepad,
    "book" to R.drawable.lucide_ic_book,
    "palette" to R.drawable.lucide_ic_palette,
    "film" to R.drawable.lucide_ic_film,
    "drama" to R.drawable.lucide_ic_drama,
    "trophy" to R.drawable.lucide_ic_trophy,
    "award" to R.drawable.lucide_ic_award,
    "crown" to R.drawable.lucide_ic_crown,
    "gem" to R.drawable.lucide_ic_gem,
    "rocket" to R.drawable.lucide_ic_rocket,
    "gift" to R.drawable.lucide_ic_gift,
    "smile" to R.drawable.lucide_ic_smile,
    "flower" to R.drawable.lucide_ic_flower
)

val PLAYLIST_EMOJI_OPTIONS: List<String> = listOf(
    "🎵", "🎶", "🎧", "🎸", "🎤", "🎹", "🥁", "📻", "💿",
    "🔥", "❤️", "⭐", "✨", "🌙", "☀️", "🌊", "🍃", "❄️",
    "🎉", "😎", "😢", "💪", "🏃", "☕", "🚗", "✈️", "📚",
    "🎮", "🏋️", "☂️", "🏕️", "🏔️", "🌧️", "🎬", "🏆"
)

@Composable
fun PlaylistCoverArt(playlist: Playlist, fallbackSong: Song?, modifier: Modifier = Modifier) {
    when (playlist.coverType) {
        PlaylistCoverType.IMAGE -> {
            val bitmap = rememberLocalFileCoverBitmap(playlist.coverValue)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = modifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                DefaultPlaylistCover(modifier)
            }
        }

        PlaylistCoverType.ICON -> {
            val iconRes = PLAYLIST_ICON_OPTIONS.find { it.first == playlist.coverValue }?.second
                ?: R.drawable.lucide_ic_music
            Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PlaylistCoverType.EMOJI -> {
            Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Text(playlist.coverValue ?: "🎵", fontSize = 40.sp)
            }
        }

        PlaylistCoverType.NONE -> {
            if (fallbackSong != null) {
                AlbumArt(song = fallbackSong, modifier = modifier)
            } else {
                DefaultPlaylistCover(modifier)
            }
        }
    }
}

@Composable
private fun DefaultPlaylistCover(modifier: Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_music),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
