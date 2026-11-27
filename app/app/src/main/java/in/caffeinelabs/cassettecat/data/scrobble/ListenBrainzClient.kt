package `in`.caffeinelabs.cassettecat.data.scrobble

import android.util.Log
import java.io.IOException
import `in`.caffeinelabs.cassettecat.BuildConfig
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl.Companion.toHttpUrl
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
private data class LookupResponse(@SerialName("recording_mbid") val recordingMbid: String? = null)

@Serializable
private data class RecordingFeedback(@SerialName("recording_mbid") val recordingMbid: String, val score: Int)

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

    // Feedback is given for a MusicBrainz recording, which the files don't name, so it's looked up first.
    // True once sent, and also when MusicBrainz has no such recording, as there's then nothing to love.
    // A 4xx answer other than a bad token or the rate limit won't change if the same request is sent again.
    private fun isRefusal(code: Int) = code in 400..499 && code != 401 && code != 429

    suspend fun sendFeedback(token: String, song: Song, loved: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext false
        runCatching {
            // With the album the match is closer, and without it a song still matches when the album is named
            // differently there, such as a deluxe edition.
            fun lookUp(album: String?): String? {
                val url = "$API_BASE/metadata/lookup/".toHttpUrl().newBuilder()
                    .addQueryParameter("artist_name", song.artist)
                    .addQueryParameter("recording_name", song.title)
                    .apply { if (album != null) addQueryParameter("release_name", album) }
                    .build()
                val request = Request.Builder().url(url).header("Authorization", "Token ${token.trim()}").get().build()
                return sharedHttpClient.newCall(request).execute().use { response ->
                    // A lookup turned down for good counts as no match, so the love isn't asked again for weeks.
                    if (isRefusal(response.code)) return null
                    if (!response.isSuccessful) throw IOException("Lookup failed: ${response.code}")
                    sharedJson.decodeFromString<LookupResponse>(response.body.string()).recordingMbid
                }
            }
            val mbid = song.album.takeIf { it.isNotBlank() }?.let(::lookUp) ?: lookUp(null) ?: return@runCatching true
            val body = sharedJson.encodeToString(RecordingFeedback(mbid, if (loved) 1 else 0))
            val feedback = Request.Builder()
                .url("$API_BASE/feedback/recording-feedback")
                .header("Authorization", "Token ${token.trim()}")
                .post(body.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            sharedHttpClient.newCall(feedback).execute().use { it.isSuccessful || isRefusal(it.code) }
        }.onFailure { Log.w("ListenBrainz", "Couldn't send feedback", it) }.getOrDefault(false)
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
