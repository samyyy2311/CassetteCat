package `in`.caffeinelabs.cassettecat.data.scrobble

import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val API_BASE = "https://api.listenbrainz.org/1"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

@Serializable
private data class ValidateTokenResponse(
    val valid: Boolean = false,
    @SerialName("user_name") val userName: String? = null
)

@Serializable
private data class TrackMetadata(
    @SerialName("artist_name") val artistName: String,
    @SerialName("track_name") val trackName: String,
    @SerialName("release_name") val releaseName: String? = null
)

@Serializable
private data class ListenPayload(
    @SerialName("listened_at") val listenedAt: Long? = null,
    @SerialName("track_metadata") val trackMetadata: TrackMetadata
)

@Serializable
private data class SubmitListenRequest(
    @SerialName("listen_type") val listenType: String,
    val payload: List<ListenPayload>
)

class ListenBrainzClient {

    suspend fun validateToken(token: String): String? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url("$API_BASE/validate-token")
                .header("Authorization", "Token ${token.trim()}")
                .header("User-Agent", "CassetteCat/1.0.0")
                .get()
                .build()

            sharedHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                val parsed = sharedJson.decodeFromString<ValidateTokenResponse>(body)
                if (parsed.valid) parsed.userName else null
            }
        }.getOrNull()
    }

    suspend fun submitNowPlaying(token: String, song: Song): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext false
        runCatching {
            val requestBody = SubmitListenRequest(
                listenType = "playing_now",
                payload = listOf(
                    ListenPayload(
                        listenedAt = null,
                        trackMetadata = TrackMetadata(
                            artistName = song.artist,
                            trackName = song.title,
                            releaseName = song.album.ifBlank { null }
                        )
                    )
                )
            )

            val request = Request.Builder()
                .url("$API_BASE/submit-listens")
                .header("Authorization", "Token ${token.trim()}")
                .header("User-Agent", "CassetteCat/1.0.0")
                .post(sharedJson.encodeToString(requestBody).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            sharedHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun submitListen(token: String, song: Song, listenedAtUnixSec: Long): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext false
        runCatching {
            val requestBody = SubmitListenRequest(
                listenType = "single",
                payload = listOf(
                    ListenPayload(
                        listenedAt = listenedAtUnixSec,
                        trackMetadata = TrackMetadata(
                            artistName = song.artist,
                            trackName = song.title,
                            releaseName = song.album.ifBlank { null }
                        )
                    )
                )
            )

            val request = Request.Builder()
                .url("$API_BASE/submit-listens")
                .header("Authorization", "Token ${token.trim()}")
                .header("User-Agent", "CassetteCat/1.0.0")
                .post(sharedJson.encodeToString(requestBody).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            sharedHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
