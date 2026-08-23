@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song

// Local songs are already on-device, nothing to show. Only reflects state here (start/
// retry lives in the dedicated download actions, not a tap target on every row).
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun DownloadStatusIcon(song: Song, modifier: Modifier = Modifier) {
    if (song.source == MusicSource.Local) return
    val context = LocalContext.current
    val repository = remember { SongDownloadRepository.getInstance(context) }
    val downloads by repository.downloads.collectAsStateWithLifecycle()
    val state = downloads[song.id]?.state ?: return

    when (state) {
        Download.STATE_COMPLETED -> Icon(
            painter = painterResource(R.drawable.lucide_ic_circle_check),
            contentDescription = "Downloaded",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = modifier.size(16.dp)
        )
        Download.STATE_DOWNLOADING, Download.STATE_QUEUED, Download.STATE_RESTARTING -> CircularProgressIndicator(
            modifier = modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Download.STATE_FAILED -> Icon(
            painter = painterResource(R.drawable.lucide_ic_circle_alert),
            contentDescription = "Download failed",
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier.size(16.dp)
        )
        else -> Unit
    }
}
