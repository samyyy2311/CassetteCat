package `in`.caffeinelabs.cassettecat.data.streaming.subsonic

import `in`.caffeinelabs.cassettecat.data.streaming.await
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import `in`.caffeinelabs.cassettecat.data.playback.lyricsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.security.MessageDigest
import java.util.UUID

class SubsonicApiException(message: String) : Exception(message)

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

    suspend fun getLyrics(songId: String): String? {
        val all = get("getLyricsBySongId.view") { addQueryParameter("id", songId) }.lyricsList?.structuredLyrics.orEmpty()
        val lyrics = all.firstOrNull { it.synced && it.line.isNotEmpty() } ?: all.firstOrNull { it.line.isNotEmpty() }
            ?: return null
        // A positive offset shows the lyrics sooner than their times say.
        return lyricsText(lyrics.line.map { line -> line.start?.let { maxOf(0L, it - lyrics.offset) } to line.value }, lyrics.synced)
    }

    suspend fun savePlayQueue(songIds: List<String>, currentId: String, positionMs: Long) {
        get("savePlayQueue.view") {
            songIds.forEach { addQueryParameter("id", it) }
            addQueryParameter("current", currentId)
            addQueryParameter("position", positionMs.toString())
        }
    }

    suspend fun getPlayQueue(): SubsonicPlayQueue? = get("getPlayQueue.view").playQueue

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

    private suspend fun get(path: String, configure: HttpUrl.Builder.() -> Unit = {}): SubsonicResponse = withContext(Dispatchers.IO) {
        val url = urlBuilder(path).apply(configure).build()
        val response = sharedHttpClient.newCall(Request.Builder().url(url).build()).await()
        val body = response.use { it.body.string() }
        val result = sharedJson.decodeFromString(SubsonicEnvelope.serializer(), body).response
        if (result.status != "ok") {
            throw SubsonicApiException(result.error?.message ?: "Subsonic request to $path failed")
        }
        result
    }

    private fun md5Hex(input: String): String =
        MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val API_VERSION = "1.16.1"
        // The server records this as who saved a queue, so it names the phone too, telling two of them apart.
        val CLIENT_NAME = "CassetteCat Android (${android.os.Build.MODEL})"
        private const val PAGE_SIZE = 500
    }
}
