package `in`.caffeinelabs.cassettecat.data.library

import android.graphics.Bitmap
import android.util.LruCache
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import okhttp3.Request

private const val MAX_CACHE_BYTES = 24 * 1024 * 1024

@Serializable
private data class DeezerSearchResponse(val data: List<DeezerArtist> = emptyList())

@Serializable
private data class DeezerArtist(
    val name: String? = null,
    val picture_xl: String? = null,
    val picture_big: String? = null,
    val picture_medium: String? = null
)

@Serializable
private data class AudioDbSearchResponse(val artists: List<AudioDbArtist>? = null)

@Serializable
private data class AudioDbArtist(
    val strArtist: String? = null,
    val strArtistFanart: String? = null,
    val strArtistThumb: String? = null
)

// Deezer first, TheAudioDB fallback; both no-auth (TheAudioDB's "2" is their published test key)
class ArtistImageLoader {
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun peek(artist: String): Bitmap? = cache.get(artist)

    suspend fun load(artist: String, deezerEnabled: Boolean, audioDbEnabled: Boolean): Bitmap? {
        cache.get(artist)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) { fetch(artist, deezerEnabled, audioDbEnabled) } ?: return null
        cache.put(artist, bitmap)
        return bitmap
    }

    private fun fetch(artist: String, deezerEnabled: Boolean, audioDbEnabled: Boolean): Bitmap? {
        if (deezerEnabled) fetchFromDeezer(artist)?.let { return it }
        if (audioDbEnabled) fetchFromAudioDb(artist)?.let { return it }
        return null
    }

    private fun fetchFromDeezer(artist: String): Bitmap? = runCatching {
        val url = "https://api.deezer.com/search/artist?q=${artist.urlEncode()}"
        val body = getBody(url) ?: return@runCatching null
        val artist = sharedJson.decodeFromString<DeezerSearchResponse>(body).data
            .firstOrNull { it.name?.isSameArtistAs(artist) == true }
            ?: return@runCatching null
        val pictureUrl = artist.picture_xl ?: artist.picture_big ?: artist.picture_medium
            ?: return@runCatching null
        downloadImage(pictureUrl)
    }.getOrNull()

    private fun fetchFromAudioDb(artist: String): Bitmap? = runCatching {
        val url = "https://www.theaudiodb.com/api/v1/json/2/search.php?s=${artist.urlEncode()}"
        val body = getBody(url) ?: return@runCatching null
        val artist = sharedJson.decodeFromString<AudioDbSearchResponse>(body).artists
            ?.firstOrNull { it.strArtist?.isSameArtistAs(artist) == true }
            ?: return@runCatching null
        val pictureUrl = artist.strArtistFanart ?: artist.strArtistThumb
            ?: return@runCatching null
        downloadImage(pictureUrl)
    }.getOrNull()

    private fun getBody(url: String): String? =
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) null else it.body.string()
        }

    private fun downloadImage(url: String): Bitmap? =
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) return null
            it.body.bytes().let { bytes -> decodeSampledBitmap(bytes, maxDimension = 1440) }
        }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    // Search APIs happily return partial-name matches (for example, "Adele" can return
    // unrelated releases that contain the name). A missing portrait is preferable to a
    // confident but incorrect one, so only accept a canonical full-name match.
    private fun String.isSameArtistAs(other: String): Boolean =
        canonicalArtistName() == other.canonicalArtistName()

    private fun String.canonicalArtistName(): String =
        lowercase().filter(Char::isLetterOrDigit)
}
