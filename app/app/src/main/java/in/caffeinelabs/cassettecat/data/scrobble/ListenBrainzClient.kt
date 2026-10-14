package `in`.caffeinelabs.cassettecat.data.scrobble

import android.util.Log
import `in`.caffeinelabs.cassettecat.BuildConfig
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
private data class ErrorResponse(val error: String = "")

@Serializable
private data class TrackMetadata(
    @SerialName("artist_name") val artistName: String,
    @SerialName("track_name") val trackName: String,
    @SerialName("release_name") val releaseName: String? = null,
    @SerialName("additional_info") val additionalInfo: AdditionalInfo
)

@Serializable
private data class AdditionalInfo(
    @SerialName("submission_client") val submissionClient: String,
    @SerialName("submission_client_version") val submissionClientVersion: String
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

    suspend fun submitNowPlaying(token: String, song: Song): String? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        submit(token, song, listenType = "playing_now", listenedAt = null)
    }

    suspend fun submitListen(token: String, song: Song, listenedAtUnixSec: Long): String? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        submit(token, song, listenType = "single", listenedAt = listenedAtUnixSec)
    }

    /**
     * Empty when ListenBrainz took the listen, the first sentence of its reason when it refused, or null when it
     * couldn't be reached or gave no reason.
     */
    private fun submit(token: String, song: Song, listenType: String, listenedAt: Long?): String? = runCatching {
        val requestBody = SubmitListenRequest(
            listenType = listenType,
            payload = listOf(
                ListenPayload(
                    listenedAt = listenedAt,
                    trackMetadata = TrackMetadata(
                        artistName = song.artist,
                        trackName = song.title,
                        releaseName = song.album.ifBlank { null },
                        additionalInfo = AdditionalInfo("CassetteCat Android", BuildConfig.VERSION_NAME)
                    )
                )
            )
        )
        val request = Request.Builder()
            .url("$API_BASE/submit-listens")
            .header("Authorization", "Token ${token.trim()}")
            .post(sharedJson.encodeToString(requestBody).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        sharedHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) return@use ""
            val reason = runCatching { sharedJson.decodeFromString<ErrorResponse>(response.body.string()).error }
                .getOrDefault("")
            Log.w("ListenBrainz", "Refused $listenType: ${response.code} $reason")
            reason.substringBefore(". ").takeIf { it.isNotBlank() }?.let { if (it.endsWith(".")) it else "$it." }
        }
    }.onFailure { Log.w("ListenBrainz", "Couldn't submit $listenType", it) }.getOrNull()
}
