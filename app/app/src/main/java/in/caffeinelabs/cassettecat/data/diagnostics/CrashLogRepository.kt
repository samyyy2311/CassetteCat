package `in`.caffeinelabs.cassettecat.data.diagnostics

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashLogRepository(context: Context) {
    private val logFile = File(context.filesDir, "crash_log.txt")

    fun install() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { appendCrash(throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun hasCrashLog(): Boolean = logFile.length() > 0

    fun readCrashLog(): String = if (logFile.exists()) logFile.readText() else ""

    fun clearCrashLog() {
        logFile.delete()
    }

    private fun appendCrash(throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val existing = if (logFile.exists()) logFile.readText() else ""
        val combined = "=== $timestamp ===\n$stackTrace\n$existing"
        logFile.writeText(combined.take(MAX_LOG_CHARS))
    }

    companion object {
        private const val MAX_LOG_CHARS = 50_000
    }
}
