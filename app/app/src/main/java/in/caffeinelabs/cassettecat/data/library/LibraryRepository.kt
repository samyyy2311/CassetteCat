package `in`.caffeinelabs.cassettecat.data.library

interface LibraryRepository {
    suspend fun getSongs(): List<Song>
    suspend fun setFavorite(songId: String, favorite: Boolean)
}
