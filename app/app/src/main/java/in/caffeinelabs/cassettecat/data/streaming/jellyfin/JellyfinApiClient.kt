package `in`.caffeinelabs.cassettecat.data.streaming.jellyfin

import `in`.caffeinelabs.cassettecat.BuildConfig
import `in`.caffeinelabs.cassettecat.data.streaming.await
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class JellyfinApiException(message: String) : Exception(message)

// What the phone's player decodes; "container|codec" limits a container to that codec.
private const val PLAYABLE_CONTAINERS =
    "mp3,aac,m4a|aac,m4b|aac,mp4|aac,flac,ogg|vorbis,ogg|opus,oga|vorbis,oga|opus,opus,webm|opus,webma|opus,wav"

class JellyfinApiClient(serverUrl: String, private val deviceId: String) {
    private val baseUrl = serverUrl.trimEnd('/')

    suspend fun authenticate(username: String, password: String): JellyfinAuthResult = withContext(Dispatchers.IO) {
        val requestJson = sharedJson.encodeToString(
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
        sharedJson.decodeFromString(JellyfinAuthResult.serializer(), body)
    }

    suspend fun initiateQuickConnect(): QuickConnectInitiateResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/QuickConnect/Initiate")
            .header("Authorization", authorizationHeader(null))
            .post("".toRequestBody(null))
            .build()
        val response = sharedHttpClient.newCall(request).await()
        if (!response.isSuccessful) {
            response.close()
            throw JellyfinApiException("Quick Connect isn't available on this server (${response.code})")
        }
        val body = response.use { it.body.string() }
        sharedJson.decodeFromString(QuickConnectInitiateResult.serializer(), body)
    }

    suspend fun isQuickConnectAuthenticated(secret: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$baseUrl/QuickConnect/Connect".toHttpUrl().newBuilder()
            .addQueryParameter("Secret", secret)
            .build()
        val request = Request.Builder().url(url).build()
        val response = sharedHttpClient.newCall(request).await()
        if (!response.isSuccessful) {
            response.close()
            throw JellyfinApiException("Failed to check Quick Connect status (${response.code})")
        }
        val body = response.use { it.body.string() }
        sharedJson.decodeFromString(QuickConnectStateResult.serializer(), body).Authenticated
    }

    suspend fun authenticateWithQuickConnect(secret: String): JellyfinAuthResult = withContext(Dispatchers.IO) {
        val requestJson = sharedJson.encodeToString(QuickConnectAuthRequest.serializer(), QuickConnectAuthRequest(Secret = secret))
        val request = Request.Builder()
            .url("$baseUrl/Users/AuthenticateWithQuickConnect")
            .header("Authorization", authorizationHeader(null))
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()
        val response = sharedHttpClient.newCall(request).await()
        if (!response.isSuccessful) {
            response.close()
            throw JellyfinApiException("Quick Connect sign-in failed (${response.code})")
        }
        val body = response.use { it.body.string() }
        sharedJson.decodeFromString(JellyfinAuthResult.serializer(), body)
    }

    suspend fun getAllAudioItems(userId: String, accessToken: String): List<JellyfinItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<JellyfinItem>()
        var startIndex = 0
        while (true) {
            val url = "$baseUrl/Users/$userId/Items".toHttpUrl().newBuilder()
                .addQueryParameter("IncludeItemTypes", "Audio")
                .addQueryParameter("Recursive", "true")
                .addQueryParameter("SortBy", "SortName")
                .addQueryParameter("Fields", "Genres,ProductionYear,Path,Artists")
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
            val page = sharedJson.decodeFromString(JellyfinItemsResponse.serializer(), body)
            items += page.Items
            if (page.Items.size < PAGE_SIZE) break
            startIndex += PAGE_SIZE
        }
        items
    }

    suspend fun setFavorite(userId: String, itemId: String, accessToken: String, favorite: Boolean) = withContext(Dispatchers.IO) {
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

    // The universal stream sends a song as it is when the player reads its format, and otherwise has the server
    // convert it to MP3, so ALAC, WMA, APE and other formats the phone can't decode still play.
    fun streamUrl(itemId: String, accessToken: String): String =
        "$baseUrl/Audio/$itemId/universal".toHttpUrl().newBuilder()
            .addQueryParameter("container", PLAYABLE_CONTAINERS)
            .addQueryParameter("transcodingContainer", "mp3")
            .addQueryParameter("transcodingProtocol", "http")
            .addQueryParameter("audioCodec", "mp3")
            .addQueryParameter("audioBitRate", "320000")
            // High enough that lossless files are sent as they are rather than converted to fit.
            .addQueryParameter("maxStreamingBitrate", "140000000")
            .addQueryParameter("deviceId", deviceId)
            .addQueryParameter("api_key", accessToken)
            .build()
            .toString()

    fun imageUrl(itemId: String, accessToken: String): String =
        "$baseUrl/Items/$itemId/Images/Primary".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", accessToken)
            .build()
            .toString()

    // MediaBrowser authorization header format.
    private fun authorizationHeader(accessToken: String?): String {
        val tokenPart = accessToken?.let { ", Token=\"$it\"" }.orEmpty()
        return "MediaBrowser Client=\"CassetteCat\", Device=\"Android\", DeviceId=\"$deviceId\", Version=\"${BuildConfig.VERSION_NAME}\"$tokenPart"
    }

    private companion object {
        const val PAGE_SIZE = 200
    }
}
