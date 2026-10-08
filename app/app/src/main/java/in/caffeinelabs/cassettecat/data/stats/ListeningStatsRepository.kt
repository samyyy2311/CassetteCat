package `in`.caffeinelabs.cassettecat.data.stats

import android.app.backup.BackupManager
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.io.File
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private val Context.statsDataStore by preferencesDataStore(name = "listening_stats")
private val STATS_KEY = stringPreferencesKey("stats_json")

private val MINUTE_MILESTONES = listOf(60L, 300L, 1000L, 5000L, 10000L, 50000L)
private val PLAY_MILESTONES = listOf(50L, 250L, 1000L, 5000L, 10000L)

@Serializable
data class MonthlyStats(
    val songPlayCounts: Map<String, Int> = emptyMap(),
    val listeningMs: Long = 0L,
    val songListeningMs: Map<String, Long> = emptyMap()
)

enum class MilestoneType { MINUTES_PLAYED, SONGS_PLAYED }

@Serializable
data class Milestone(val type: MilestoneType, val thresholdValue: Long, val reachedAtMs: Long)

@Serializable
data class Listen(
    val at: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val genre: String = "",
    val ms: Long,
    val songId: String? = null,
    /** Whether this listen was a play; a skip only adds listening time. */
    val counted: Boolean = true,
    /** Set only on a month's total for a song from before single listens were kept, sent to the computer. */
    val plays: Int? = null
)

val Listen.statsSongId: String get() = songId ?: "song:${title.trim().lowercase()}\u001f${artist.trim().lowercase()}"

val Listen.monthKey: String get() = YearMonth.from(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())).toString()

internal fun monthlyStatsOf(listens: List<Listen>, earlier: Map<String, MonthlyStats>): Map<String, MonthlyStats> {
    val monthly = earlier.toMutableMap()
    listens.forEach { listen ->
        val month = monthly.getOrDefault(listen.monthKey, MonthlyStats())
        val id = listen.statsSongId
        // Plays follow Apple Music, counting once most of a song is heard; listening time counts every minute, as in
        // Apple Music Replay.
        monthly[listen.monthKey] = month.copy(
            songPlayCounts = if (listen.counted) month.songPlayCounts + (id to (month.songPlayCounts[id] ?: 0) + 1) else month.songPlayCounts,
            listeningMs = month.listeningMs + listen.ms,
            songListeningMs = month.songListeningMs + (id to (month.songListeningMs[id] ?: 0L) + listen.ms)
        )
    }
    return monthly
}

// Monthly totals from before the listening log existed are kept as they were, since their single listens are unknown.
@Serializable
private data class StatsData(
    val monthly: Map<String, MonthlyStats> = emptyMap(),
    val milestones: List<Milestone> = emptyList()
)

private object ListeningLog {
    val listens = MutableStateFlow<List<Listen>?>(null)
    val lock = Mutex()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class ListeningStatsRepository(private val context: Context) {
    private val logFile = File(context.filesDir, "listening_log.jsonl")
    private val stored = context.statsDataStore.data.map { it.decode() }

    val listens: Flow<List<Listen>> = ListeningLog.listens.onStart { load() }.filterNotNull()
    val earlierMonthlyStats: Flow<Map<String, MonthlyStats>> = stored.map { it.monthly }
    val monthlyStats: Flow<Map<String, MonthlyStats>> = combine(listens, earlierMonthlyStats, ::monthlyStatsOf)
    val milestones: Flow<List<Milestone>> = stored.map { it.milestones }

    fun record(listen: Listen) {
        ListeningLog.scope.launch { addListens(listOf(listen)) }
    }

    suspend fun addListens(listens: List<Listen>) {
        if (listens.isEmpty()) return
        ListeningLog.lock.withLock {
            val all = loaded() + listens
            withContext(Dispatchers.IO) { logFile.appendText(listens.joinToString("") { sharedJson.encodeToString(it) + "\n" }) }
            ListeningLog.listens.value = all
            context.statsDataStore.edit { prefs ->
                val data = prefs.decode()
                val monthly = monthlyStatsOf(all, data.monthly).values
                val plays = monthly.sumOf { it.songPlayCounts.values.sum() }.toLong()
                val minutes = monthly.sumOf { it.listeningMs } / 60_000
                val milestones = withNewMilestones(data.milestones, MilestoneType.SONGS_PLAYED, plays, PLAY_MILESTONES)
                prefs[STATS_KEY] = sharedJson.encodeToString(
                    data.copy(milestones = withNewMilestones(milestones, MilestoneType.MINUTES_PLAYED, minutes, MINUTE_MILESTONES))
                )
            }
        }
        BackupManager(context).dataChanged()
    }

    suspend fun clearAll() = replaceAll(emptyList(), emptyMap(), emptyList())

    suspend fun replaceAll(listens: List<Listen>, earlierMonthly: Map<String, MonthlyStats>, milestones: List<Milestone>) {
        ListeningLog.lock.withLock {
            withContext(Dispatchers.IO) { logFile.writeText(listens.joinToString("") { sharedJson.encodeToString(it) + "\n" }) }
            ListeningLog.listens.value = listens
            context.statsDataStore.edit { it[STATS_KEY] = sharedJson.encodeToString(StatsData(earlierMonthly, milestones)) }
        }
        BackupManager(context).dataChanged()
    }

    private suspend fun load() = ListeningLog.lock.withLock { loaded() }

    private suspend fun loaded(): List<Listen> = ListeningLog.listens.value ?: withContext(Dispatchers.IO) {
        if (!logFile.exists()) return@withContext emptyList()
        logFile.readLines().mapNotNull { line -> runCatching { sharedJson.decodeFromString<Listen>(line) }.getOrNull() }
    }.also { ListeningLog.listens.value = it }

    private fun withNewMilestones(current: List<Milestone>, type: MilestoneType, newTotal: Long, thresholds: List<Long>): List<Milestone> {
        val alreadyReached = current.filter { it.type == type }.map { it.thresholdValue }.toSet()
        val newlyReached = thresholds.filter { it <= newTotal && it !in alreadyReached }
        return current + newlyReached.map { Milestone(type, it, System.currentTimeMillis()) }
    }

    private fun Preferences.decode(): StatsData =
        this[STATS_KEY]?.let { runCatching { sharedJson.decodeFromString<StatsData>(it) }.getOrNull() } ?: StatsData()
}
