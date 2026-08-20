package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.PlaylistRepository
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.local.LocalLibraryRepository
import `in`.caffeinelabs.cassettecat.data.radio.RadioBrowserApiClient
import `in`.caffeinelabs.cassettecat.data.radio.RadioFavoritesRepository
import `in`.caffeinelabs.cassettecat.data.radio.toSong
import kotlinx.coroutines.flow.first

private const val ROOT_ID = "root"
private const val LIKED_SONGS_ID = "liked_songs"
private const val PLAYLISTS_ID = "playlists"
private const val ALBUMS_ID = "albums"
private const val ARTISTS_ID = "artists"
private const val ALL_SONGS_ID = "all_songs"
private const val RADIO_ID = "radio"
private const val RADIO_FAVORITES_ID = "radio_favorites"
private const val RADIO_TOP_ID = "radio_top"
private const val PLAYLIST_PREFIX = "playlist:"
private const val ALBUM_PREFIX = "album:"
private const val ARTIST_PREFIX = "artist:"
private const val RADIO_STATION_PREFIX = "radio:"
private const val EMPTY_LIBRARY_MESSAGE_ID = "empty_library_message"
private const val EMPTY_RADIO_MESSAGE_ID = "empty_radio_message"
private const val SEARCH_LIBRARY_LIMIT = 25
private const val SEARCH_RADIO_LIMIT = 15

class MediaLibraryTree(context: Context) {
    private val localLibrary = LocalLibraryRepository(context)
    private val playlistRepository = PlaylistRepository(context)
    private val favoritesRepository = FavoritesRepository(context)
    private val radioFavoritesRepository = RadioFavoritesRepository(context)
    private val radioBrowserApiClient = RadioBrowserApiClient()

    // Browsing a car head unit can select an item fetched a moment earlier via children();
    // cache the last-seen batches so item(mediaId) can resolve stations that aren't favorited.
    @Volatile private var cachedRadioResults: Map<String, Song> = emptyMap()

    val rootItem: MediaItem = folderItem(ROOT_ID, "CassetteCat")

    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == ROOT_ID -> rootItem
        mediaId == LIKED_SONGS_ID -> folderItem(LIKED_SONGS_ID, "Liked Songs")
        mediaId == PLAYLISTS_ID -> folderItem(PLAYLISTS_ID, "Playlists")
        mediaId == ALBUMS_ID -> folderItem(ALBUMS_ID, "Albums")
        mediaId == ARTISTS_ID -> folderItem(ARTISTS_ID, "Artists")
        mediaId == ALL_SONGS_ID -> folderItem(ALL_SONGS_ID, "All Songs")
        mediaId == RADIO_ID -> folderItem(RADIO_ID, "Radio")
        mediaId == RADIO_FAVORITES_ID -> folderItem(RADIO_FAVORITES_ID, "Favorites")
        mediaId == RADIO_TOP_ID -> folderItem(RADIO_TOP_ID, "Top Stations")
        mediaId == EMPTY_LIBRARY_MESSAGE_ID -> messageItem()
        mediaId == EMPTY_RADIO_MESSAGE_ID -> messageItem(EMPTY_RADIO_MESSAGE_ID, "No favorite radio stations yet")

        mediaId.startsWith(RADIO_STATION_PREFIX) -> {
            val favoriteSong = radioFavoritesRepository.favoriteStations.first().map { it.toSong() }.firstOrNull { it.id == mediaId }
            (favoriteSong ?: cachedRadioResults[mediaId])?.toMediaItem()
        }

        mediaId.startsWith(PLAYLIST_PREFIX) -> playlistRepository.playlists.first()
            .firstOrNull { it.id == mediaId.removePrefix(PLAYLIST_PREFIX) }
            ?.let { folderItem(mediaId, it.name) }

        mediaId.startsWith(ALBUM_PREFIX) -> localLibrary.getSongs()
            .firstOrNull { it.albumId == mediaId.removePrefix(ALBUM_PREFIX) }
            ?.let { folderItem(mediaId, it.album) }

        mediaId.startsWith(ARTIST_PREFIX) -> folderItem(mediaId, mediaId.removePrefix(ARTIST_PREFIX))

        else -> localLibrary.getSongs().firstOrNull { it.id == mediaId }?.toMediaItem()
    }

    suspend fun children(parentId: String): List<MediaItem>? {
        val songs = localLibrary.getSongs()
        return when {
            parentId == ROOT_ID -> if (songs.isEmpty()) {
                listOf(messageItem())
            } else {
                listOf(
                    folderItem(LIKED_SONGS_ID, "Liked Songs"),
                    folderItem(PLAYLISTS_ID, "Playlists"),
                    folderItem(ALBUMS_ID, "Albums"),
                    folderItem(ARTISTS_ID, "Artists"),
                    folderItem(ALL_SONGS_ID, "All Songs"),
                    folderItem(RADIO_ID, "Radio")
                )
            }

            parentId == RADIO_ID -> listOf(
                folderItem(RADIO_FAVORITES_ID, "Favorites"),
                folderItem(RADIO_TOP_ID, "Top Stations")
            )

            parentId == RADIO_FAVORITES_ID -> {
                val favorites = radioFavoritesRepository.favoriteStations.first().map { it.toSong() }
                if (favorites.isEmpty()) listOf(messageItem(EMPTY_RADIO_MESSAGE_ID, "No favorite radio stations yet"))
                else favorites.map { it.toMediaItem() }
            }

            parentId == RADIO_TOP_ID -> {
                val top = runCatching { radioBrowserApiClient.topStations() }.getOrDefault(emptyList()).map { it.toSong() }
                cacheRadioResults(top)
                top.map { it.toMediaItem() }
            }

            parentId == LIKED_SONGS_ID -> {
                val liked = favoritesRepository.favoriteIds.first()
                songs.filter { it.id in liked }.map { it.toMediaItem() }
            }

            parentId == ALL_SONGS_ID -> songs.map { it.toMediaItem() }

            parentId == PLAYLISTS_ID -> playlistRepository.playlists.first()
                .map { folderItem(PLAYLIST_PREFIX + it.id, it.name) }

            parentId.startsWith(PLAYLIST_PREFIX) -> {
                val playlist = playlistRepository.playlists.first()
                    .firstOrNull { it.id == parentId.removePrefix(PLAYLIST_PREFIX) } ?: return emptyList()
                val songsById = songs.associateBy { it.id }
                playlist.songIds.mapNotNull { songsById[it] }.map { it.toMediaItem() }
            }

            parentId == ALBUMS_ID -> songs.groupBy { it.albumId }
                .map { (albumId, albumSongs) -> folderItem(ALBUM_PREFIX + albumId, albumSongs.first().album) }
                .sortedBy { it.mediaMetadata.title?.toString()?.lowercase() }

            parentId.startsWith(ALBUM_PREFIX) ->
                songs.filter { it.albumId == parentId.removePrefix(ALBUM_PREFIX) }.map { it.toMediaItem() }

            parentId == ARTISTS_ID -> songs.map { it.artist }.distinct().sortedBy { it.lowercase() }
                .map { folderItem(ARTIST_PREFIX + it, it) }

            parentId.startsWith(ARTIST_PREFIX) ->
                songs.filter { it.artist == parentId.removePrefix(ARTIST_PREFIX) }.map { it.toMediaItem() }

            else -> null
        }
    }

    // Backs both onSearch/onGetSearchResult: local library by title/artist, plus a live
    // Radio Browser lookup, so voice search ("play jazz fm") can resolve to a real station.
    suspend fun search(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        val librarySongs = localLibrary.getSongs()
            .filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
            .take(SEARCH_LIBRARY_LIMIT)
        val radioSongs = runCatching { radioBrowserApiClient.search(query, limit = SEARCH_RADIO_LIMIT) }
            .getOrDefault(emptyList())
            .map { it.toSong() }
        cacheRadioResults(radioSongs)
        return (librarySongs + radioSongs).map { it.toMediaItem() }
    }

    private fun cacheRadioResults(stations: List<Song>) {
        cachedRadioResults = cachedRadioResults + stations.associateBy { it.id }
    }

    private fun folderItem(id: String, title: String): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build()
        )
        .build()

    private fun messageItem(
        id: String = EMPTY_LIBRARY_MESSAGE_ID,
        text: String = "Open CassetteCat on your phone to finish setup"
    ): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(text)
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .build()
        )
        .build()
}
