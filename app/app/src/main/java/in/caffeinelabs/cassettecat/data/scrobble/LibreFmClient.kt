package `in`.caffeinelabs.cassettecat.data.scrobble

import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

private const val LIBREFM_API_URL = "https://libre.fm/2.0/"
private const val LIBREFM_API_KEY = "cassettecat"
private const val LIBREFM_SHARED_SECRET = "cassettecat_secret"

class LibreFmClient {

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateApiSig(params: Map<String, String>): String {
        val sortedString = params.toSortedMap().map { "${it.key}${it.value}" }.joinToString("")
        return md5("$sortedString$LIBREFM_SHARED_SECRET")
    }

    suspend fun authenticate(username: String, password: String): String? = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) return@withContext null
        runCatching {
            val authToken = md5(username.trim().lowercase() + md5(password))
            val params = mapOf(
                "method" to "auth.getMobileSession",
                "username" to username.trim(),
                "authToken" to authToken,
                "api_key" to LIBREFM_API_KEY
            )
            val sig = generateApiSig(params)

            val formBuilder = FormBody.Builder()
            params.forEach { (k, v) -> formBuilder.add(k, v) }
            formBuilder.add("api_sig", sig)
            formBuilder.add("format", "json")

            val request = Request.Builder()
                .url(LIBREFM_API_URL)
                .header("User-Agent", "CassetteCat/1.0.0")
                .post(formBuilder.build())
                .build()

            sharedHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                // Parse session key from JSON or fallback string extraction
                if (body.contains("\"key\":\"")) {
                    body.substringAfter("\"key\":\"").substringBefore("\"")
                } else if (body.contains("<key>")) {
                    body.substringAfter("<key>").substringBefore("</key>")
                } else null
            }
        }.getOrNull()
    }

    suspend fun updateNowPlaying(sessionKey: String, song: Song): Boolean = withContext(Dispatchers.IO) {
        if (sessionKey.isBlank()) return@withContext false
        runCatching {
            val params = mutableMapOf(
                "method" to "track.updateNowPlaying",
                "artist" to song.artist,
                "track" to song.title,
                "sk" to sessionKey,
                "api_key" to LIBREFM_API_KEY
            )
            if (song.album.isNotBlank()) {
                params["album"] = song.album
            }
            val sig = generateApiSig(params)

            val formBuilder = FormBody.Builder()
            params.forEach { (k, v) -> formBuilder.add(k, v) }
            formBuilder.add("api_sig", sig)
            formBuilder.add("format", "json")

            val request = Request.Builder()
                .url(LIBREFM_API_URL)
                .header("User-Agent", "CassetteCat/1.0.0")
                .post(formBuilder.build())
                .build()

            sharedHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun scrobble(sessionKey: String, song: Song, timestampSec: Long): Boolean = withContext(Dispatchers.IO) {
        if (sessionKey.isBlank()) return@withContext false
        runCatching {
            val params = mutableMapOf(
                "method" to "track.scrobble",
                "artist" to song.artist,
                "track" to song.title,
                "timestamp" to timestampSec.toString(),
                "sk" to sessionKey,
                "api_key" to LIBREFM_API_KEY
            )
            if (song.album.isNotBlank()) {
                params["album"] = song.album
            }
            val sig = generateApiSig(params)

            val formBuilder = FormBody.Builder()
            params.forEach { (k, v) -> formBuilder.add(k, v) }
            formBuilder.add("api_sig", sig)
            formBuilder.add("format", "json")

            val request = Request.Builder()
                .url(LIBREFM_API_URL)
                .header("User-Agent", "CassetteCat/1.0.0")
                .post(formBuilder.build())
                .build()

            sharedHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
