package `in`.caffeinelabs.cassettecat.data.update

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request

private const val RELEASES_URL = "https://api.github.com/repos/samyyy2311/CassetteCat/releases/latest"
private const val DAY_MS = 24 * 60 * 60 * 1000L

private val Context.updateDataStore by preferencesDataStore(name = "updates")
private val LAST_CHECK = longPreferencesKey("last_check")
private val PROMPTED_VERSION = stringPreferencesKey("prompted_version")
private val SEEN_VERSION = stringPreferencesKey("seen_version")

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val body: String = ""
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class UpdateAvailable(
        val version: String,
        val url: String,
        val notes: List<ReleaseSection> = emptyList()
    ) : UpdateCheckResult
    data object Error : UpdateCheckResult
}

class GitHubUpdateChecker {
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .build()
            val body = sharedHttpClient.newCall(request).execute().use {
                if (!it.isSuccessful) return@runCatching UpdateCheckResult.Error
                it.body.string()
            }
            val release = sharedJson.decodeFromString<GitHubRelease>(body)
            val latestVersion = release.tag_name.removePrefix("v")
            if (isNewer(latestVersion, currentVersion)) {
                UpdateCheckResult.UpdateAvailable(
                    version = latestVersion,
                    url = release.html_url,
                    // The release text is the version's changelog section, so it reads like What's New.
                    notes = releaseNotesFrom(release.body, latestVersion)
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        }.getOrDefault(UpdateCheckResult.Error)
    }
}

internal fun isNewer(latest: String, current: String): Boolean {
    val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
        val l = latestParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (l != c) return l > c
    }
    return false
}

/** An update to offer on launch: GitHub is asked at most once a day, and each version is offered once. */
suspend fun updateToPrompt(context: Context, currentVersion: String): UpdateCheckResult.UpdateAvailable? {
    val prefs = context.updateDataStore.data.first()
    val now = System.currentTimeMillis()
    if (now - (prefs[LAST_CHECK] ?: 0L) < DAY_MS) return null
    context.updateDataStore.edit { it[LAST_CHECK] = now }
    val update = GitHubUpdateChecker().checkForUpdate(currentVersion) as? UpdateCheckResult.UpdateAvailable
    return update?.takeIf { it.version != prefs[PROMPTED_VERSION] }
}

suspend fun markUpdatePrompted(context: Context, version: String) {
    context.updateDataStore.edit { it[PROMPTED_VERSION] = version }
}

/** One part of a release's notes, such as Lyrics or Fixes, with its points. Untitled when the notes have no parts. */
data class ReleaseSection(val title: String?, val notes: List<String>)

/** The changelog's points for [version]: its "### " headings and the lines starting with "* " under them. */
fun releaseNotesFrom(changelog: String, version: String): List<ReleaseSection> {
    val sections = mutableListOf<ReleaseSection>()
    changelog.lineSequence()
        .dropWhile { !it.startsWith("## [$version]") }
        .drop(1)
        .takeWhile { !it.startsWith("## [") }
        .forEach { line ->
            when {
                line.startsWith("### ") -> sections += ReleaseSection(line.removePrefix("### ").trim(), emptyList())
                line.startsWith("* ") -> {
                    val note = line.removePrefix("* ").replace("**", "").replace("`", "").trim()
                    val last = sections.lastOrNull() ?: ReleaseSection(null, emptyList()).also { sections += it }
                    sections[sections.lastIndex] = last.copy(notes = last.notes + note)
                }
            }
        }
    return sections.filter { it.notes.isNotEmpty() }
}

// A test build's "-debug" suffix isn't part of the release it's built from.
fun releaseNotes(context: Context, version: String): List<ReleaseSection> =
    releaseNotesFrom(context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }, version.substringBefore("-"))

/** Release notes to show once after updating to [version]; empty when already seen. */
suspend fun unseenReleaseNotes(context: Context, version: String): List<ReleaseSection> =
    if (context.updateDataStore.data.first()[SEEN_VERSION] == version) emptyList() else releaseNotes(context, version)

suspend fun markReleaseNotesSeen(context: Context, version: String) {
    context.updateDataStore.edit { it[SEEN_VERSION] = version }
}
