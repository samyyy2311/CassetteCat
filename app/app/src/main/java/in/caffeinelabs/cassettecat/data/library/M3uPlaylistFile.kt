package `in`.caffeinelabs.cassettecat.data.library

import java.io.File

data class M3uEntry(val title: String?, val artist: String?, val path: String)

// Local songs only: a streamed song's contentUri is a pre-authenticated URL with
// credentials in the query string, same constraint ui/util/ShareSongs.kt already solved.
fun buildM3u(songs: List<Song>): Pair<String, Int> {
    val local = songs.filter { it.source == MusicSource.Local && it.filePath != null }
    val skipped = songs.size - local.size
    val text = buildString {
        appendLine("#EXTM3U")
        local.forEach { song ->
            appendLine("#EXTINF:${song.durationMs / 1000},${song.artist} - ${song.title}")
            appendLine(song.filePath)
        }
    }
    return text to skipped
}

fun parseM3u(text: String): List<M3uEntry> {
    val entries = mutableListOf<M3uEntry>()
    var pendingTitle: String? = null
    var pendingArtist: String? = null
    text.lineSequence().map { it.trim() }.forEach { line ->
        when {
            line.startsWith("#EXTINF:") -> {
                val meta = line.substringAfter(",", "")
                val parts = meta.split(" - ", limit = 2)
                pendingArtist = parts.getOrNull(0)?.trim()?.ifEmpty { null }
                pendingTitle = parts.getOrNull(1)?.trim()?.ifEmpty { null }
            }
            line.isEmpty() || line.startsWith("#") -> Unit
            else -> {
                entries += M3uEntry(pendingTitle, pendingArtist, line)
                pendingTitle = null
                pendingArtist = null
            }
        }
    }
    return entries
}

// filename match first (robust across devices with different absolute path prefixes),
// falling back to title+artist from #EXTINF when present
fun List<Song>.matchM3uEntries(entries: List<M3uEntry>): List<String> {
    val byFileName = filter { it.source == MusicSource.Local && it.filePath != null }
        .associateBy { File(it.filePath!!).name }

    return entries.mapNotNull { entry ->
        byFileName[File(entry.path).name]?.id
            ?: entry.title?.let { title ->
                find {
                    it.title.equals(title, ignoreCase = true) &&
                        (entry.artist == null || it.artist.equals(entry.artist, ignoreCase = true))
                }?.id
            }
    }
}
