package `in`.caffeinelabs.cassettecat.data.streaming.jellyfin

import `in`.caffeinelabs.cassettecat.data.library.LibraryRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first

private data class JellyfinSession(val client: JellyfinApiClient, val userId: String, val accessToken: String)

class JellyfinLibraryRepository(
    private val serverRepository: StreamingServerRepository,
    private val credentialStore: CredentialStore
) : LibraryRepository {

    override suspend fun getSongs(): List<Song> {
        val session = session() ?: return emptyList()
        return session.client.getAllAudioItems(session.userId, session.accessToken)
            .map { it.toSong(session.client, session.accessToken) }
    }

    override suspend fun setFavorite(songId: String, favorite: Boolean) {
        val session = session() ?: return
        val rawId = songId.removePrefix("jellyfin:")
        session.client.setFavorite(session.userId, rawId, session.accessToken, favorite)
    }

    private suspend fun session(): JellyfinSession? {
        val config = serverRepository.config(StreamingProtocol.JELLYFIN).first()
        val accessToken = credentialStore.getJellyfinAccessToken()
        val userId = config.userId
        if (!config.connected || config.serverUrl.isBlank() || accessToken == null || userId == null) {
            return null
        }
        val client = JellyfinApiClient(config.serverUrl, serverRepository.deviceId())
        return JellyfinSession(client, userId, accessToken)
    }
}

private fun JellyfinItem.toSong(client: JellyfinApiClient, accessToken: String): Song {
    val artItemId = if (ImageTags?.Primary != null) Id else AlbumId
    return Song(
        id = "jellyfin:$Id",
        title = Name,
        artist = AlbumArtist ?: "Unknown artist",
        album = Album ?: "Unknown album",
        albumId = "jellyfin:${AlbumId ?: Id}",
        durationMs = (RunTimeTicks ?: 0) / 10_000,
        contentUri = client.streamUrl(Id, accessToken).toUri(),
        source = MusicSource.Jellyfin,
        artUri = artItemId?.let { client.imageUrl(it, accessToken).toUri() },
        isFavorite = UserData?.IsFavorite ?: false,
        genres = Genres,
        releaseYear = ProductionYear
    )
}
