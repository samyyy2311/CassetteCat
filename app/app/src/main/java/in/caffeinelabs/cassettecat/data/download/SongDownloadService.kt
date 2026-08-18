package `in`.caffeinelabs.cassettecat.data.download

import android.app.Notification
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR

private const val NOTIFICATION_ID = 2
private const val CHANNEL_ID = "cassettecat_downloads"

// Separate foreground service from PlaybackService, per Media3's DownloadManager design.
class SongDownloadService : DownloadService(
    NOTIFICATION_ID,
    DownloadService.DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    AppR.string.download_channel_name,
    0
) {
    private val notificationHelper by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    override fun getDownloadManager(): DownloadManager = SongDownloadRepository.getInstance(this).downloadManager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        notificationHelper.buildProgressNotification(
            this,
            R.drawable.lucide_ic_download,
            null,
            null,
            downloads,
            notMetRequirements
        )
}
