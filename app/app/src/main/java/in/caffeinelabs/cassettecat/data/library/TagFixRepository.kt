package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import android.util.Log
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

private const val BUSY_LIMIT = 3

/** The details a check needs from a song, kept so a saved check can carry on without the library loaded. */
@Serializable
data class TagFixSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val year: Int? = null
)

@Serializable
data class TagFix(val song: TagFixSong, val changes: List<TagChange>)

enum class TagFixProblem { OFF, BUSY }

@Serializable
data class TagFixState(
    val queue: List<TagFixSong> = emptyList(),
    val checked: Int = 0,
    val fixes: List<TagFix> = emptyList(),
    // Fixes turned off in the review, by song id.
    val skipped: Set<String> = emptySet(),
    // Every song checked so far, so a new check of the whole library starts with the others.
    val checkedBefore: Set<String> = emptySet(),
    val running: Boolean = false,
    val problem: TagFixProblem? = null
) {
    val paused: Boolean get() = !running && checked in 1 until queue.size
    val accepted: List<TagFix> get() = fixes.filter { it.song.id !in skipped }
}

/**
 * Checks songs against MusicBrainz one at a time, as fast as its rate limit allows, and collects the details that
 * would change. It outlives the screen, and saves its state so a check survives closing the app.
 */
class TagFixRepository private constructor(context: Context) {
    private val file = File(context.filesDir, "tag_fixes.json")
    private val serviceSettings = ServiceSettingsRepository(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _state = MutableStateFlow(load())
    val state: StateFlow<TagFixState> = _state.asStateFlow()

    /** Checks the songs not checked before; songs picked on purpose go through [check] instead. */
    fun checkLibrary(songs: List<Song>) {
        begin(songs.filter { checkable(it) && it.id !in _state.value.checkedBefore })
    }

    fun check(songs: List<Song>) = begin(songs.filter(::checkable))

    fun checkAllAgain(songs: List<Song>) {
        update { it.copy(checkedBefore = emptySet()) }
        checkLibrary(songs)
    }

    fun resume() {
        if (job?.isActive == true) return
        update { it.copy(running = true, problem = null) }
        job = scope.launch { run() }
    }

    fun stop() {
        job?.cancel()
        update { it.copy(running = false) }
    }

    fun setSkipped(songId: String, skip: Boolean) = update {
        it.copy(skipped = if (skip) it.skipped + songId else it.skipped - songId)
    }

    fun setAllSkipped(skip: Boolean) = update {
        it.copy(skipped = if (skip) it.fixes.map { fix -> fix.song.id }.toSet() else emptySet())
    }

    /** Saves the accepted fixes as tag edits, the way the tag editor does, and returns the songs as they now read. */
    suspend fun apply(songs: List<Song>, overrides: SongMetadataOverridesRepository): List<Song> {
        val byId = songs.associateBy { it.id }
        val results = _state.value.accepted.mapNotNull { fix ->
            val song = byId[fix.song.id] ?: return@mapNotNull null
            fun to(field: TagField) = fix.changes.firstOrNull { it.field == field }?.to
            val year = to(TagField.YEAR)
            applyTagEdits(
                song,
                overrides.overrides.value[song.id],
                TagEdits(title = to(TagField.TITLE), artist = to(TagField.ARTIST), album = to(TagField.ALBUM),
                    yearEdited = year != null, year = year?.toIntOrNull())
            )
        }
        overrides.saveOverrides(results.map { it.first })
        update { it.copy(fixes = emptyList(), skipped = emptySet()) }
        return results.map { it.second }
    }

    private fun begin(songs: List<Song>) {
        job?.cancel()
        update { TagFixState(queue = songs.map(::brief), checkedBefore = it.checkedBefore) }
        if (songs.isNotEmpty()) resume() else update { it.copy(running = false) }
    }

    private suspend fun run() {
        var busyInARow = 0
        while (true) {
            val current = _state.value
            if (current.checked >= current.queue.size) break
            if (!serviceSettings.settings.first().isEnabled(ExternalService.COVER_ART_ARCHIVE)) {
                update { it.copy(running = false, problem = TagFixProblem.OFF) }
                return
            }
            val song = current.queue[current.checked]
            val answer = lookUp(song)
            if (answer == null) {
                busyInARow += 1
                if (busyInARow >= BUSY_LIMIT) {
                    update { it.copy(running = false, problem = TagFixProblem.BUSY) }
                    return
                }
                update { it.copy(checked = it.checked + 1) }
                continue
            }
            busyInARow = 0
            val changes = answer.getOrNull()?.let { tagChanges(song.title, song.artist, song.album, song.year, it) }.orEmpty()
            update {
                it.copy(
                    checked = it.checked + 1,
                    checkedBefore = it.checkedBefore + song.id,
                    fixes = if (changes.isEmpty()) it.fixes else it.fixes + TagFix(song, changes)
                )
            }
        }
        update { it.copy(running = false) }
    }

    /**
     * The match for [song], an empty result without a confident one, or null when MusicBrainz kept refusing. It
     * answers busy when asked too often, so a refused lookup is tried again a few times, further apart.
     */
    private suspend fun lookUp(song: TagFixSong): Result<MusicBrainzTags?>? {
        repeat(BUSY_LIMIT + 1) { attempt ->
            try {
                return Result.success(findMusicBrainzTags(song.title, song.artist, song.album, song.durationMs))
            } catch (e: IOException) {
                Log.w("TagFix", "MusicBrainz refused ${song.title}: ${e.message}")
            } catch (e: SerializationException) {
                return Result.success(null)
            }
            if (attempt < BUSY_LIMIT) delay(2_000L * (attempt + 1))
        }
        return null
    }

    private fun update(transform: (TagFixState) -> TagFixState) {
        _state.update(transform)
        runCatching { file.writeText(sharedJson.encodeToString(TagFixState.serializer(), _state.value)) }
            .onFailure { Log.w("TagFix", "Couldn't save the check", it) }
    }

    private fun load(): TagFixState = runCatching {
        sharedJson.decodeFromString(TagFixState.serializer(), file.readText()).copy(running = false)
    }.getOrDefault(TagFixState())

    private fun checkable(song: Song) = song.source == MusicSource.Local && song.title.isNotBlank()

    private fun brief(song: Song) = TagFixSong(song.id, song.title, song.artist, song.album, song.durationMs, song.releaseYear)

    companion object {
        @Volatile private var instance: TagFixRepository? = null

        fun getInstance(context: Context): TagFixRepository =
            instance ?: synchronized(this) {
                instance ?: TagFixRepository(context.applicationContext).also { instance = it }
            }
    }
}
