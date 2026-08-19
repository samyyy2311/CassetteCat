package `in`.caffeinelabs.cassettecat.data.library

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request

@Serializable
private data class WikipediaSummary(val extract: String? = null)

@Serializable
private data class AudioDbBiographyResponse(val artists: List<AudioDbBiography>? = null)

@Serializable
private data class AudioDbBiography(val strBiographyEN: String? = null)

data class ArtistBiography(val text: String, val source: String)

// Free, no-auth. A miss (obscure/misspelled title, disambiguation page) just returns
// null: no error surfaced, same as ArtistImageLoader's graceful fallback.
class WikipediaInfoLoader {
    /**
     * Album titles such as "Music" and "Utopia" are also ordinary Wikipedia topics. Always
     * qualify the lookup with the primary artist and never fall back to the bare word.
     */
    suspend fun fetchAlbumSummary(album: String, artist: String): String? {
        val primaryArtist = artist.substringBefore(',').substringBefore('&').trim()
        if (album.isBlank() || primaryArtist.isBlank()) return null
        return fetchSummary("$album ($primaryArtist album)")
    }

    suspend fun fetchArtistBiography(
        artist: String,
        wikipediaEnabled: Boolean,
        audioDbEnabled: Boolean
    ): ArtistBiography? {
        if (wikipediaEnabled) {
            fetchSummary(artist)?.let { return ArtistBiography(it, "Wikipedia · CC BY-SA") }
        }
        if (audioDbEnabled) {
            fetchAudioDbBiography(artist)?.let { return ArtistBiography(it, "TheAudioDB") }
        }
        return null
    }

    suspend fun fetchSummary(title: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(title, "UTF-8")}"
            // Wikimedia rejects anonymous/default HTTP clients on some routes. An identifying
            // User-Agent makes artist summaries work consistently instead of silently falling
            // through to the empty About state.
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CassetteCat/0.1.0 (Android music library client)")
                .header("Accept", "application/json")
                .build()
            val body = sharedHttpClient.newCall(request).execute().use {
                if (!it.isSuccessful) return@runCatching null
                it.body.string()
            }
            sharedJson.decodeFromString<WikipediaSummary>(body).extract?.trim()?.ifEmpty { null }
        }.getOrNull()
    }

    private suspend fun fetchAudioDbBiography(artist: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://www.theaudiodb.com/api/v1/json/2/search.php?s=${URLEncoder.encode(artist, "UTF-8")}" 
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CassetteCat/0.1.0 (Android music library client)")
                .build()
            val body = sharedHttpClient.newCall(request).execute().use {
                if (!it.isSuccessful) return@runCatching null
                it.body.string()
            }
            sharedJson.decodeFromString<AudioDbBiographyResponse>(body)
                .artists
                ?.firstOrNull()
                ?.strBiographyEN
                ?.trim()
                ?.ifEmpty { null }
        }.getOrNull()
    }
}
