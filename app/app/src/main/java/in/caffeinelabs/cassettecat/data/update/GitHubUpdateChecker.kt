package `in`.caffeinelabs.cassettecat.data.update

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
private const val RELEASES_URL = "https://api.github.com/repos/samyyy2311/CassetteCat/releases/latest"

@Serializable
private data class GitHubRelease(val tag_name: String, val html_url: String)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class UpdateAvailable(val version: String, val url: String) : UpdateCheckResult
    data object Error : UpdateCheckResult
}

// Public releases need no auth. Manual check only, no background polling.
class GitHubUpdateChecker {
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
                UpdateCheckResult.UpdateAvailable(latestVersion, release.html_url)
            } else {
                UpdateCheckResult.UpToDate
            }
        }.getOrDefault(UpdateCheckResult.Error)
    }
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
