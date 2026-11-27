package `in`.caffeinelabs.cassettecat.data.scrobble

import `in`.caffeinelabs.cassettecat.BuildConfig
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import android.util.Log
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

// Libre.fm and Last.fm speak the same Audioscrobbler 2.0 API.
class AudioscrobblerClient private constructor(
    private val apiUrl: String,
    private val apiKey: String,
    private val sharedSecret: String,
    private val signsInWithToken: Boolean
) {

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // The service's answer, with the error code of a request it turned down. Null when it couldn't be reached.
    private class Reply(val body: String, val error: Int?)

    private suspend fun post(params: Map<String, String>): Reply? = withContext(Dispatchers.IO) {
        runCatching {
            val signed = params + ("api_key" to apiKey)
            val signature = md5(signed.toSortedMap().map { "${it.key}${it.value}" }.joinToString("") + sharedSecret)
            val form = FormBody.Builder()
            signed.forEach { (k, v) -> form.add(k, v) }
            form.add("api_sig", signature)
            form.add("format", "json")
            val request = Request.Builder().url(apiUrl).post(form.build()).build()
            sharedHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                val error = errorCode(body)
                if (!response.isSuccessful && error == null) null else Reply(body, error)
            }
        }.getOrNull()
    }

    // A request the service turned down for good, such as a song it doesn't know, is finished with rather than
    // sent again for weeks.
    private fun settled(method: String, reply: Reply?): Boolean {
        if (reply?.error != null && !isTemporaryError(reply.error)) {
            Log.w("Audioscrobbler", "$apiUrl turned down $method with error ${reply.error}")
        }
        return reply != null && (reply.error == null || !isTemporaryError(reply.error))
    }

    suspend fun authenticate(username: String, password: String): String? {
        if (username.isBlank() || password.isBlank()) return null
        val user = username.trim()
        // Libre.fm takes a token made from the password, Last.fm the password itself over HTTPS.
        val credential = if (signsInWithToken) {
            "authToken" to md5(user.lowercase() + md5(password))
        } else {
            "password" to password
        }
        val body = post(mapOf("method" to "auth.getMobileSession", "username" to user, credential))
            ?.takeIf { it.error == null }?.body ?: return null
        return when {
            body.contains("\"key\":\"") -> body.substringAfter("\"key\":\"").substringBefore("\"")
            body.contains("<key>") -> body.substringAfter("<key>").substringBefore("</key>")
            else -> null
        }
    }

    suspend fun updateNowPlaying(sessionKey: String, song: Song): Boolean =
        sessionKey.isNotBlank() && post(trackParams("track.updateNowPlaying", sessionKey, song))?.error == null

    /** Whether the scrobble is done with: taken, or turned down for good. */
    suspend fun scrobble(sessionKey: String, song: Song, timestampSec: Long): Boolean =
        sessionKey.isNotBlank() && settled(
            "track.scrobble",
            post(trackParams("track.scrobble", sessionKey, song) + ("timestamp" to timestampSec.toString()))
        )

    /** Whether the love is done with: taken, or turned down for good. */
    suspend fun love(sessionKey: String, song: Song, loved: Boolean): Boolean {
        val method = if (loved) "track.love" else "track.unlove"
        return sessionKey.isNotBlank() &&
            settled(method, post(mapOf("method" to method, "artist" to song.artist, "track" to song.title, "sk" to sessionKey)))
    }

    private fun trackParams(method: String, sessionKey: String, song: Song): Map<String, String> = buildMap {
        put("method", method)
        put("artist", song.artist)
        put("track", song.title)
        if (song.album.isNotBlank()) put("album", song.album)
        put("sk", sessionKey)
    }

    companion object {
        /** The Audioscrobbler error code in a reply, or null when it has none. */
        internal fun errorCode(body: String): Int? =
            Regex("\"error\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull()

        /**
         * Whether asking again later can succeed: a backend failure (8), a service that's down (11, 16), the rate
         * limit (29), or a session key that needs signing in again (9), after which the queue goes out.
         */
        internal fun isTemporaryError(code: Int): Boolean = code in setOf(8, 9, 11, 16, 29)

        val libreFm = AudioscrobblerClient("https://libre.fm/2.0/", "cassettecat", "cassettecat_secret", signsInWithToken = true)

        /** Null when this build has no Last.fm API account. */
        val lastFm: AudioscrobblerClient? = BuildConfig.LASTFM_API_KEY
            .takeIf { it.isNotBlank() && BuildConfig.LASTFM_API_SECRET.isNotBlank() }
            ?.let { AudioscrobblerClient("https://ws.audioscrobbler.com/2.0/", it, BuildConfig.LASTFM_API_SECRET, signsInWithToken = false) }
    }
}
