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

    private suspend fun client(): SubsonicApiClient? {
        val config = serverRepository.config(StreamingProtocol.SUBSONIC).first()
        val password = credentialStore.getSubsonicPassword()
        if (!config.connected || config.serverUrl.isBlank() || password == null) return null
        return SubsonicApiClient(config.serverUrl, config.username, password)
    }
}

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
