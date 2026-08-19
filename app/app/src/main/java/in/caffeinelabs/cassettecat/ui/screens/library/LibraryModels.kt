package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.ui.graphics.Color
import com.composables.icons.lucide.R

enum class LibraryViewMode(val label: String) {
    SONGS("Songs"), ARTISTS("Artists"), ALBUMS("Albums"), GENRES("Genres"), PLAYLISTS("Playlists")
}

enum class CollectionLayout { GRID, LIST }

enum class SongFilter(val label: String) {
    ALL("All songs"), FAVORITES("Favorites"), DOWNLOADED("Downloaded"), RECENTLY_ADDED("Recently added")
}

enum class ArtistSortOrder(val label: String) { NAME("Name"), SONG_COUNT("Song Count") }
enum class AlbumSortOrder(val label: String) { ALBUM("Album"), ARTIST("Artist"), SONG_COUNT("Song Count") }
enum class GenreSortOrder(val label: String) { NAME("Name"), SONG_COUNT("Song Count") }

data class M3uImportSummary(val name: String, val matched: Int, val total: Int)

fun ArtistSortOrder.comparator(): Comparator<ArtistGroup> = when (this) {
    ArtistSortOrder.NAME -> compareBy { it.artist.sortKey() }
    ArtistSortOrder.SONG_COUNT -> compareBy { it.songs.size }
}

fun AlbumSortOrder.comparator(): Comparator<AlbumGroup> = when (this) {
    AlbumSortOrder.ALBUM -> compareBy { it.album.sortKey() }
    AlbumSortOrder.ARTIST -> compareBy { it.artist.sortKey() }
    AlbumSortOrder.SONG_COUNT -> compareBy { it.songs.size }
}

fun GenreSortOrder.comparator(): Comparator<GenreGroup> = when (this) {
    GenreSortOrder.NAME -> compareBy { it.genre.sortKey() }
    GenreSortOrder.SONG_COUNT -> compareBy { it.songs.size }
}

data class GenreIconRule(val keywords: Set<String>, val iconRes: Int, val color: Color)

val GENRE_ICON_RULES = listOf(
    GenreIconRule(setOf("rock", "punk", "grunge", "metal", "country", "blues"), R.drawable.lucide_ic_guitar, Color(0xFFA6784A)),
    GenreIconRule(setOf("hip", "rap", "trap", "r&b", "rnb", "soul"), R.drawable.lucide_ic_mic_vocal, Color(0xFF8B6BAE)),
    GenreIconRule(setOf("electro", "dance", "edm", "techno", "house", "trance"), R.drawable.lucide_ic_zap, Color(0xFF4FA8C9)),
    GenreIconRule(setOf("jazz", "classical", "orchestral", "instrumental"), R.drawable.lucide_ic_piano, Color(0xFFB99A4B)),
    GenreIconRule(setOf("reggae", "ska"), R.drawable.lucide_ic_tree_palm, Color(0xFF5C9B6C)),
    GenreIconRule(setOf("folk", "acoustic"), R.drawable.lucide_ic_leaf, Color(0xFF7A8B5C)),
    GenreIconRule(setOf("ambient", "chill", "lofi", "lo-fi"), R.drawable.lucide_ic_waves, Color(0xFF6B7F9E)),
    GenreIconRule(setOf("pop"), R.drawable.lucide_ic_sparkles, Color(0xFFB06B8A)),
    GenreIconRule(setOf("gospel", "christian", "worship"), R.drawable.lucide_ic_church, Color(0xFFC9B37E)),
    GenreIconRule(setOf("podcast"), R.drawable.lucide_ic_radio, Color(0xFF7C8A96))
)

fun genreRuleFor(genre: String): GenreIconRule? {
    val normalized = genre.lowercase()
    return GENRE_ICON_RULES.firstOrNull { rule -> rule.keywords.any { normalized.contains(it) } }
}
