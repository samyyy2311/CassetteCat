@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.data.download

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SongDownloadRepository private constructor(private val context: Context) {
    // exposed for SongDownloadService, which must operate on this exact instance
    val downloadManager = DownloadManager(
        context,
        StandaloneDatabaseProvider(context),
        DownloadCache.get(context),
        DefaultHttpDataSource.Factory(),
        Executor(Runnable::run)
    ).apply { maxParallelDownloads = 3 }

    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads: StateFlow<Map<String, Download>> = _downloads.asStateFlow()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serviceSettingsRepository = ServiceSettingsRepository(context)

    init {
        refreshFromIndex()
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                refreshFromIndex()
            }

            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                refreshFromIndex()
            }
        })
        repositoryScope.launch {
            serviceSettingsRepository.settings
                .map { it.offlineBlackoutMode }
                .distinctUntilChanged()
                .collect { offline ->
                    if (offline) {
                        DownloadService.sendPauseDownloads(context, SongDownloadService::class.java, false)
                    } else {
                        DownloadService.sendResumeDownloads(context, SongDownloadService::class.java, false)
                    }
                }
        }
    }

    fun download(song: Song) {
        val current = _downloads.value[song.id]
        if (current != null && (current.state == Download.STATE_COMPLETED || current.state == Download.STATE_DOWNLOADING || current.state == Download.STATE_QUEUED)) {
            return
        }
        repositoryScope.launch {
            if (serviceSettingsRepository.settings.first().offlineBlackoutMode) {
                return@launch
            }
            runCatching {
                val wifiOnly = AppPreferencesRepository(context).preferences.first().wifiOnlyDownloads
                val requirements = Requirements(
                    if (wifiOnly) Requirements.NETWORK_UNMETERED else Requirements.NETWORK
                )
                DownloadService.sendSetRequirements(context, SongDownloadService::class.java, requirements, false)
                val request = DownloadRequest.Builder(song.id, song.contentUri)
                    .setCustomCacheKey(song.id)
                    .setData(song.title.toByteArray())
                    .build()
                DownloadService.sendAddDownload(context, SongDownloadService::class.java, request, false)
            }
        }
    }

    fun remove(songId: String) {
        runCatching {
            DownloadService.sendRemoveDownload(context, SongDownloadService::class.java, songId, false)
        }
    }

    private fun refreshFromIndex() {
        _downloads.value = runCatching {
            downloadManager.downloadIndex.getDownloads().use { cursor ->
                val map = mutableMapOf<String, Download>()
                while (cursor.moveToNext()) {
                    val download = cursor.download
                    map[download.request.id] = download
                }
                map
            }
        }.getOrDefault(emptyMap())
    }

    companion object {
        @android.annotation.SuppressLint("StaticFieldLeak")
        @Volatile private var instance: SongDownloadRepository? = null

        fun getInstance(context: Context): SongDownloadRepository =
            instance ?: synchronized(this) {
                instance ?: SongDownloadRepository(context.applicationContext).also { instance = it }
            }
    }
}
