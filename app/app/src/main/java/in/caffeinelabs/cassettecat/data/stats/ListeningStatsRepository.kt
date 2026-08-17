package `in`.caffeinelabs.cassettecat.data.stats

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.statsDataStore by preferencesDataStore(name = "listening_stats")
private val STATS_KEY = stringPreferencesKey("stats_json")
private val json = Json { ignoreUnknownKeys = true }

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
private data class StatsData(
    // keyed by YearMonth.toString(), e.g. "2026-06"
    val monthly: Map<String, MonthlyStats> = emptyMap(),
    val milestones: List<Milestone> = emptyList()
)

// Monthly aggregates, not a full play-event log — bounded by months-of-use x library size,
// not listen count, same reasoning as the earlier all-time aggregate this replaces.
class ListeningStatsRepository(private val context: Context) {
    val monthlyStats: Flow<Map<String, MonthlyStats>> = context.statsDataStore.data.map { it.decode().monthly }
    val milestones: Flow<List<Milestone>> = context.statsDataStore.data.map { it.decode().milestones }

    suspend fun recordPlay(songId: String, monthKey: String) = update { data ->
        val month = data.monthly.getOrDefault(monthKey, MonthlyStats())
        val updatedMonth = month.copy(
            songPlayCounts = month.songPlayCounts + (songId to (month.songPlayCounts[songId] ?: 0) + 1)
        )
        val monthly = data.monthly + (monthKey to updatedMonth)
        val totalPlays = monthly.values.sumOf { it.songPlayCounts.values.sum() }.toLong()
        data.copy(
            monthly = monthly,
            milestones = withNewMilestones(data.milestones, MilestoneType.SONGS_PLAYED, totalPlays, PLAY_MILESTONES)
        )
    }

    suspend fun addListeningTime(songId: String, monthKey: String, ms: Long) = update { data ->
        val month = data.monthly.getOrDefault(monthKey, MonthlyStats())
        val updatedMonth = month.copy(
            listeningMs = month.listeningMs + ms,
            songListeningMs = month.songListeningMs + (songId to (month.songListeningMs[songId] ?: 0L) + ms)
        )
        val monthly = data.monthly + (monthKey to updatedMonth)
        val totalMinutes = monthly.values.sumOf { it.listeningMs } / 60_000
        data.copy(
            monthly = monthly,
            milestones = withNewMilestones(data.milestones, MilestoneType.MINUTES_PLAYED, totalMinutes, MINUTE_MILESTONES)
        )
    }

    suspend fun clearAll() {
        context.statsDataStore.edit { it.remove(STATS_KEY) }
    }

    // backup restore only: full replace, not a merge
    suspend fun replaceAll(monthly: Map<String, MonthlyStats>, milestones: List<Milestone>) {
        context.statsDataStore.edit { it[STATS_KEY] = json.encodeToString(StatsData(monthly, milestones)) }
    }

    private fun withNewMilestones(
        current: List<Milestone>,
        type: MilestoneType,
        newTotal: Long,
        thresholds: List<Long>
    ): List<Milestone> {
        val alreadyReached = current.filter { it.type == type }.map { it.thresholdValue }.toSet()
        val newlyReached = thresholds.filter { it <= newTotal && it !in alreadyReached }
        return current + newlyReached.map { Milestone(type, it, System.currentTimeMillis()) }
    }

    private suspend fun update(transform: (StatsData) -> StatsData) {
        context.statsDataStore.edit { prefs -> prefs[STATS_KEY] = json.encodeToString(transform(prefs.decode())) }
    }

    private fun Preferences.decode(): StatsData =
        this[STATS_KEY]?.let { runCatching { json.decodeFromString<StatsData>(it) }.getOrNull() } ?: StatsData()
}
