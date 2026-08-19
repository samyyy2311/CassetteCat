package `in`.caffeinelabs.cassettecat.data.streaming.subsonic

import `in`.caffeinelabs.cassettecat.data.streaming.await
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.security.MessageDigest
import java.util.UUID

class SubsonicApiException(message: String) : Exception(message)

// One instance per login/fetch: salt+token computed lazily and cached for the instance's
// lifetime, matching the Subsonic spec (fresh pair needed per credential use, not per request).
class SubsonicApiClient(
    serverUrl: String,
    private val username: String,
    private val password: String
) {
    private val baseUrl = serverUrl.trimEnd('/')
    private val salt = UUID.randomUUID().toString().replace("-", "").take(12)
    private val token: String by lazy { md5Hex(password + salt) }

    suspend fun ping() {
        get("ping.view")
    }

    suspend fun getAllAlbumIds(): List<String> {
        val ids = mutableListOf<String>()
        var offset = 0
        while (true) {
            val page = get("getAlbumList2.view") {
                addQueryParameter("type", "alphabeticalByName")
                addQueryParameter("size", PAGE_SIZE.toString())
                addQueryParameter("offset", offset.toString())
            }.albumList2?.album.orEmpty()
            ids += page.map { it.id }
            if (page.size < PAGE_SIZE) break
            offset += PAGE_SIZE
        }
        return ids
    }

    suspend fun getAlbum(albumId: String): SubsonicAlbum =
        get("getAlbum.view") { addQueryParameter("id", albumId) }.album
            ?: throw SubsonicApiException("Album $albumId missing from getAlbum response")

    suspend fun star(songId: String) {
        get("star.view") { addQueryParameter("id", songId) }
    }

    suspend fun unstar(songId: String) {
        get("unstar.view") { addQueryParameter("id", songId) }
    }

    fun streamUrl(songId: String): String =
        urlBuilder("stream.view").addQueryParameter("id", songId).build().toString()

    fun coverArtUrl(coverArtId: String): String =
        urlBuilder("getCoverArt.view").addQueryParameter("id", coverArtId).build().toString()

    private fun urlBuilder(path: String): HttpUrl.Builder =
        "$baseUrl/rest/$path".toHttpUrl().newBuilder()
            .addQueryParameter("u", username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", API_VERSION)
            .addQueryParameter("c", CLIENT_NAME)
            .addQueryParameter("f", "json")

    private suspend fun get(path: String, configure: HttpUrl.Builder.() -> Unit = {}): SubsonicResponse {
        val url = urlBuilder(path).apply(configure).build()
        val response = sharedHttpClient.newCall(Request.Builder().url(url).build()).await()
        val body = response.use { it.body.string() }
        val result = sharedJson.decodeFromString(SubsonicEnvelope.serializer(), body).response
        if (result.status != "ok") {
            throw SubsonicApiException(result.error?.message ?: "Subsonic request to $path failed")
        }
        return result
    }

    private fun md5Hex(input: String): String =
        MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val API_VERSION = "1.16.1"
        const val CLIENT_NAME = "CassetteCat"
        const val PAGE_SIZE = 500
    }
}
