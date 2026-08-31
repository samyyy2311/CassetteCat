package `in`.caffeinelabs.cassettecat.data.device

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Request

private const val RELEASES_URL = "https://api.github.com/repos/samyyy2311/CassetteCat/releases/latest"

@Serializable
private data class GitHubReleaseAsset(val name: String, @SerialName("browser_download_url") val browserDownloadUrl: String)

@Serializable
private data class GitHubRelease(@SerialName("tag_name") val tagName: String, val assets: List<GitHubReleaseAsset>)

data class FirmwareUpdateInfo(val version: String, val downloadUrl: String)

sealed interface GitHubReleaseResult {
    data class Found(val info: FirmwareUpdateInfo) : GitHubReleaseResult
    data object NoReleaseYet : GitHubReleaseResult
    data object CheckFailed : GitHubReleaseResult
}

class GitHubFirmwareReleaseClient {
    suspend fun getLatestRelease(): GitHubReleaseResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(RELEASES_URL).build()
            val response = sharedHttpClient.newCall(request).execute()
            response.use {
                when {
                    it.code == 404 -> GitHubReleaseResult.NoReleaseYet
                    !it.isSuccessful -> GitHubReleaseResult.CheckFailed
                    else -> {
                        val release = sharedJson.decodeFromString<GitHubRelease>(it.body.string())
                        val asset = release.assets.firstOrNull { entry -> entry.name.endsWith(".bin") }
                        if (asset == null) GitHubReleaseResult.NoReleaseYet
                        else GitHubReleaseResult.Found(FirmwareUpdateInfo(release.tagName.removePrefix("v"), asset.browserDownloadUrl))
                    }
                }
            }
        }.getOrDefault(GitHubReleaseResult.CheckFailed)
    }
}
