package `in`.caffeinelabs.cassettecat.data.streaming.subsonic

import `in`.caffeinelabs.cassettecat.data.library.LibraryRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first

class SubsonicLibraryRepository(
    private val serverRepository: StreamingServerRepository,
    private val credentialStore: CredentialStore
) : LibraryRepository {

    override suspend fun getSongs(): List<Song> {
        val client = client() ?: return emptyList()
        return client.getAllAlbumIds()
            .map { albumId -> client.getAlbum(albumId) }
            .flatMap { album -> album.song.map { it.toSong(client, album) } }
    }

    override suspend fun setFavorite(songId: String, favorite: Boolean) {
        val client = client() ?: return
        val rawId = songId.removePrefix("subsonic:")
        if (favorite) client.star(rawId) else client.unstar(rawId)
    }

    /** Saves the queue from [current] on, at [positionMs] into it; songs from elsewhere are left out. */
    suspend fun saveQueue(current: Song, upNext: List<Song>, positionMs: Long) {
        val client = client() ?: return
        val ids = (listOf(current) + upNext).filter { it.source == MusicSource.Subsonic }.take(MAX_SAVED_QUEUE)
            .map { it.id.removePrefix("subsonic:") }
        if (ids.isEmpty() || current.source != MusicSource.Subsonic) return
        runCatching { client.savePlayQueue(ids, current.id.removePrefix("subsonic:"), positionMs) }
    }

    /** The queue another player left on the server, or null when this app saved it last or there is none. */
    suspend fun queueFromElsewhere(): SubsonicPlayQueue? {
        val client = client() ?: return null
        val queue = runCatching { client.getPlayQueue() }.getOrNull() ?: return null
        return queue.takeIf { it.entry.isNotEmpty() && it.changedBy != null && it.changedBy != SubsonicApiClient.CLIENT_NAME }
    }

    // Servers without OpenSubsonic don't know the request, which counts as no lyrics.
    override suspend fun lyrics(songId: String): String? =
        client()?.let { client ->
            runCatching { client.getLyrics(songId.removePrefix("subsonic:")) }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }.getOrNull()
        }

    private suspend fun client(): SubsonicApiClient? {
        val config = serverRepository.config(StreamingProtocol.SUBSONIC).first()
        val password = credentialStore.getSubsonicPassword()
        if (!config.connected || config.serverUrl.isBlank() || password == null) return null
        return SubsonicApiClient(config.serverUrl, config.username, password)
    }
}

// Enough to carry on with, and short enough for the request's address.
private const val MAX_SAVED_QUEUE = 200

private fun SubsonicSong.toSong(client: SubsonicApiClient, album: SubsonicAlbum): Song {
    val coverArtId = coverArt ?: album.coverArt
    return Song(
        id = "subsonic:$id",
        title = title,
        artist = artist ?: "Unknown artist",
        album = album.name,
        albumId = "subsonic:${album.id}",
        durationMs = duration * 1000,
        contentUri = client.streamUrl(id).toUri(),
        source = MusicSource.Subsonic,
        artUri = coverArtId?.let { client.coverArtUrl(it).toUri() },
        isFavorite = starred != null,
        genres = listOfNotNull(genre),
        releaseYear = album.year
    )
}
