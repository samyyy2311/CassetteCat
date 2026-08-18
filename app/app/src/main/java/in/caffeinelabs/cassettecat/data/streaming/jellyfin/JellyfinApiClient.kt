package `in`.caffeinelabs.cassettecat.data.streaming.jellyfin

import `in`.caffeinelabs.cassettecat.data.streaming.await
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class JellyfinApiException(message: String) : Exception(message)

// One instance per server connection. deviceId is the app's persisted device
// identity (StreamingServerRepository.deviceId()), stable across logins.
class JellyfinApiClient(serverUrl: String, private val deviceId: String) {
    private val baseUrl = serverUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun authenticate(username: String, password: String): JellyfinAuthResult {
        val requestJson = json.encodeToString(
            JellyfinAuthRequest.serializer(),
            JellyfinAuthRequest(Username = username, Pw = password)
        )
        val request = Request.Builder()
            .url("$baseUrl/Users/AuthenticateByName")
            .header("Authorization", authorizationHeader(null))
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()
        val response = sharedHttpClient.newCall(request).await()
        if (!response.isSuccessful) {
            response.close()
            throw JellyfinApiException("Jellyfin login failed (${response.code})")
        }
        val body = response.use { it.body.string() }
        return json.decodeFromString(JellyfinAuthResult.serializer(), body)
    }

    suspend fun getAllAudioItems(userId: String, accessToken: String): List<JellyfinItem> {
        val items = mutableListOf<JellyfinItem>()
        var startIndex = 0
        while (true) {
            val url = "$baseUrl/Users/$userId/Items".toHttpUrl().newBuilder()
                .addQueryParameter("IncludeItemTypes", "Audio")
                .addQueryParameter("Recursive", "true")
                .addQueryParameter("SortBy", "SortName")
                .addQueryParameter("Fields", "Genres,ProductionYear")
                .addQueryParameter("StartIndex", startIndex.toString())
                .addQueryParameter("Limit", PAGE_SIZE.toString())
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", authorizationHeader(accessToken))
                .build()
            val response = sharedHttpClient.newCall(request).await()
            if (!response.isSuccessful) {
                response.close()
                throw JellyfinApiException("Failed to load Jellyfin library (${response.code})")
            }
            val body = response.use { it.body.string() }
            val page = json.decodeFromString(JellyfinItemsResponse.serializer(), body)
            items += page.Items
            if (page.Items.size < PAGE_SIZE) break
            startIndex += PAGE_SIZE
        }
        return items
    }

    suspend fun setFavorite(userId: String, itemId: String, accessToken: String, favorite: Boolean) {
        val requestBuilder = Request.Builder()
            .url("$baseUrl/Users/$userId/FavoriteItems/$itemId")
            .header("Authorization", authorizationHeader(accessToken))
        val request = if (favorite) {
            requestBuilder.post("".toRequestBody(null)).build()
        } else {
            requestBuilder.delete().build()
        }
        val response = sharedHttpClient.newCall(request).await()
        if (!response.isSuccessful) {
            response.close()
            throw JellyfinApiException("Failed to update favorite (${response.code})")
        }
        response.close()
    }

    fun streamUrl(itemId: String, accessToken: String): String =
        "$baseUrl/Audio/$itemId/stream".toHttpUrl().newBuilder()
            .addQueryParameter("static", "true")
            .addQueryParameter("api_key", accessToken)
            .build()
            .toString()

    fun imageUrl(itemId: String, accessToken: String): String =
        "$baseUrl/Items/$itemId/Images/Primary".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", accessToken)
            .build()
            .toString()

    // Deprecated X-Emby-Authorization/X-MediaBrowser-Token headers intentionally not
    // used; this is the current Authorization scheme. Token is omitted pre-login.
    private fun authorizationHeader(accessToken: String?): String {
        val tokenPart = accessToken?.let { ", Token=\"$it\"" }.orEmpty()
        return "MediaBrowser Client=\"CassetteCat\", Device=\"Android\", DeviceId=\"$deviceId\", Version=\"$APP_VERSION\"$tokenPart"
    }

    private companion object {
        const val APP_VERSION = "0.1.0"
        const val PAGE_SIZE = 200
    }
}
