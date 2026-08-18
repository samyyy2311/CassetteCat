package `in`.caffeinelabs.cassettecat.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.library.LibraryRepository
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.local.LocalLibraryRepository
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.data.streaming.jellyfin.JellyfinLibraryRepository
import `in`.caffeinelabs.cassettecat.data.streaming.subsonic.SubsonicLibraryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Loaded(val songs: List<Song>, val sourceWarnings: List<String> = emptyList()) : LibraryUiState
}

enum class SongSortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album")
}

enum class SortDirection { ASCENDING, DESCENDING }

fun SortDirection.flipped(): SortDirection =
    if (this == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING

private fun numericNamesLast(value: String): Int =
    if (value.trimStart().firstOrNull()?.isDigit() == true) 1 else 0

fun SongSortOrder.comparator(): Comparator<Song> = when (this) {
    SongSortOrder.TITLE -> compareBy<Song> { numericNamesLast(it.title) }.thenBy { it.title.lowercase() }
    SongSortOrder.ARTIST -> compareBy<Song> { numericNamesLast(it.artist) }.thenBy { it.artist.lowercase() }
    SongSortOrder.ALBUM -> compareBy<Song> { numericNamesLast(it.album) }.thenBy { it.album.lowercase() }
}

data class ArtistGroup(val artist: String, val songs: List<Song>)
data class AlbumGroup(val albumId: String, val album: String, val artist: String, val songs: List<Song>)
data class GenreGroup(val genre: String, val songs: List<Song>)

// misfires on stylized names like "Simon & Garfunkel": no way to tell those apart from credits
private val ARTIST_SPLIT_REGEX = Regex("""\s*(?:,|&|\bfeat\.?\b|\bfeaturing\b|\bft\.?\b)\s*""", RegexOption.IGNORE_CASE)

fun String.splitArtists(): List<String> =
    ARTIST_SPLIT_REGEX.split(this).map { it.trim() }.filter { it.isNotEmpty() }

fun List<Song>.groupedByArtist(): List<ArtistGroup> {
    val byArtist = LinkedHashMap<String, MutableList<Song>>()
    forEach { song -> song.artist.splitArtists().forEach { name -> byArtist.getOrPut(name) { mutableListOf() }.add(song) } }
    return byArtist.map { (artist, songs) -> ArtistGroup(artist, songs) }.sortedBy { it.artist.lowercase() }
}

fun List<Song>.groupedByAlbum(): List<AlbumGroup> =
    groupBy { it.albumId }.map { (id, songs) -> AlbumGroup(id, songs.first().album, songs.first().artist, songs) }
        .sortedBy { it.album.lowercase() }

fun Song.effectiveGenres(): List<String> = genres.ifEmpty { listOf("Unknown Genre") }

fun List<Song>.groupedByGenre(): List<GenreGroup> {
    val byGenre = LinkedHashMap<String, MutableList<Song>>()
    forEach { song -> song.effectiveGenres().forEach { genre -> byGenre.getOrPut(genre) { mutableListOf() }.add(song) } }
    return byGenre.map { (genre, songs) -> GenreGroup(genre, songs) }.sortedBy { it.genre.lowercase() }
}

private data class LabeledSource(val label: String, val repository: LibraryRepository)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val serverRepository = StreamingServerRepository(app)
    private val credentialStore = CredentialStore(app)

    // each repository no-ops if its source isn't configured
    private val sources = listOf(
        LabeledSource("Local", LocalLibraryRepository(app)),
        LabeledSource("Subsonic", SubsonicLibraryRepository(serverRepository, credentialStore)),
        LabeledSource("Jellyfin", JellyfinLibraryRepository(serverRepository, credentialStore))
    )

    // unsorted; re-sorting on setSortOrder doesn't need a re-fetch
    private var loadedSongs: List<Song> = emptyList()
    private var loadedWarnings: List<String> = emptyList()

    private val _sortOrder = MutableStateFlow(SongSortOrder.TITLE)
    val sortOrder: StateFlow<SongSortOrder> = _sortOrder.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading

            // per-source, so one dead server doesn't blank out the others
            val results = coroutineScope {
                sources.map { source -> async { source.label to runCatching { source.repository.getSongs() } } }
                    .map { it.await() }
            }

            loadedSongs = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) }
            loadedWarnings = results.mapNotNull { (label, result) ->
                result.exceptionOrNull()?.let { "$label: couldn't connect" }
            }
            publishLoadedSongs()
        }
    }

    // re-tapping the active field flips direction instead of no-op
    fun setSortOrder(order: SongSortOrder) {
        if (order == _sortOrder.value) {
            _sortDirection.value = _sortDirection.value.flipped()
        } else {
            _sortOrder.value = order
            _sortDirection.value = SortDirection.ASCENDING
        }
        if (_uiState.value is LibraryUiState.Loaded) publishLoadedSongs()
    }

    private fun publishLoadedSongs() {
        _uiState.value = if (loadedSongs.isEmpty() && loadedWarnings.isEmpty()) {
            LibraryUiState.Empty
        } else {
            val comparator = _sortOrder.value.comparator()
                .let { if (_sortDirection.value == SortDirection.DESCENDING) it.reversed() else it }
            LibraryUiState.Loaded(loadedSongs.sortedWith(comparator), loadedWarnings)
        }
    }
}
