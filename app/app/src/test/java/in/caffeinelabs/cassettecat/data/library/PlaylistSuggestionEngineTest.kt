package `in`.caffeinelabs.cassettecat.data.library

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class PlaylistSuggestionEngineTest {

    private val dummyUri: Uri = mock(Uri::class.java)

    private fun createDummySong(
        id: String,
        title: String = "Title $id",
        artist: String = "Artist $id",
        album: String = "Album $id",
        genres: List<String> = emptyList(),
        releaseYear: Int? = null,
        durationMs: Long = 180_000L,
        isFavorite: Boolean = false
    ): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = "album_$id",
        durationMs = durationMs,
        contentUri = dummyUri,
        source = MusicSource.Local,
        genres = genres,
        releaseYear = releaseYear,
        isFavorite = isFavorite
    )

    @Test
    fun generatesSoundtrackSuggestionForGameAndOstTracks() {
        val songs = listOf(
            createDummySong("1", title = "Zelda Theme", album = "Ocarina of Time", genres = listOf("Game")),
            createDummySong("2", title = "Halo Theme", album = "Halo OST", genres = listOf("Soundtrack")),
            createDummySong("3", title = "Megalovania", album = "Undertale", genres = listOf("VGM")),
            createDummySong("4", title = "Main Theme", album = "Skyrim Score", genres = listOf("Original Soundtrack")),
            createDummySong("5", title = "Regular Pop Song", genres = listOf("Pop"))
        )

        val suggestions = PlaylistSuggestionEngine.generateSuggestions(allSongs = songs)
        val ostSuggestion = suggestions.find { it.category == SuggestionCategory.SOUNDTRACK }

        assertTrue(ostSuggestion != null)
        assertEquals("Game & Soundtrack Vault", ostSuggestion?.title)
        assertEquals(4, ostSuggestion?.songs?.size)
    }

    @Test
    fun clustersProminentGenres() {
        val rockSongs = (1..6).map {
            createDummySong("rock_$it", title = "Rock Track $it", genres = listOf("Rock"))
        }
        val popSongs = (1..2).map {
            createDummySong("pop_$it", title = "Pop Track $it", genres = listOf("Pop"))
        }

        val suggestions = PlaylistSuggestionEngine.generateSuggestions(allSongs = rockSongs + popSongs)
        val rockSuggestion = suggestions.find { it.category == SuggestionCategory.GENRE && it.title == "Rock Essentials" }

        assertTrue(rockSuggestion != null)
        assertEquals(6, rockSuggestion?.songs?.size)
        assertFalse(suggestions.any { it.title == "Pop Essentials" })
    }

    @Test
    fun spotlightsTopArtist() {
        val artistSongs = (1..6).map {
            createDummySong("daft_$it", title = "Track $it", artist = "Daft Punk")
        }

        val suggestions = PlaylistSuggestionEngine.generateSuggestions(allSongs = artistSongs)
        val artistSuggestion = suggestions.find { it.category == SuggestionCategory.ARTIST }

        assertTrue(artistSuggestion != null)
        assertEquals("Best of Daft Punk", artistSuggestion?.title)
        assertEquals(6, artistSuggestion?.songs?.size)
    }

    @Test
    fun groupsDecadeTimeCapsules() {
        val ninetiesSongs = (1..5).map {
            createDummySong("90s_$it", title = "90s Track $it", releaseYear = 1994)
        }

        val suggestions = PlaylistSuggestionEngine.generateSuggestions(allSongs = ninetiesSongs)
        val eraSuggestion = suggestions.find { it.category == SuggestionCategory.ERA }

        assertTrue(eraSuggestion != null)
        assertEquals("90s Nostalgia", eraSuggestion?.title)
        assertEquals(5, eraSuggestion?.songs?.size)
    }

    @Test
    fun excludesExistingPlaylistNames() {
        val rockSongs = (1..6).map {
            createDummySong("rock_$it", title = "Rock Track $it", genres = listOf("Rock"))
        }
        val existingPlaylists = listOf(
            Playlist(id = "p1", name = "Rock Essentials")
        )

        val suggestions = PlaylistSuggestionEngine.generateSuggestions(
            allSongs = rockSongs,
            existingPlaylists = existingPlaylists
        )

        assertFalse(suggestions.any { it.title.equals("Rock Essentials", ignoreCase = true) })
    }
}
