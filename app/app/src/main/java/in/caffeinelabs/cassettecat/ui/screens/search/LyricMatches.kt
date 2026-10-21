package `in`.caffeinelabs.cassettecat.ui.screens.search

import `in`.caffeinelabs.cassettecat.data.library.Song
import java.text.Normalizer

private val LYRIC_TAGS = Regex("""\[[^\]]*]|<[^>]*>""")

/** A song's sung lines, as shown and as folded for matching. */
class SongLyrics(val song: Song, val lines: List<String>, val folded: List<String>)

data class LyricMatch(val song: Song, val line: String)

// Lower case without accents or punctuation, so "Don't Stop" finds "dont stop". Apostrophes join the word they're in.
internal fun foldedForSearch(text: String): String {
    val folded = StringBuilder(text.length)
    for (c in Normalizer.normalize(text, Normalizer.Form.NFKD).lowercase()) {
        when {
            c.isLetterOrDigit() -> folded.append(c)
            Character.getType(c) == Character.NON_SPACING_MARK.toInt() || c == '\'' || c == '’' -> Unit
            folded.isNotEmpty() && folded.last() != ' ' -> folded.append(' ')
        }
    }
    return folded.toString().trim()
}

// The sung lines of plain or LRC lyrics, without their times or tags such as [ar:Artist]. A repeated chorus counts once.
internal fun songLyrics(song: Song, lyrics: String): SongLyrics? {
    val lines = lyrics.lines().map { it.replace(LYRIC_TAGS, "").trim() }.filter { it.isNotEmpty() }.distinct()
    return if (lines.isEmpty()) null else SongLyrics(song, lines, lines.map(::foldedForSearch))
}

/**
 * Songs with a line holding every word of [query], best first. Words match from their start, so one still being typed
 * counts, and the words in order rank above the same words anywhere in the line.
 */
internal fun lyricMatches(songs: List<SongLyrics>, query: String, limit: Int = 20): List<LyricMatch> {
    val folded = foldedForSearch(query)
    if (folded.length < 3) return emptyList()
    val words = folded.split(' ')
    return songs.mapNotNull { song ->
        var best = 0
        var bestLine = ""
        for (i in song.folded.indices) {
            val line = " " + song.folded[i]
            val score = when {
                line.contains(" $folded") -> 2
                words.all { line.contains(" $it") } -> 1
                else -> 0
            }
            if (score > best) {
                best = score
                bestLine = song.lines[i]
                if (best == 2) break
            }
        }
        if (best > 0) Triple(best, song.song, bestLine) else null
    }
        .sortedByDescending { it.first }
        .take(limit)
        .map { (_, song, line) -> LyricMatch(song, line) }
}
