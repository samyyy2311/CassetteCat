package `in`.caffeinelabs.cassettecat.data.library

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request

@Serializable
private data class WikipediaExtractResponse(val query: WikipediaQuery? = null)

@Serializable
private data class WikipediaQuery(val pages: Map<String, WikipediaPage>? = null)

@Serializable
private data class WikipediaPage(val extract: String? = null)

@Serializable
private data class AudioDbBiographyResponse(val artists: List<AudioDbBiography>? = null)

@Serializable
private data class AudioDbBiography(val strBiographyEN: String? = null)

data class ArtistBiography(val text: String, val source: String)

// Free, no-auth. A miss (obscure/misspelled title, disambiguation page) just returns
// null: no error surfaced, same as ArtistImageLoader's graceful fallback.
class WikipediaInfoLoader {
    companion object {
        private val summaryCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    }

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
            val bio = fetchSummary("$artist (band)")
                ?: fetchSummary("$artist (musician)")
                ?: fetchSummary("$artist (singer)")
                ?: fetchSummary(artist)
            bio?.let { return ArtistBiography(it, "Wikipedia · CC BY-SA") }
        }
        if (audioDbEnabled) {
            fetchAudioDbBiography(artist)?.let { return ArtistBiography(it, "TheAudioDB") }
        }
        return null
    }

    suspend fun fetchSummary(title: String): String? {
        summaryCache[title]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val encodedTitle = URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
                val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts&exintro=true&explaintext=true&redirects=true&titles=$encodedTitle"
                // Wikimedia rejects anonymous/default HTTP clients on some routes. An identifying
                // User-Agent makes artist summaries work consistently instead of silently falling
                // through to the empty About state.
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "CassetteCat/0.1.0 (https://github.com/samyyy2311/CassetteCat)")
                    .header("Accept", "application/json")
                    .build()
                val body = sharedHttpClient.newCall(request).execute().use {
                    if (!it.isSuccessful) return@runCatching null
                    it.body.string()
                }
                sharedJson.decodeFromString<WikipediaExtractResponse>(body)
                    .query?.pages?.values?.firstOrNull()?.extract?.trim()?.ifEmpty { null }
            }.getOrNull()
        }?.also { summaryCache[title] = it }
    }

    private suspend fun fetchAudioDbBiography(artist: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://www.theaudiodb.com/api/v1/json/123/search.php?s=${URLEncoder.encode(artist, "UTF-8")}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CassetteCat/0.1.0 (https://github.com/samyyy2311/CassetteCat)")
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
