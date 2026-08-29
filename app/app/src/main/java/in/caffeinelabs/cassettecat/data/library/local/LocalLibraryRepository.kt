package `in`.caffeinelabs.cassettecat.data.library.local

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.LibraryFolderRepository
import `in`.caffeinelabs.cassettecat.data.library.LibraryRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.SongMetadataOverridesRepository
import `in`.caffeinelabs.cassettecat.data.library.matchesFolderFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val SHORT_CLIP_MAX_MS = 29_999L

// The MediaStore-backed source, always available regardless of any streaming server.
class LocalLibraryRepository(private val context: Context) : LibraryRepository {
    private val folderRepository = LibraryFolderRepository(context)
    private val favoritesRepository = FavoritesRepository(context)
    private val appPreferencesRepository = `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository(context)
    private val overridesRepository = SongMetadataOverridesRepository.getInstance(context)

    override suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        ensureContentObserverRegistered(context)
        val folderConfig = folderRepository.folderFilterConfig.first()
        val favoriteIds = favoritesRepository.favoriteIds.first()
        val appPreferences = appPreferencesRepository.preferences.first()

        val rawSongs = cachedRawSongs ?: run {
            val generation = cacheGeneration
            publishScan(generation, scanMediaStore())
        }

        val songs = rawSongs.filter { song ->
            song.filePath.orEmpty().matchesFolderFilter(folderConfig) &&
                !(appPreferences.ignoreShortAudioClips && song.durationMs in 1..SHORT_CLIP_MAX_MS)
        }.map { it.copy(isFavorite = it.id in favoriteIds) }

        overridesRepository.applyTo(songs)
    }

    private fun scanMediaStore(): List<Song> {
        val hasDirectGenreColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val genreByAudioId = if (hasDirectGenreColumn) null else loadGenresByAudioId()

        val projection = if (hasDirectGenreColumn) {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.GENRE
            )
        } else {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATA
            )
        }
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val songs = mutableListOf<Song>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val genreCol = if (hasDirectGenreColumn) cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE) else -1

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: ""
                val id = cursor.getLong(idCol)
                val songId = "local:$id"
                val genre = if (hasDirectGenreColumn) {
                    cursor.getString(genreCol)?.takeIf { it.isNotBlank() }
                } else {
                    genreByAudioId?.get(id)
                }
                songs += Song(
                    id = songId,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown artist",
                    album = cursor.getString(albumCol) ?: "Unknown album",
                    albumId = "local:${cursor.getLong(albumIdCol)}",
                    durationMs = cursor.getLong(durationCol),
                    contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ),
                    source = MusicSource.Local,
                    isFavorite = false,
                    genres = genre?.let { listOf(it) } ?: emptyList(),
                    releaseYear = cursor.getInt(yearCol).takeIf { it > 0 },
                    dateAddedMs = cursor.getLong(dateAddedCol) * 1_000L,
                    filePath = path
                )
            }
        }
        return songs
    }

    private fun loadGenresByAudioId(): Map<Long, String> {
        val genreNames = mutableMapOf<Long, String>()
        context.contentResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                genreNames[cursor.getLong(idCol)] = name
            }
        }

        val audioIdToGenre = mutableMapOf<Long, String>()
        genreNames.forEach { (genreId, genreName) ->
            context.contentResolver.query(
                MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                null, null, null
            )?.use { cursor ->
                val audioIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                while (cursor.moveToNext()) {
                    audioIdToGenre[cursor.getLong(audioIdCol)] = genreName
                }
            }
        }
        return audioIdToGenre
    }

    override suspend fun setFavorite(songId: String, favorite: Boolean) {
        favoritesRepository.setFavorite(songId, favorite)
    }

    companion object {
        @Volatile private var cachedRawSongs: List<Song>? = null
        @Volatile private var cacheGeneration: Int = 0
        @Volatile private var observerRegistered = false

        // A scan started before invalidation must not overwrite the cache after it,
        // so publication is only honored if the generation hasn't moved since the scan began.
        private fun publishScan(generation: Int, songs: List<Song>): List<Song> {
            synchronized(this) {
                if (generation == cacheGeneration) cachedRawSongs = songs
            }
            return songs
        }

        private fun ensureContentObserverRegistered(context: Context) {
            if (observerRegistered) return
            synchronized(this) {
                if (observerRegistered) return
                observerRegistered = true
                context.applicationContext.contentResolver.registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    true,
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            synchronized(this@Companion) {
                                cachedRawSongs = null
                                cacheGeneration++
                            }
                        }
                    }
                )
            }
        }
    }
}
