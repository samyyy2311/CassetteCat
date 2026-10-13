package `in`.caffeinelabs.cassettecat.data.library

import android.graphics.Bitmap
import `in`.caffeinelabs.cassettecat.data.streaming.ArtworkCache
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.net.URLEncoder
import kotlinx.serialization.Serializable
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import okhttp3.Request

private const val THUMBNAIL_CACHE_BYTES = 12 * 1024 * 1024
private const val FULL_CACHE_BYTES = 16 * 1024 * 1024

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

// Remote artist image fetching with Deezer and TheAudioDB backends.
class ArtistImageLoader {
    private val cache = ArtworkCache(THUMBNAIL_CACHE_BYTES, FULL_CACHE_BYTES)

    fun clearCache() = cache.clear()

    fun trimCaches(level: Int) = cache.trim(level)

    fun peek(artist: String, thumbnail: Boolean = true): Bitmap? = cache.get(artist, thumbnail)

    suspend fun load(artist: String, deezerEnabled: Boolean, audioDbEnabled: Boolean, thumbnail: Boolean = true): Bitmap? =
        cache.getOrLoad(artist, thumbnail) { fetch(artist, deezerEnabled, audioDbEnabled, it) }

    private fun fetch(artist: String, deezerEnabled: Boolean, audioDbEnabled: Boolean, maxDimension: Int): Bitmap? {
        if (deezerEnabled) fetchFromDeezer(artist, maxDimension)?.let { return it }
        if (audioDbEnabled) fetchFromAudioDb(artist, maxDimension)?.let { return it }
        return null
    }

    private fun fetchFromDeezer(artist: String, maxDimension: Int): Bitmap? = runCatching {
        val url = "https://api.deezer.com/search/artist?q=${artist.urlEncode()}"
        val body = getBody(url) ?: return@runCatching null
        val artist = sharedJson.decodeFromString<DeezerSearchResponse>(body).data
            .firstOrNull { it.name?.isSameArtistAs(artist) == true }
            ?: return@runCatching null
        val pictureUrl = artist.picture_xl ?: artist.picture_big ?: artist.picture_medium
            ?: return@runCatching null
        downloadImage(pictureUrl, maxDimension)
    }.getOrNull()

    private fun fetchFromAudioDb(artist: String, maxDimension: Int): Bitmap? = runCatching {
        val url = "https://www.theaudiodb.com/api/v1/json/123/search.php?s=${artist.urlEncode()}"
        val body = getBody(url) ?: return@runCatching null
        val artist = sharedJson.decodeFromString<AudioDbSearchResponse>(body).artists
            ?.firstOrNull { it.strArtist?.isSameArtistAs(artist) == true }
            ?: return@runCatching null
        val pictureUrl = artist.strArtistFanart ?: artist.strArtistThumb
            ?: return@runCatching null
        downloadImage(pictureUrl, maxDimension)
    }.getOrNull()

    private fun getBody(url: String): String? =
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) null else it.body.string()
        }

    private fun downloadImage(url: String, maxDimension: Int): Bitmap? =
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) return null
            it.body.bytes().let { bytes -> decodeSampledBitmap(bytes, maxDimension = maxDimension) }
        }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    // Alphanumeric canonical comparison prevents partial search false-positives.
    private fun String.isSameArtistAs(other: String): Boolean =
        canonicalArtistName() == other.canonicalArtistName()

    private fun String.canonicalArtistName(): String =
        lowercase().filter(Char::isLetterOrDigit)
}
