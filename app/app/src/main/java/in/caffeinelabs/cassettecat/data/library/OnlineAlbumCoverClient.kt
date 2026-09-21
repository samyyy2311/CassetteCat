package `in`.caffeinelabs.cassettecat.data.library

import android.graphics.Bitmap
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request

enum class CoverSource(val label: String) {
    ITUNES("iTunes"),
    DEEZER("Deezer")
}

data class OnlineCoverResult(
    val id: String,
    val album: String,
    val artist: String,
    val year: String? = null,
    val previewUrl: String,
    val downloadUrl: String,
    val source: CoverSource,
    val resolutionLabel: String
)

@Serializable
private data class ITunesSearchDto(
    val resultCount: Int = 0,
    val results: List<ITunesItemDto> = emptyList()
)

@Serializable
private data class ITunesItemDto(
    val collectionName: String? = null,
    val artistName: String? = null,
    val artworkUrl100: String? = null,
    val releaseDate: String? = null
)

@Serializable
private data class DeezerSearchDto(
    val data: List<DeezerItemDto> = emptyList()
)

@Serializable
private data class DeezerItemDto(
    val id: Long? = null,
    val title: String? = null,
    val cover_xl: String? = null,
    val cover_big: String? = null,
    val cover_medium: String? = null,
    val artist: DeezerArtistDto? = null
)

@Serializable
private data class DeezerArtistDto(
    val name: String? = null
)

class OnlineAlbumCoverClient {
    suspend fun searchCovers(album: String, artist: String): List<OnlineCoverResult> = withContext(Dispatchers.IO) {
        val cleanAlbum = sanitizeQuery(album)
        val cleanArtist = sanitizeQuery(artist)
        val combinedQuery = "$cleanAlbum $cleanArtist".trim().ifBlank { cleanAlbum.ifBlank { cleanArtist } }
        if (combinedQuery.isBlank()) return@withContext emptyList()

        coroutineScope {
            val primaryResults = queryAllSources(combinedQuery)
            if (primaryResults.size >= 4 || cleanAlbum.isBlank() || cleanAlbum == combinedQuery) {
                return@coroutineScope primaryResults
            }

            val albumOnlyResults = queryAllSources(cleanAlbum)
            (primaryResults + albumOnlyResults).distinctBy { it.downloadUrl }
        }
    }

    private suspend fun queryAllSources(query: String): List<OnlineCoverResult> = coroutineScope {
        val iTunesDeferred = async { searchITunes(query) }
        val deezerDeferred = async { searchDeezer(query) }
        val iTunesResults = iTunesDeferred.await()
        val deezerResults = deezerDeferred.await()
        (iTunesResults + deezerResults).distinctBy { it.downloadUrl }
    }

    private fun searchITunes(query: String): List<OnlineCoverResult> = runCatching {
        val url = "https://itunes.apple.com/search?term=${query.urlEncode()}&entity=album&limit=15"
        val body = getBody(url) ?: return@runCatching emptyList()
        val response = sharedJson.decodeFromString<ITunesSearchDto>(body)
        response.results.mapNotNull { item ->
            val url100 = item.artworkUrl100 ?: return@mapNotNull null
            val albumName = item.collectionName ?: return@mapNotNull null
            val artistName = item.artistName ?: ""
            val year = item.releaseDate?.take(4)?.toIntOrNull()?.toString()
            val highRes = url100
                .replace(Regex("\\d+x\\d+bb[^\"]*"), "1200x1200bb.jpg")
                .replace(Regex("/\\d+x\\d+[^\"]*"), "/1200x1200bb.jpg")
            OnlineCoverResult(
                id = "itunes_${url100.hashCode()}_${albumName.hashCode()}",
                album = albumName,
                artist = artistName,
                year = year,
                previewUrl = url100,
                downloadUrl = highRes,
                source = CoverSource.ITUNES,
                resolutionLabel = "1200x1200"
            )
        }
    }.getOrDefault(emptyList())

    private fun searchDeezer(query: String): List<OnlineCoverResult> = runCatching {
        val url = "https://api.deezer.com/search/album?q=${query.urlEncode()}&limit=15"
        val body = getBody(url) ?: return@runCatching emptyList()
        val response = sharedJson.decodeFromString<DeezerSearchDto>(body)
        response.data.mapNotNull { item ->
            val download = item.cover_xl ?: item.cover_big ?: return@mapNotNull null
            val preview = item.cover_medium ?: item.cover_big ?: download
            val albumName = item.title ?: return@mapNotNull null
            val artistName = item.artist?.name ?: ""
            OnlineCoverResult(
                id = "deezer_${item.id ?: item.hashCode()}",
                album = albumName,
                artist = artistName,
                year = null,
                previewUrl = preview,
                downloadUrl = download,
                source = CoverSource.DEEZER,
                resolutionLabel = "1000x1000"
            )
        }
    }.getOrDefault(emptyList())

    suspend fun downloadCover(url: String, fallbackUrl: String? = null): Bitmap? = withContext(Dispatchers.IO) {
        downloadBitmap(url) ?: fallbackUrl?.let { downloadBitmap(it) }
    }

    private fun downloadBitmap(url: String): Bitmap? = runCatching {
        val request = Request.Builder()
            .url(url)
            .build()
        sharedHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            val bytes = response.body.bytes()
            decodeSampledBitmap(bytes, maxDimension = 1440)
        }
    }.getOrNull()

    private fun getBody(url: String): String? =
        sharedHttpClient.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).execute().use {
            if (!it.isSuccessful) null else it.body.string()
        }

    private fun sanitizeQuery(input: String): String =
        input.replace(Regex("(?i)<unknown>"), "")
            .replace(Regex("(?i)\\[(remastered|deluxe|bonus|explicit|expanded|anniversary|edition|version|mono|stereo|reissue)[^\\]]*\\]"), "")
            .replace(Regex("(?i)\\((remastered|deluxe|bonus|explicit|expanded|anniversary|edition|version|mono|stereo|reissue|feat\\.?)[^\\)]*\\)"), "")
            .replace(Regex("(?i)-\\s*(remastered|deluxe|bonus|expanded|anniversary).*$"), "")
            .trim()

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
}
