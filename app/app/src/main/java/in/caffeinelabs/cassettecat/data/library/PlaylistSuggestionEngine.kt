package `in`.caffeinelabs.cassettecat.data.library

import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import java.util.Locale

enum class SuggestionCategory(val label: String) {
    SOUNDTRACK("Soundtracks & Games"),
    GENRE("Genre Essentials"),
    ARTIST("Artist Spotlight"),
    ERA("Time Capsule"),
    HABIT("Listening Habits")
}

data class PlaylistSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String = "cassette",
    val iconRes: Int = R.drawable.lucide_ic_cassette_tape,
    val coverEmoji: String = "🎵",
    val songs: List<Song>,
    val category: SuggestionCategory
)

object PlaylistSuggestionEngine {
    private const val MIN_OST_SONGS = 4
    private const val MIN_GENRE_SONGS = 5
    private const val MIN_ARTIST_SONGS = 5
    private const val MIN_DECADE_SONGS = 4
    private const val MIN_HABIT_SONGS = 4
    private const val MAX_SUGGESTIONS_TOTAL = 8
    private const val MAX_SONGS_PER_SUGGESTION = 50
    private const val LONG_DURATION_MS = 300_000L

    private val OST_KEYWORDS = listOf(
        "ost", "soundtrack", "original soundtrack", "game", "video game", "vgm", "score",
        "chiptune", "8-bit", "16-bit", "nintendo", "playstation", "sega", "capcom",
        "square enix", "final fantasy", "pokemon", "genshin", "minecraft", "zelda",
        "mario", "halo", "doom", "witcher", "elder scrolls", "persona", "undertale"
    )

    fun generateSuggestions(
        allSongs: List<Song>,
        monthlyStats: Map<String, MonthlyStats> = emptyMap(),
        existingPlaylists: List<Playlist> = emptyList(),
        favoriteIds: Set<String> = emptySet(),
        playbackHistory: List<Song> = emptyList()
    ): List<PlaylistSuggestion> {
        if (allSongs.isEmpty()) return emptyList()

        val existingNames = existingPlaylists.map { it.name.lowercase(Locale.ROOT).trim() }.toSet()
        val existingSongSets = existingPlaylists.map { it.songIds.toSet() }.filter { it.isNotEmpty() }.toSet()

        val candidates = mutableListOf<PlaylistSuggestion>()

        val ostSongs = allSongs.filter { song ->
            val genreHit = song.genres.any { g -> OST_KEYWORDS.any { g.lowercase(Locale.ROOT).contains(it) } }
            val albumHit = OST_KEYWORDS.any { song.album.lowercase(Locale.ROOT).contains(it) }
            val titleHit = OST_KEYWORDS.any { song.title.lowercase(Locale.ROOT).contains(it) }
            genreHit || albumHit || titleHit
        }.distinctBy { it.id }.take(MAX_SONGS_PER_SUGGESTION)

        if (ostSongs.size >= MIN_OST_SONGS) {
            candidates.add(
                PlaylistSuggestion(
                    id = "suggestion_soundtracks",
                    title = "Game & Soundtrack Vault",
                    description = "${ostSongs.size} tracks from games and scores",
                    iconKey = "gamepad",
                    iconRes = R.drawable.lucide_ic_gamepad,
                    coverEmoji = "🎮",
                    songs = ostSongs,
                    category = SuggestionCategory.SOUNDTRACK
                )
            )
        }

        val genreGroups = mutableMapOf<String, MutableList<Song>>()
        val genreDisplayLabels = mutableMapOf<String, String>()
        for (song in allSongs) {
            for (genre in song.genres) {
                val trimmed = genre.trim()
                val key = trimmed.lowercase(Locale.ROOT)
                if (trimmed.isBlank() || isIgnoredGenre(key)) continue
                genreDisplayLabels.getOrPut(key) {
                    trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                }
                genreGroups.getOrPut(key) { mutableListOf() }.add(song)
            }
        }

        val topGenres = genreGroups.entries
            .filter { it.value.distinctBy { s -> s.id }.size >= MIN_GENRE_SONGS }
            .sortedByDescending { it.value.size }
            .take(3)

        for ((genreKey, songs) in topGenres) {
            val genre = genreDisplayLabels[genreKey] ?: genreKey
            val distinctSongs = songs.distinctBy { it.id }.take(MAX_SONGS_PER_SUGGESTION)
            val (iconKey, iconRes, emoji) = genreArt(genre)
            candidates.add(
                PlaylistSuggestion(
                    id = "suggestion_genre_${genre.lowercase(Locale.ROOT).replace(" ", "_")}",
                    title = "$genre Essentials",
                    description = "Top $genre tracks from your library",
                    iconKey = iconKey,
                    iconRes = iconRes,
                    coverEmoji = emoji,
                    songs = distinctSongs,
                    category = SuggestionCategory.GENRE
                )
            )
        }

        val artistGroups = mutableMapOf<String, MutableList<Song>>()
        val artistDisplayLabels = mutableMapOf<String, String>()
        for (song in allSongs) {
            val trimmed = song.artist.trim()
            val key = trimmed.lowercase(Locale.ROOT)
            if (trimmed.isBlank() || isIgnoredArtist(key)) continue
            artistDisplayLabels.getOrPut(key) { trimmed }
            artistGroups.getOrPut(key) { mutableListOf() }.add(song)
        }

        val totalPlayCounts = HashMap<String, Int>()
        monthlyStats.values.forEach { month ->
            month.songPlayCounts.forEach { (songId, count) ->
                totalPlayCounts[songId] = (totalPlayCounts[songId] ?: 0) + count
            }
        }

        val topArtists = artistGroups.entries
            .filter { it.value.distinctBy { s -> s.id }.size >= MIN_ARTIST_SONGS }
            .sortedByDescending { (_, songs) ->
                songs.sumOf { totalPlayCounts[it.id] ?: 0 } * 10 + songs.size
            }
            .take(2)

        for ((artistKey, songs) in topArtists) {
            val artist = artistDisplayLabels[artistKey] ?: artistKey
            val sortedSongs = songs.distinctBy { it.id }
                .sortedByDescending { totalPlayCounts[it.id] ?: 0 }
                .take(MAX_SONGS_PER_SUGGESTION)
            candidates.add(
                PlaylistSuggestion(
                    id = "suggestion_artist_${artist.lowercase(Locale.ROOT).replace(" ", "_")}",
                    title = "Best of $artist",
                    description = "The essentials from $artist",
                    iconKey = "mic",
                    iconRes = R.drawable.lucide_ic_mic_vocal,
                    coverEmoji = "⭐",
                    songs = sortedSongs,
                    category = SuggestionCategory.ARTIST
                )
            )
        }

        val songsByDecade = allSongs.filter { it.releaseYear != null && it.releaseYear in 1950..2030 }
            .groupBy { (it.releaseYear!! / 10) * 10 }

        val decadeOptions = listOf(
            DecadeSpec(1980, "80s Rewind", "disc", R.drawable.lucide_ic_disc, "📻"),
            DecadeSpec(1990, "90s Nostalgia", "cassette", R.drawable.lucide_ic_cassette_tape, "📼"),
            DecadeSpec(2000, "2000s Throwback", "disc", R.drawable.lucide_ic_disc, "💿"),
            DecadeSpec(2010, "2010s Hits", "sparkles", R.drawable.lucide_ic_sparkles, "✨"),
            DecadeSpec(2020, "Modern Favorites", "flame", R.drawable.lucide_ic_flame, "🔥")
        )

        for (spec in decadeOptions) {
            val decadeSongs = songsByDecade[spec.decade].orEmpty().distinctBy { it.id }.take(MAX_SONGS_PER_SUGGESTION)
            if (decadeSongs.size >= MIN_DECADE_SONGS) {
                candidates.add(
                    PlaylistSuggestion(
                        id = "suggestion_decade_${spec.decade}",
                        title = spec.title,
                        description = "${spec.decade}s hits from your collection",
                        iconKey = spec.iconKey,
                        iconRes = spec.iconRes,
                        coverEmoji = spec.emoji,
                        songs = decadeSongs,
                        category = SuggestionCategory.ERA
                    )
                )
            }
        }

        val recentIds = playbackHistory.map { it.id }.toSet()
        val forgottenFavorites = allSongs.filter {
            (it.isFavorite || it.id in favoriteIds) && it.id !in recentIds
        }.distinctBy { it.id }.take(MAX_SONGS_PER_SUGGESTION)

        if (forgottenFavorites.size >= MIN_HABIT_SONGS) {
            candidates.add(
                PlaylistSuggestion(
                    id = "suggestion_forgotten_favorites",
                    title = "Forgotten Treasures",
                    description = "Favorited tracks you haven't played recently",
                    iconKey = "compass",
                    iconRes = R.drawable.lucide_ic_compass,
                    coverEmoji = "🧭",
                    songs = forgottenFavorites,
                    category = SuggestionCategory.HABIT
                )
            )
        }

        val longSongs = allSongs.filter { it.durationMs >= LONG_DURATION_MS }
            .distinctBy { it.id }
            .sortedByDescending { it.durationMs }
            .take(MAX_SONGS_PER_SUGGESTION)

        if (longSongs.size >= MIN_HABIT_SONGS) {
            candidates.add(
                PlaylistSuggestion(
                    id = "suggestion_long_journeys",
                    title = "Long Journeys",
                    description = "Extended mixes & deep cuts over 5 minutes",
                    iconKey = "mountain",
                    iconRes = R.drawable.lucide_ic_mountain,
                    coverEmoji = "🏔️",
                    songs = longSongs,
                    category = SuggestionCategory.HABIT
                )
            )
        }

        return candidates.filter { candidate ->
            val nameTaken = candidate.title.lowercase(Locale.ROOT).trim() in existingNames
            val idsMatching = candidate.songs.map { it.id }.toSet() in existingSongSets
            !nameTaken && !idsMatching
        }.distinctBy { it.id }.take(MAX_SUGGESTIONS_TOTAL)
    }

    private fun isIgnoredGenre(genre: String): Boolean {
        val lower = genre.lowercase(Locale.ROOT)
        return lower in listOf("unknown", "other", "soundtrack", "ost", "game", "score", "audio", "various")
    }

    private fun isIgnoredArtist(artist: String): Boolean {
        val lower = artist.lowercase(Locale.ROOT)
        return lower in listOf("unknown", "unknown artist", "various", "various artists", "<unknown>")
    }

    private fun genreArt(genre: String): Triple<String, Int, String> {
        val lower = genre.lowercase(Locale.ROOT)
        return when {
            lower.contains("rock") || lower.contains("metal") || lower.contains("punk") ->
                Triple("guitar", R.drawable.lucide_ic_guitar, "🎸")
            lower.contains("electronic") || lower.contains("techno") || lower.contains("edm") || lower.contains("house") || lower.contains("synth") ->
                Triple("zap", R.drawable.lucide_ic_zap, "⚡")
            lower.contains("jazz") || lower.contains("blues") ->
                Triple("piano", R.drawable.lucide_ic_piano, "🎷")
            lower.contains("classical") || lower.contains("orchestral") || lower.contains("symphony") ->
                Triple("piano", R.drawable.lucide_ic_piano, "🎻")
            lower.contains("hip hop") || lower.contains("rap") || lower.contains("r&b") ->
                Triple("mic", R.drawable.lucide_ic_mic_vocal, "🎤")
            lower.contains("pop") || lower.contains("dance") ->
                Triple("sparkles", R.drawable.lucide_ic_sparkles, "✨")
            lower.contains("acoustic") || lower.contains("folk") || lower.contains("country") ->
                Triple("coffee", R.drawable.lucide_ic_coffee, "☕")
            lower.contains("ambient") || lower.contains("chill") || lower.contains("lo-fi") || lower.contains("lofi") ->
                Triple("moon", R.drawable.lucide_ic_moon, "🌙")
            lower.contains("anime") || lower.contains("j-pop") || lower.contains("j-rock") ->
                Triple("star", R.drawable.lucide_ic_star, "⭐")
            else ->
                Triple("disc", R.drawable.lucide_ic_disc, "🎵")
        }
    }

    private data class DecadeSpec(
        val decade: Int,
        val title: String,
        val iconKey: String,
        val iconRes: Int,
        val emoji: String
    )
}
