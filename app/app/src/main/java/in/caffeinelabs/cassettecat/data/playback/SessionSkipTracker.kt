package `in`.caffeinelabs.cassettecat.data.playback

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

class SessionSkipTracker(
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    private val cooldownMs: Long = SKIP_COOLDOWN_MS
) {
    private val skippedAtMs = ConcurrentHashMap<String, Long>()

    fun recordSkip(songId: String, elapsedMs: Long, durationMs: Long): Boolean {
        if (songId.isBlank()) return false
        val isEarlyFraction = durationMs > 0 && (elapsedMs.toFloat() / durationMs) < EARLY_SKIP_FRACTION
        val isEarlyAbsolute = elapsedMs < EARLY_SKIP_THRESHOLD_MS
        val isEarlySkip = elapsedMs >= MIN_ELIGIBLE_SKIP_MS && (isEarlyAbsolute || isEarlyFraction)

        if (isEarlySkip) {
            skippedAtMs[songId] = clock()
            return true
        }
        return false
    }

    fun recordCompletion(songId: String) {
        skippedAtMs.remove(songId)
    }

    fun isSkipped(songId: String): Boolean {
        val timestamp = skippedAtMs[songId] ?: return false
        if (clock() - timestamp > cooldownMs) {
            skippedAtMs.remove(songId)
            return false
        }
        return true
    }

    fun skippedSongIds(): Set<String> {
        val now = clock()
        skippedAtMs.entries.removeIf { now - it.value > cooldownMs }
        return skippedAtMs.keys.toSet()
    }

    fun clear() {
        skippedAtMs.clear()
    }

    companion object {
        private const val MIN_ELIGIBLE_SKIP_MS = 800L
        private const val EARLY_SKIP_THRESHOLD_MS = 25_000L
        private const val EARLY_SKIP_FRACTION = 0.20f
        private const val SKIP_COOLDOWN_MS = 45 * 60_000L
    }
}
