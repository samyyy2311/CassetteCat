package `in`.caffeinelabs.cassettecat.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.library.LibraryRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.SongMetadataOverridesRepository
import `in`.caffeinelabs.cassettecat.data.library.local.LocalLibraryRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.DefaultSortMetric
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.data.streaming.jellyfin.JellyfinLibraryRepository
import `in`.caffeinelabs.cassettecat.data.streaming.subsonic.SubsonicLibraryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

private val LEADING_ARTICLE_REGEX = Regex("""^(the|an|a)\s+""", RegexOption.IGNORE_CASE)

fun String.sortKey(): String {
    val withoutArticle = LEADING_ARTICLE_REGEX.find(this)?.let { substring(it.value.length) } ?: this
    val clean = withoutArticle.trimStart('\'', '"', '[', '(', '{', '#', ' ', '\t', '.', '-', '_')
    return clean.lowercase().ifEmpty { lowercase() }
}

private fun symbolOrNumberFirst(value: String): Int {
    val first = value.sortKey().firstOrNull() ?: return 0
    return if (first in 'a'..'z') 1 else 0
}

fun SongSortOrder.comparator(): Comparator<Song> = when (this) {
    SongSortOrder.TITLE -> compareBy<Song> { symbolOrNumberFirst(it.title) }
        .thenBy { it.title.sortKey() }
        .thenBy { it.artist.sortKey() }
    SongSortOrder.ARTIST -> compareBy<Song> { symbolOrNumberFirst(it.artist) }
        .thenBy { it.artist.sortKey() }
        .thenBy { it.title.sortKey() }
    SongSortOrder.ALBUM -> compareBy<Song> { symbolOrNumberFirst(it.album) }
        .thenBy { it.album.sortKey() }
        .thenBy { it.title.sortKey() }
}

data class ArtistGroup(val artist: String, val songs: List<Song>)
data class AlbumGroup(val albumId: String, val album: String, val artist: String, val songs: List<Song>)
data class GenreGroup(val genre: String, val songs: List<Song>)

data class FolderGroup(
    val folderName: String,
    val folderPath: String,
    val parentName: String?,
    val songs: List<Song>,
    val customCoverPath: String? = null
)

// misfires on stylized names like "Simon & Garfunkel": no way to tell those apart from credits
private val ARTIST_SPLIT_REGEX = Regex("""\s*(?:,|&|;|/|\bfeat\.?\b|\bfeaturing\b|\bft\.?\b)\s*""", RegexOption.IGNORE_CASE)

fun String.splitArtists(): List<String> =
    ARTIST_SPLIT_REGEX.split(this).map { it.trim() }.filter { it.isNotEmpty() }

fun List<Song>.groupedByArtist(): List<ArtistGroup> {
    val byArtist = LinkedHashMap<String, MutableList<Song>>()
    forEach { song -> song.artist.splitArtists().forEach { name -> byArtist.getOrPut(name) { mutableListOf() }.add(song) } }
    return byArtist.map { (artist, songs) -> ArtistGroup(artist, songs) }
        .sortedWith(compareBy<ArtistGroup> { symbolOrNumberFirst(it.artist) }.thenBy { it.artist.sortKey() })
}

fun List<Song>.groupedByAlbum(): List<AlbumGroup> =
    groupBy { it.albumId }.map { (id, songs) -> AlbumGroup(id, songs.first().album, songs.first().artist, songs) }
        .sortedWith(compareBy<AlbumGroup> { symbolOrNumberFirst(it.album) }.thenBy { it.album.sortKey() })

fun Song.effectiveGenres(): List<String> = genres.ifEmpty { listOf("Unknown Genre") }

fun List<Song>.groupedByGenre(): List<GenreGroup> {
    val byGenre = LinkedHashMap<String, MutableList<Song>>()
    forEach { song -> song.effectiveGenres().forEach { genre -> byGenre.getOrPut(genre) { mutableListOf() }.add(song) } }
    return byGenre.map { (genre, songs) -> GenreGroup(genre, songs) }
        .sortedWith(compareBy<GenreGroup> { symbolOrNumberFirst(it.genre) }.thenBy { it.genre.sortKey() })
}

fun List<Song>.groupedByFolder(): List<FolderGroup> {
    val byFolder = LinkedHashMap<String, MutableList<Song>>()
    forEach { song ->
        val path = song.filePath
        if (path != null) {
            val parentFile = java.io.File(path).parentFile
            val folderPath = parentFile?.absolutePath ?: "Unknown"
            byFolder.getOrPut(folderPath) { mutableListOf() }.add(song)
        }
    }
    return byFolder.map { (path, songs) ->
        val file = java.io.File(path)
        val name = file.name.ifBlank { path }
        val parentName = file.parentFile?.name?.ifBlank { null }
        FolderGroup(name, path, parentName, songs)
    }.sortedWith(compareBy<FolderGroup> { symbolOrNumberFirst(it.folderName) }.thenBy { it.folderName.sortKey() })
}

private inline fun <reified T : Enum<T>> enumFromNameOrDefault(name: String, default: T): T =
    try { enumValueOf<T>(name) } catch (_: Exception) { default }

private data class LabeledSource(val label: String, val repository: LibraryRepository)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val serverRepository = StreamingServerRepository(app)
    private val credentialStore = CredentialStore(app)
    private val serviceSettingsRepository = ServiceSettingsRepository(app)
    private val appPreferencesRepository = AppPreferencesRepository(app)
    private val metadataOverridesRepo = SongMetadataOverridesRepository.getInstance(app)
    private var refreshJob: Job? = null

    val isOfflineMode: StateFlow<Boolean> = serviceSettingsRepository.settings
        .map { it.offlineBlackoutMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // each repository no-ops if its source isn't configured
    private val sources = listOf(
        LabeledSource("Local", LocalLibraryRepository(app)),
        LabeledSource("Subsonic", SubsonicLibraryRepository(serverRepository, credentialStore)),
        LabeledSource("Jellyfin", JellyfinLibraryRepository(serverRepository, credentialStore))
    )

    // unsorted; re-sorting on setSortOrder doesn't need a re-fetch
    private var rawSongs: List<Song> = emptyList()
    private var loadedSongs: List<Song> = emptyList()
    private var loadedWarnings: List<String> = emptyList()

    private val _sortOrder = MutableStateFlow(SongSortOrder.TITLE)
    val sortOrder: StateFlow<SongSortOrder> = _sortOrder.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _collectionLayout = MutableStateFlow(CollectionLayout.GRID)
    val collectionLayout: StateFlow<CollectionLayout> = _collectionLayout.asStateFlow()

    private val _songFilter = MutableStateFlow(SongFilter.ALL)
    val songFilter: StateFlow<SongFilter> = _songFilter.asStateFlow()

    private val _sourceFilter = MutableStateFlow(LibrarySourceFilter.ALL)
    val sourceFilter: StateFlow<LibrarySourceFilter> = _sourceFilter.asStateFlow()

    private val _availableSources = MutableStateFlow<List<LibrarySourceFilter>>(listOf(LibrarySourceFilter.LOCAL))
    val availableSources: StateFlow<List<LibrarySourceFilter>> = _availableSources.asStateFlow()

    private var lastSubsonicConnected = false
    private var lastJellyfinConnected = false

    private fun updateAvailableSources(subsonicConnected: Boolean = lastSubsonicConnected, jellyfinConnected: Boolean = lastJellyfinConnected) {
        lastSubsonicConnected = subsonicConnected
        lastJellyfinConnected = jellyfinConnected
        val hasSubsonic = subsonicConnected || loadedSongs.any { it.source == MusicSource.Subsonic }
        val hasJellyfin = jellyfinConnected || loadedSongs.any { it.source == MusicSource.Jellyfin }
        val list = mutableListOf<LibrarySourceFilter>()
        if (hasSubsonic || hasJellyfin) {
            list.add(LibrarySourceFilter.ALL)
        }
        list.add(LibrarySourceFilter.LOCAL)
        if (hasSubsonic) list.add(LibrarySourceFilter.SUBSONIC)
        if (hasJellyfin) list.add(LibrarySourceFilter.JELLYFIN)
        _availableSources.value = list
        if (_sourceFilter.value !in list) {
            _sourceFilter.value = if (list.contains(LibrarySourceFilter.ALL)) LibrarySourceFilter.ALL else LibrarySourceFilter.LOCAL
        }
    }

    private val _artistSortOrder = MutableStateFlow(ArtistSortOrder.NAME)
    val artistSortOrder: StateFlow<ArtistSortOrder> = _artistSortOrder.asStateFlow()

    private val _artistSortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val artistSortDirection: StateFlow<SortDirection> = _artistSortDirection.asStateFlow()

    private val _albumSortOrder = MutableStateFlow(AlbumSortOrder.ALBUM)
    val albumSortOrder: StateFlow<AlbumSortOrder> = _albumSortOrder.asStateFlow()

    private val _albumSortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val albumSortDirection: StateFlow<SortDirection> = _albumSortDirection.asStateFlow()

    private val _genreSortOrder = MutableStateFlow(GenreSortOrder.NAME)
    val genreSortOrder: StateFlow<GenreSortOrder> = _genreSortOrder.asStateFlow()

    private val _genreSortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val genreSortDirection: StateFlow<SortDirection> = _genreSortDirection.asStateFlow()

    private val _folderSortOrder = MutableStateFlow(FolderSortOrder.NAME)
    val folderSortOrder: StateFlow<FolderSortOrder> = _folderSortOrder.asStateFlow()

    private val _folderSortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val folderSortDirection: StateFlow<SortDirection> = _folderSortDirection.asStateFlow()

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _lastRefreshAtMs = MutableStateFlow<Long?>(null)
    val lastRefreshAtMs: StateFlow<Long?> = _lastRefreshAtMs.asStateFlow()
    private val _dismissedWarnings = MutableStateFlow<Set<String>>(emptySet())
    val dismissedWarnings: StateFlow<Set<String>> = _dismissedWarnings.asStateFlow()

    fun dismissWarning(warning: String) {
        _dismissedWarnings.value = _dismissedWarnings.value + warning
    }

    init {
        viewModelScope.launch {
            val prefs = appPreferencesRepository.preferences.first()
            _sortOrder.value = SongSortOrder.valueOf(prefs.defaultSortMetric.name)
            _sortDirection.value = enumFromNameOrDefault(prefs.librarySortDirection, SortDirection.ASCENDING)
            _collectionLayout.value = enumFromNameOrDefault(prefs.libraryCollectionLayout, CollectionLayout.GRID)
            _songFilter.value = enumFromNameOrDefault(prefs.librarySongFilter, SongFilter.ALL)
            _sourceFilter.value = enumFromNameOrDefault(prefs.librarySourceFilter, LibrarySourceFilter.ALL)
            _artistSortOrder.value = enumFromNameOrDefault(prefs.libraryArtistSortOrder, ArtistSortOrder.NAME)
            _artistSortDirection.value = enumFromNameOrDefault(prefs.libraryArtistSortDirection, SortDirection.ASCENDING)
            _albumSortOrder.value = enumFromNameOrDefault(prefs.libraryAlbumSortOrder, AlbumSortOrder.ALBUM)
            _albumSortDirection.value = enumFromNameOrDefault(prefs.libraryAlbumSortDirection, SortDirection.ASCENDING)
            _genreSortOrder.value = enumFromNameOrDefault(prefs.libraryGenreSortOrder, GenreSortOrder.NAME)
            _genreSortDirection.value = enumFromNameOrDefault(prefs.libraryGenreSortDirection, SortDirection.ASCENDING)
            isOfflineMode.collect { refresh() }
        }
        viewModelScope.launch {
            combine(
                serverRepository.config(StreamingProtocol.SUBSONIC),
                serverRepository.config(StreamingProtocol.JELLYFIN)
            ) { sub, jelly -> sub.connected to jelly.connected }
                .collect { (sub, jelly) -> updateAvailableSources(sub, jelly) }
        }
        viewModelScope.launch {
            metadataOverridesRepo.overrides.collect {
                if (rawSongs.isNotEmpty()) {
                    loadedSongs = metadataOverridesRepo.applyTo(rawSongs)
                    publishLoadedSongs()
                }
            }
        }
    }

    fun refresh() {
        _dismissedWarnings.value = emptySet()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { loadSongs() }
    }

    private suspend fun loadSongs() {
        if (loadedSongs.isEmpty()) {
            _uiState.value = LibraryUiState.Loading
        }
        _isRefreshing.value = true
        try {
            val offline = isOfflineMode.value
            val activeSources = if (offline) sources.filter { it.label == "Local" } else sources

            // per-source, so one dead server doesn't blank out the others
            val results = coroutineScope {
                activeSources.map { source -> async {
                    source.label to runCatching { source.repository.getSongs() }
                        .onFailure { if (it is CancellationException) throw it }
                } }
                    .map { it.await() }
            }

            val allRaw = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) }
            rawSongs = allRaw
            loadedSongs = metadataOverridesRepo.applyTo(allRaw)
            loadedWarnings = results.mapNotNull { (label, result) ->
                result.exceptionOrNull()?.let { "$label: ${it.message ?: it::class.simpleName ?: "couldn't connect"}" }
            }
            publishLoadedSongs()
            updateAvailableSources()
            _lastRefreshAtMs.value = System.currentTimeMillis()
        } finally {
            _isRefreshing.value = false
        }
    }

    fun updateSongMetadata(updatedSong: Song) {
        rawSongs = rawSongs.map { if (it.id == updatedSong.id) updatedSong else it }
        loadedSongs = loadedSongs.map { if (it.id == updatedSong.id) updatedSong else it }
        publishLoadedSongs()
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
        viewModelScope.launch {
            appPreferencesRepository.setDefaultSortMetric(DefaultSortMetric.valueOf(_sortOrder.value.name))
            appPreferencesRepository.setLibrarySortDirection(_sortDirection.value.name)
        }
    }

    fun setCollectionLayout(layout: CollectionLayout) {
        _collectionLayout.value = layout
        viewModelScope.launch { appPreferencesRepository.setLibraryCollectionLayout(layout.name) }
    }

    fun setSongFilter(filter: SongFilter) {
        _songFilter.value = filter
        viewModelScope.launch { appPreferencesRepository.setLibrarySongFilter(filter.name) }
    }

    fun setSourceFilter(filter: LibrarySourceFilter) {
        _sourceFilter.value = filter
        viewModelScope.launch { appPreferencesRepository.setLibrarySourceFilter(filter.storageKey) }
    }

    fun setArtistSortOrder(order: ArtistSortOrder) {
        if (order == _artistSortOrder.value) {
            _artistSortDirection.value = _artistSortDirection.value.flipped()
        } else {
            _artistSortOrder.value = order
            _artistSortDirection.value = SortDirection.ASCENDING
        }
        viewModelScope.launch {
            appPreferencesRepository.setLibraryArtistSortOrder(_artistSortOrder.value.name)
            appPreferencesRepository.setLibraryArtistSortDirection(_artistSortDirection.value.name)
        }
    }

    fun setAlbumSortOrder(order: AlbumSortOrder) {
        if (order == _albumSortOrder.value) {
            _albumSortDirection.value = _albumSortDirection.value.flipped()
        } else {
            _albumSortOrder.value = order
            _albumSortDirection.value = SortDirection.ASCENDING
        }
        viewModelScope.launch {
            appPreferencesRepository.setLibraryAlbumSortOrder(_albumSortOrder.value.name)
            appPreferencesRepository.setLibraryAlbumSortDirection(_albumSortDirection.value.name)
        }
    }

    fun setGenreSortOrder(order: GenreSortOrder) {
        if (order == _genreSortOrder.value) {
            _genreSortDirection.value = _genreSortDirection.value.flipped()
        } else {
            _genreSortOrder.value = order
            _genreSortDirection.value = SortDirection.ASCENDING
        }
        viewModelScope.launch {
            appPreferencesRepository.setLibraryGenreSortOrder(_genreSortOrder.value.name)
            appPreferencesRepository.setLibraryGenreSortDirection(_genreSortDirection.value.name)
        }
    }

    fun setFolderSortOrder(order: FolderSortOrder) {
        if (order == _folderSortOrder.value) {
            _folderSortDirection.value = _folderSortDirection.value.flipped()
        } else {
            _folderSortOrder.value = order
            _folderSortDirection.value = SortDirection.ASCENDING
        }
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
