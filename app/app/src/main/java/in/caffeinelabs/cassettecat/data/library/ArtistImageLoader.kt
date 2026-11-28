package `in`.caffeinelabs.cassettecat.data.library

import android.graphics.Bitmap
import `in`.caffeinelabs.cassettecat.data.streaming.ArtworkCache
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.net.URLEncoder
import kotlinx.serialization.Serializable
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import okhttp3.Request
import java.io.File
import java.io.IOException

private const val THUMBNAIL_CACHE_BYTES = 12 * 1024 * 1024
private const val FULL_CACHE_BYTES = 16 * 1024 * 1024
private const val MISS_KEPT_MS = 7L * 24 * 60 * 60 * 1000

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

// Remote artist image fetching with Deezer and TheAudioDB backends. A picture found is also kept on the phone, in
// [diskDir], so it isn't downloaded again each time the app starts; so is a miss, for a week, as artists the
// services don't know rarely appear there soon after.
class ArtistImageLoader(private val diskDir: File) {
    private val cache = ArtworkCache(THUMBNAIL_CACHE_BYTES, FULL_CACHE_BYTES)

    fun clearCache() = cache.clear()

    fun trimCaches(level: Int) = cache.trim(level)

    fun peek(artist: String, thumbnail: Boolean = true): Bitmap? = cache.get(artist, thumbnail)

    suspend fun load(artist: String, deezerEnabled: Boolean, audioDbEnabled: Boolean, thumbnail: Boolean = true): Bitmap? =
        cache.getOrLoad(artist, thumbnail) { fetch(artist, deezerEnabled, audioDbEnabled, it) }

    private fun fetch(artist: String, deezerEnabled: Boolean, audioDbEnabled: Boolean, maxDimension: Int): Bitmap? {
        val saved = File(diskDir, artist.canonicalArtistName().ifEmpty { "_" } + ".img")
        val missing = File(diskDir, saved.nameWithoutExtension + ".none")
        if (saved.exists()) return decodeSampledBitmap(saved.readBytes(), maxDimension = maxDimension)
        if (missing.exists() && System.currentTimeMillis() - missing.lastModified() < MISS_KEPT_MS) return null
        var reached = false
        for ((enabled, find) in listOf(deezerEnabled to ::deezerPictureUrl, audioDbEnabled to ::audioDbPictureUrl)) {
            if (!enabled) continue
            val url = runCatching { find(artist) }.onSuccess { reached = true }.getOrNull() ?: continue
            val bytes = runCatching { downloadBytes(url) }.getOrNull() ?: continue
            diskDir.mkdirs()
            runCatching { saved.writeBytes(bytes) }
            return decodeSampledBitmap(bytes, maxDimension = maxDimension)
        }
        // A service that answered without the artist is a miss; one that couldn't be reached is asked again.
        if (reached) runCatching { diskDir.mkdirs(); missing.writeText("") }
        return null
    }

    // The picture's address, or null when the service doesn't have the artist; throws when it can't be reached.
    private fun deezerPictureUrl(artist: String): String? {
        val body = getBody("https://api.deezer.com/search/artist?q=${artist.urlEncode()}")
        val match = sharedJson.decodeFromString<DeezerSearchResponse>(body).data
            .firstOrNull { it.name?.isSameArtistAs(artist) == true } ?: return null
        return match.picture_xl ?: match.picture_big ?: match.picture_medium
    }

    private fun audioDbPictureUrl(artist: String): String? {
        val body = getBody("https://www.theaudiodb.com/api/v1/json/123/search.php?s=${artist.urlEncode()}")
        val match = sharedJson.decodeFromString<AudioDbSearchResponse>(body).artists
            ?.firstOrNull { it.strArtist?.isSameArtistAs(artist) == true } ?: return null
        return match.strArtistFanart ?: match.strArtistThumb
    }

    private fun getBody(url: String): String =
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) throw IOException("${it.code} from $url")
            it.body.string()
        }

    private fun downloadBytes(url: String): ByteArray? =
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use {
            if (it.isSuccessful) it.body.bytes() else null
        }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    // Alphanumeric canonical comparison prevents partial search false-positives.
    private fun String.isSameArtistAs(other: String): Boolean =
        canonicalArtistName() == other.canonicalArtistName()

    private fun String.canonicalArtistName(): String =
        lowercase().filter(Char::isLetterOrDigit)
}
