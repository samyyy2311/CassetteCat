package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.ui.graphics.Color
import com.composables.icons.lucide.R

enum class LibraryViewMode(val label: String) {
    SONGS("Songs"), ARTISTS("Artists"), ALBUMS("Albums"), GENRES("Genres"), PLAYLISTS("Playlists"), FOLDERS("Folders")
}

enum class CollectionLayout { GRID, LIST }

enum class SongFilter(val label: String) {
    ALL("All songs"), FAVORITES("Favorites"), DOWNLOADED("Downloaded"), RECENTLY_ADDED("Recently added")
}

enum class ArtistSortOrder(val label: String) { NAME("Name"), SONG_COUNT("Song Count") }
enum class AlbumSortOrder(val label: String) { ALBUM("Album"), ARTIST("Artist"), SONG_COUNT("Song Count") }
enum class GenreSortOrder(val label: String) { NAME("Name"), SONG_COUNT("Song Count") }
enum class FolderSortOrder(val label: String) { NAME("Name"), SONG_COUNT("Song Count") }

data class M3uImportSummary(val name: String, val matched: Int, val total: Int)

fun ArtistSortOrder.comparator(): Comparator<ArtistGroup> = when (this) {
    ArtistSortOrder.NAME -> compareBy { it.artist.sortKey() }
    ArtistSortOrder.SONG_COUNT -> compareBy { it.songs.size }
}

fun FolderSortOrder.comparator(): Comparator<FolderGroup> = when (this) {
    FolderSortOrder.NAME -> compareBy { it.folderName.sortKey() }
    FolderSortOrder.SONG_COUNT -> compareBy { it.songs.size }
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
    GenreIconRule(setOf("bollywood", "indian", "desi", "filmi", "hindi", "punjabi", "tamil", "telugu"), R.drawable.lucide_ic_disc_3, Color(0xFFE65100)),
    GenreIconRule(setOf("rock", "metal", "punk", "grunge", "hard rock", "heavy metal"), R.drawable.lucide_ic_guitar, Color(0xFFD63031)),
    GenreIconRule(setOf("hip", "rap", "trap", "r&b", "rnb", "soul"), R.drawable.lucide_ic_mic_vocal, Color(0xFF8E44AD)),
    GenreIconRule(setOf("dance", "club", "edm", "house"), R.drawable.lucide_ic_audio_lines, Color(0xFF00B894)),
    GenreIconRule(setOf("electro", "techno", "trance", "synth", "electronic"), R.drawable.lucide_ic_zap, Color(0xFF0984E3)),
    GenreIconRule(setOf("pop", "indie pop", "synth-pop"), R.drawable.lucide_ic_disc_3, Color(0xFFE84393)),
    GenreIconRule(setOf("alt", "alternative", "indie", "post-rock"), R.drawable.lucide_ic_compass, Color(0xFFE17055)),
    GenreIconRule(setOf("jazz", "blues", "funk", "fusion"), R.drawable.lucide_ic_music, Color(0xFFF39C12)),
    GenreIconRule(setOf("classical", "orchestral", "instrumental", "piano"), R.drawable.lucide_ic_piano, Color(0xFF6C5CE7)),
    GenreIconRule(setOf("reggae", "ska", "dub"), R.drawable.lucide_ic_tree_palm, Color(0xFF2ECC71)),
    GenreIconRule(setOf("folk", "acoustic", "singer-songwriter"), R.drawable.lucide_ic_leaf, Color(0xFF27AE60)),
    GenreIconRule(setOf("ambient", "chill", "lofi", "lo-fi", "downtempo"), R.drawable.lucide_ic_waves, Color(0xFF4A69BD)),
    GenreIconRule(setOf("soundtrack", "film", "score", "ost", "theme"), R.drawable.lucide_ic_film, Color(0xFFFA8231)),
    GenreIconRule(setOf("gospel", "christian", "worship", "spiritual"), R.drawable.lucide_ic_church, Color(0xFFC9B37E)),
    GenreIconRule(setOf("podcast", "audiobook", "speech"), R.drawable.lucide_ic_radio, Color(0xFF7C8A96))
)

private val FALLBACK_GENRE_COLORS = listOf(
    Color(0xFFE17055),
    Color(0xFF0984E3),
    Color(0xFF8E44AD),
    Color(0xFF00B894),
    Color(0xFFE84393),
    Color(0xFFE65100),
    Color(0xFF6C5CE7),
    Color(0xFF4A69BD)
)

fun genreRuleFor(genre: String): GenreIconRule {
    val normalized = genre.lowercase().trim()
    val match = GENRE_ICON_RULES.firstOrNull { rule ->
        rule.keywords.any { normalized.contains(it) }
    }
    if (match != null) return match
    val fallbackColor = FALLBACK_GENRE_COLORS[kotlin.math.abs(genre.hashCode()) % FALLBACK_GENRE_COLORS.size]
    return GenreIconRule(emptySet(), R.drawable.lucide_ic_disc_3, fallbackColor)
}
