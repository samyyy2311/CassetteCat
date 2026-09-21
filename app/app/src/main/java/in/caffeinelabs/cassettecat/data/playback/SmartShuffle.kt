package `in`.caffeinelabs.cassettecat.data.playback

import `in`.caffeinelabs.cassettecat.data.library.Song

object SmartShuffle {

    fun shuffleUpcoming(
        upcoming: List<Song>,
        recentHistory: List<Song> = emptyList(),
        skippedIds: Set<String> = emptySet(),
        previousSong: Song? = null
    ): List<Song> {
        if (upcoming.size <= 2) return upcoming.shuffled()

        val recentHistoryIds = recentHistory.take(15).map { it.id }.toSet()
        val (skipped, nonSkipped) = upcoming.partition { it.id in skippedIds }
        val (history, fresh) = nonSkipped.partition { it.id in recentHistoryIds }

        val candidatePool = ArrayList<Song>(upcoming.size).apply {
            addAll(fresh.shuffled())
            addAll(history.shuffled())
            addAll(skipped.shuffled())
        }

        return spreadArtists(candidatePool, previousSong)
    }

    fun shuffleAll(
        songs: List<Song>,
        startIndex: Int? = null,
        recentHistory: List<Song> = emptyList(),
        skippedIds: Set<String> = emptySet()
    ): Pair<List<Song>, Int> {
        if (songs.isEmpty()) return emptyList<Song>() to 0
        if (startIndex != null && startIndex in songs.indices) {
            val chosen = songs[startIndex]
            val remaining = songs.filterIndexed { index, _ -> index != startIndex }
            val shuffledUpcoming = shuffleUpcoming(remaining, recentHistory, skippedIds, previousSong = chosen)
            return (listOf(chosen) + shuffledUpcoming) to 0
        }
        val shuffled = shuffleUpcoming(songs, recentHistory, skippedIds, previousSong = null)
        return shuffled to 0
    }

    fun reShuffleCycle(
        originalQueue: List<Song>,
        justPlayed: Song?,
        skippedIds: Set<String> = emptySet()
    ): List<Song> {
        if (originalQueue.isEmpty()) return emptyList()
        val shuffled = shuffleUpcoming(
            upcoming = originalQueue,
            recentHistory = listOfNotNull(justPlayed),
            skippedIds = skippedIds,
            previousSong = justPlayed
        )
        if (shuffled.size > 1 && shuffled.first().id == justPlayed?.id) {
            val mutable = shuffled.toMutableList()
            mutable.add(mutable.removeAt(0))
            return mutable
        }
        return shuffled
    }

    private fun spreadArtists(pool: MutableList<Song>, previousSong: Song?): List<Song> {
        if (pool.size <= 2) return pool

        if (previousSong != null && previousSong.artist.isNotBlank()) {
            if (pool[0].artist.equals(previousSong.artist, ignoreCase = true)) {
                val nonMatchingIndex = pool.indexOfFirst {
                    it.artist.isNotBlank() && !it.artist.equals(previousSong.artist, ignoreCase = true)
                }
                if (nonMatchingIndex > 0) {
                    val song = pool.removeAt(nonMatchingIndex)
                    pool.add(0, song)
                }
            }
        }

        var i = 1
        while (i < pool.size) {
            val currentArtist = pool[i].artist
            val previousArtist = pool[i - 1].artist
            if (currentArtist.isNotBlank() && currentArtist.equals(previousArtist, ignoreCase = true)) {
                var moved = false
                for (j in (i + 1) until pool.size) {
                    val candidate = pool[j]
                    val nextAfterCandidate = pool.getOrNull(j + 1)
                    if (candidate.artist.isNotBlank() && !candidate.artist.equals(currentArtist, ignoreCase = true)) {
                        if (nextAfterCandidate == null || !nextAfterCandidate.artist.equals(currentArtist, ignoreCase = true)) {
                            val song = pool.removeAt(i)
                            pool.add(j, song)
                            moved = true
                            break
                        }
                    }
                }
                if (!moved) {
                    for (k in 0 until i) {
                        val prevNeighbor = pool.getOrNull(k - 1)
                        val nextNeighbor = pool.getOrNull(k)
                        val prevMatches = prevNeighbor != null && prevNeighbor.artist.isNotBlank() && prevNeighbor.artist.equals(currentArtist, ignoreCase = true)
                        val nextMatches = nextNeighbor != null && nextNeighbor.artist.isNotBlank() && nextNeighbor.artist.equals(currentArtist, ignoreCase = true)
                        if (!prevMatches && !nextMatches) {
                            val song = pool.removeAt(i)
                            pool.add(k, song)
                            moved = true
                            break
                        }
                    }
                }
                if (!moved) {
                    i++
                }
            } else {
                i++
            }
        }

        return pool
    }
}
