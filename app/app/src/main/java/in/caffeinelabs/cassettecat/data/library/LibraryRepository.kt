package `in`.caffeinelabs.cassettecat.data.library

// Interface justified by three real implementations (Local/Subsonic/Jellyfin), per
// this project's rule: no interface until there's a second implementation to abstract over.
interface LibraryRepository {
    suspend fun getSongs(): List<Song>
    suspend fun setFavorite(songId: String, favorite: Boolean)
}
