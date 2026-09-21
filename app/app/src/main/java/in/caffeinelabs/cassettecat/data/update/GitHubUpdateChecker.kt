package `in`.caffeinelabs.cassettecat.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

private const val RELEASES_URL = "https://api.github.com/repos/samyyy2311/CassetteCat/releases/latest"

@Serializable
private data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long = 0L
)

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val assets: List<GitHubAsset> = emptyList()
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class UpdateAvailable(
        val version: String,
        val url: String,
        val downloadUrl: String? = null,
        val apkSize: Long = 0L
    ) : UpdateCheckResult
    data object Error : UpdateCheckResult
}

class GitHubUpdateChecker {
    private val downloadClient by lazy {
        sharedHttpClient.newBuilder()
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .build()
            val body = sharedHttpClient.newCall(request).execute().use {
                if (!it.isSuccessful) return@runCatching UpdateCheckResult.Error
                it.body.string()
            }
            val release = sharedJson.decodeFromString<GitHubRelease>(body)
            val latestVersion = release.tag_name.removePrefix("v")
            if (isNewer(latestVersion, currentVersion)) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                UpdateCheckResult.UpdateAvailable(
                    version = latestVersion,
                    url = release.html_url,
                    downloadUrl = apkAsset?.browser_download_url,
                    apkSize = apkAsset?.size ?: 0L
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        }.getOrDefault(UpdateCheckResult.Error)
    }

    suspend fun downloadApk(
        downloadUrl: String,
        destination: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val parent = destination.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val tempFile = File(parent, "${destination.name}.tmp")
            val request = Request.Builder().url(downloadUrl).build()
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching false
                val body = response.body
                val totalBytes = body.contentLength()
                var bytesRead = 0L
                tempFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (totalBytes > 0) {
                                onProgress((bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                if (destination.exists()) destination.delete()
                tempFile.renameTo(destination)
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun cleanOldUpdates(cacheDir: File) {
        runCatching {
            val updatesDir = File(cacheDir, "updates")
            if (updatesDir.exists()) {
                updatesDir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean = runCatching {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

internal fun isNewer(latest: String, current: String): Boolean {
    val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
        val l = latestParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (l != c) return l > c
    }
    return false
}
