package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR

import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song

enum class LibraryViewMode(@StringRes val labelRes: Int) {
    SONGS(AppR.string.customization_library_songs),
    ARTISTS(AppR.string.customization_library_artists),
    ALBUMS(AppR.string.customization_library_albums),
    GENRES(AppR.string.customization_library_genres),
    PLAYLISTS(AppR.string.customization_library_playlists),
    FOLDERS(AppR.string.customization_library_folders)
}

enum class CollectionLayout { GRID, LIST }

enum class SongFilter(@StringRes val labelRes: Int) {
    ALL(AppR.string.library_filter_all_songs),
    FAVORITES(AppR.string.library_filter_favorites),
    DOWNLOADED(AppR.string.library_filter_downloaded),
    RECENTLY_ADDED(AppR.string.library_filter_recently_added)
}

enum class LibrarySourceFilter(val storageKey: String) {
    ALL("ALL"),
    LOCAL("LOCAL"),
    SUBSONIC("SUBSONIC"),
    JELLYFIN("JELLYFIN");

    @Composable
    fun displayName(): String = when (this) {
        ALL -> stringResource(AppR.string.library_source_all)
        LOCAL -> stringResource(AppR.string.library_source_local)
        SUBSONIC -> "Subsonic"
        JELLYFIN -> "Jellyfin"
    }
}

fun List<Song>.filterBySource(filter: LibrarySourceFilter): List<Song> = when (filter) {
    LibrarySourceFilter.ALL -> this
    LibrarySourceFilter.LOCAL -> filter { it.source == MusicSource.Local }
    LibrarySourceFilter.SUBSONIC -> filter { it.source == MusicSource.Subsonic }
    LibrarySourceFilter.JELLYFIN -> filter { it.source == MusicSource.Jellyfin }
}

// The sort sheet picks each option's icon and direction wording from what it orders by.
enum class SortKind(@DrawableRes val iconRes: Int) {
    TEXT(R.drawable.lucide_ic_arrow_up_down),
    COUNT(R.drawable.lucide_ic_hash),
    ALBUM(R.drawable.lucide_ic_disc_3),
    ARTIST(R.drawable.lucide_ic_mic_vocal)
}

interface SortOption {
    @get:StringRes val labelRes: Int
    val kind: SortKind
}

enum class ArtistSortOrder(override val labelRes: Int, override val kind: SortKind) : SortOption {
    NAME(AppR.string.library_sort_name, SortKind.TEXT),
    SONG_COUNT(AppR.string.library_sort_song_count, SortKind.COUNT)
}
enum class AlbumSortOrder(override val labelRes: Int, override val kind: SortKind) : SortOption {
    ALBUM(AppR.string.library_sort_album, SortKind.ALBUM),
    ARTIST(AppR.string.library_sort_artist, SortKind.ARTIST),
    SONG_COUNT(AppR.string.library_sort_song_count, SortKind.COUNT)
}
enum class GenreSortOrder(override val labelRes: Int, override val kind: SortKind) : SortOption {
    NAME(AppR.string.library_sort_name, SortKind.TEXT),
    SONG_COUNT(AppR.string.library_sort_song_count, SortKind.COUNT)
}
enum class FolderSortOrder(override val labelRes: Int, override val kind: SortKind) : SortOption {
    NAME(AppR.string.library_sort_name, SortKind.TEXT),
    SONG_COUNT(AppR.string.library_sort_song_count, SortKind.COUNT)
}

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
