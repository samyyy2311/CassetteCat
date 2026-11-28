package `in`.caffeinelabs.cassettecat.data.library

interface LibraryRepository {
    suspend fun getSongs(): List<Song>
    suspend fun setFavorite(songId: String, favorite: Boolean)

    /** The song's lyrics as the server keeps them, timed as LRC when they are, or null when it has none. */
    suspend fun lyrics(songId: String): String? = null
}
