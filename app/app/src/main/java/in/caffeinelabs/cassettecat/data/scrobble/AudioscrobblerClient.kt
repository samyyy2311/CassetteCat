package `in`.caffeinelabs.cassettecat.data.scrobble

import `in`.caffeinelabs.cassettecat.BuildConfig
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.songMatchKey
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import android.util.Log
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
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
        sessionKey.isNotBlank() && post(trackParams("track.updateNowPlaying", sessionKey, song))?.let { it.error == null } == true

    suspend fun scrobble(sessionKey: String, song: Song, timestampSec: Long): Boolean =
        sessionKey.isNotBlank() && settled(
            "track.scrobble",
            post(trackParams("track.scrobble", sessionKey, song) + ("timestamp" to timestampSec.toString()))
        )

    suspend fun love(sessionKey: String, song: Song, loved: Boolean): Boolean {
        val method = if (loved) "track.love" else "track.unlove"
        return sessionKey.isNotBlank() &&
            settled(method, post(mapOf("method" to method, "artist" to song.artist, "track" to song.title, "sk" to sessionKey)))
    }

    suspend fun lovedSongs(username: String): Set<String>? = withContext(Dispatchers.IO) {
        runCatching {
            val keys = mutableSetOf<String>()
            var page = 1
            do {
                val url = apiUrl.toHttpUrl().newBuilder()
                    .addQueryParameter("method", "user.getLovedTracks")
                    .addQueryParameter("user", username)
                    .addQueryParameter("api_key", apiKey)
                    .addQueryParameter("limit", "1000")
                    .addQueryParameter("page", page.toString())
                    .addQueryParameter("format", "json")
                    .build()
                val body = sharedHttpClient.newCall(Request.Builder().url(url).get().build()).execute().use {
                    if (!it.isSuccessful) return@runCatching null
                    it.body.string()
                }
                val (songs, pages) = lovedPage(body) ?: return@runCatching null
                keys += songs
            } while (page++ < pages)
            keys
        }.onFailure { Log.w("Audioscrobbler", "Couldn't read the loved songs from $apiUrl", it) }.getOrNull()
    }

    private fun trackParams(method: String, sessionKey: String, song: Song): Map<String, String> = buildMap {
        put("method", method)
        put("artist", song.artist)
        put("track", song.title)
        if (song.album.isNotBlank()) put("album", song.album)
        put("sk", sessionKey)
    }

    companion object {
        internal fun errorCode(body: String): Int? =
            Regex("\"error\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull()

        /**
         * Whether asking again later can succeed: a backend failure (8), a service that's down (11, 16), the rate
         * limit (29), or a session key that needs signing in again (9), after which the queue goes out.
         */
        internal fun isTemporaryError(code: Int): Boolean = code in setOf(8, 9, 11, 16, 29)

        internal fun lovedPage(body: String): Pair<Set<String>, Int>? {
            val loved = sharedJson.parseToJsonElement(body).jsonObject["lovedtracks"]?.jsonObject ?: return null
            // A page holding a single song gives it as an object rather than a list of one.
            val tracks = when (val track = loved["track"]) {
                is JsonArray -> track.map { it.jsonObject }
                is JsonObject -> listOf(track)
                else -> emptyList()
            }
            val songs = tracks.mapNotNullTo(mutableSetOf()) { track ->
                val title = track["name"]?.jsonPrimitive?.content ?: return@mapNotNullTo null
                val artist = track["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: return@mapNotNullTo null
                songMatchKey(title, artist)
            }
            val pages = loved["@attr"]?.jsonObject?.get("totalPages")?.jsonPrimitive?.content?.toIntOrNull() ?: 1
            return songs to pages
        }

        val libreFm = AudioscrobblerClient("https://libre.fm/2.0/", "cassettecat", "cassettecat_secret", signsInWithToken = true)

        val lastFm: AudioscrobblerClient? = BuildConfig.LASTFM_API_KEY
            .takeIf { it.isNotBlank() && BuildConfig.LASTFM_API_SECRET.isNotBlank() }
            ?.let { AudioscrobblerClient("https://ws.audioscrobbler.com/2.0/", it, BuildConfig.LASTFM_API_SECRET, signsInWithToken = false) }
    }
}
