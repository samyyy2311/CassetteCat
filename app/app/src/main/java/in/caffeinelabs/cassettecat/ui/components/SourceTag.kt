package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.JellyfinBlue
import `in`.caffeinelabs.cassettecat.ui.theme.SubsonicOrange

@Composable
fun MusicSource.label(): String = stringResource(
    when (this) {
        MusicSource.Local -> R.string.library_source_local
        MusicSource.Subsonic -> R.string.source_subsonic
        MusicSource.Jellyfin -> R.string.source_jellyfin
        MusicSource.ListeningRoomHost -> R.string.source_room
        MusicSource.Radio -> R.string.source_radio
        MusicSource.Desktop, MusicSource.Computer -> R.string.source_desktop
    }
)

@Composable
fun MusicSource.color(): Color = when (this) {
    MusicSource.Local -> MaterialTheme.colorScheme.onSurfaceVariant
    MusicSource.Subsonic -> SubsonicOrange
    MusicSource.Jellyfin -> JellyfinBlue
    MusicSource.ListeningRoomHost, MusicSource.Radio, MusicSource.Desktop, MusicSource.Computer -> MaterialTheme.colorScheme.tertiary
}

/** Names where a song comes from, next to its title in lists. */
@Composable
fun SourceTag(source: MusicSource, modifier: Modifier = Modifier) {
    val color = source.color()
    Text(
        text = source.label(),
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 9.sp),
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}
