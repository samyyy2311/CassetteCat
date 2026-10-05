package `in`.caffeinelabs.cassettecat

import `in`.caffeinelabs.cassettecat.data.streaming.shouldClearArtworkThumbnails

import `in`.caffeinelabs.cassettecat.data.listeningroom.readLineBounded
import `in`.caffeinelabs.cassettecat.data.listeningroom.isInvalidLocalRange
import `in`.caffeinelabs.cassettecat.data.listeningroom.skipFully
import `in`.caffeinelabs.cassettecat.data.device.DiscoveredDesktop
import `in`.caffeinelabs.cassettecat.data.device.parseDiscoveryReply
import `in`.caffeinelabs.cassettecat.data.playback.adjustLyricsSync
import `in`.caffeinelabs.cassettecat.data.radio.radioBrowserMirrors
import `in`.caffeinelabs.cassettecat.data.playback.parseLrc
import `in`.caffeinelabs.cassettecat.data.scrobble.credentialToMigrate
import `in`.caffeinelabs.cassettecat.data.update.isNewer
import `in`.caffeinelabs.cassettecat.data.settings.DefaultLibraryTab
import `in`.caffeinelabs.cassettecat.data.settings.ThemeAccent
import `in`.caffeinelabs.cassettecat.data.settings.orderedEnumValues
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.playback.SessionSkipTracker
import `in`.caffeinelabs.cassettecat.data.playback.SmartShuffle
import `in`.caffeinelabs.cassettecat.ui.theme.artworkAccentFromPixels
import `in`.caffeinelabs.cassettecat.ui.theme.normalizeArtworkAccent
import `in`.caffeinelabs.cassettecat.ui.playback.instantMixAffinity
import `in`.caffeinelabs.cassettecat.ui.screens.library.TagEdits
import `in`.caffeinelabs.cassettecat.ui.screens.library.applyTagEdits
import `in`.caffeinelabs.cassettecat.ui.screens.library.isExtendedCut
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.isSeekablePlayback
import `in`.caffeinelabs.cassettecat.data.device.DesktopAddress
import `in`.caffeinelabs.cassettecat.data.device.HandoffTrack
import `in`.caffeinelabs.cassettecat.data.device.findAllInLibrary
import `in`.caffeinelabs.cassettecat.data.device.matchInLibrary
import `in`.caffeinelabs.cassettecat.data.device.planLikesSync
import `in`.caffeinelabs.cassettecat.data.device.desktopServiceStates
import `in`.caffeinelabs.cassettecat.data.device.desktopSettingsFrom
import `in`.caffeinelabs.cassettecat.data.device.withDesktopSettings
import `in`.caffeinelabs.cassettecat.data.backup.BackupAppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettings
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import `in`.caffeinelabs.cassettecat.data.device.parseDesktopAddress
import `in`.caffeinelabs.cassettecat.ui.screens.settings.findDuplicateGroups
import android.net.Uri
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringReader
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {
    @Test
    fun parsesAndAdjustsLyrics() {
        val lines = parseLrc("[00:01.2]First\n[01:02.345]Second")

        assertEquals(listOf(1_200L, 62_345L), lines.map { it.timestampMs })
        assertEquals(0L, adjustLyricsSync(lines, -2_000L).first().timestampMs)
    }

    @Test
    fun keepsOnlyNamedRadioBrowserMirrors() {
        val mirrors = radioBrowserMirrors(
            listOf("de1.api.radio-browser.info", "91.132.145.114", "all.api.radio-browser.info", "de1.api.radio-browser.info")
        )
        assertEquals(listOf("de1.api.radio-browser.info"), mirrors)
    }

    @Test
    fun batchTagEditChangesOnlyEditedFields() {
        val song = testSong("1", artist = "Old", title = "Keep", genres = listOf("Rock"), releaseYear = 1999)
        val (override, updated) = applyTagEdits(song, existing = null, edits = TagEdits(artist = " New "))

        assertEquals("Keep", updated.title)
        assertEquals("New", updated.artist)
        assertEquals(listOf("Rock"), updated.genres)
        assertEquals(1999, updated.releaseYear)
        assertEquals("Old", override.originalArtist)
        assertNull(override.title)
        assertNull(override.genres)
        assertFalse(override.releaseYearSet)

        val (second, _) = applyTagEdits(updated, existing = override, edits = TagEdits(artist = "Newer"))
        assertEquals("Old", second.originalArtist)

        val noYear = testSong("2", releaseYear = null)
        val (dated, datedSong) = applyTagEdits(noYear, existing = null, edits = TagEdits(yearEdited = true, year = 2020))
        val (redated, _) = applyTagEdits(datedSong, existing = dated, edits = TagEdits(yearEdited = true, year = 2021))
        assertNull(redated.originalReleaseYear)
    }

    @Test
    fun parsesDesktopDiscoveryReplies() {
        assertEquals(
            DiscoveredDesktop("STUDIO-PC", "192.168.1.20", 47800),
            parseDiscoveryReply("""{"name":"STUDIO-PC","port":47800}""", "192.168.1.20")
        )
        assertNull(parseDiscoveryReply("CASSETTECAT_DISCOVER", "192.168.1.20"))
        assertNull(parseDiscoveryReply("""{"name":"","port":47800}""", "192.168.1.20"))
        assertNull(parseDiscoveryReply("""{"name":"PC","port":0}""", "192.168.1.20"))
    }

    @Test
    fun handsOffOnlyWhenTheCurrentSongIsInTheLibrary() {
        val library = listOf(testSong("a", artist = "Ann", title = "One"), testSong("b", artist = "Bo", title = "Two"))
        val fromDesktop = listOf(
            testSong("desktop:1", artist = " ann ", title = "ONE"),
            testSong("desktop:2", artist = "Cy", title = "Missing"),
            testSong("desktop:3", artist = "Bo", title = "Two")
        )

        assertEquals(listOf("a", "b"), matchInLibrary(fromDesktop, library).map { it.id })
        assertEquals(emptyList<Song>(), matchInLibrary(fromDesktop.drop(1), library))
    }

    @Test
    fun likesSyncAddsOnFirstSyncAndThenCarriesLikesAndUnlikesBothWays() {
        val shared = setOf("a", "b", "c")

        val first = planLikesSync(setOf("a", "x"), setOf("b"), shared, lastAgreed = null)
        assertEquals(setOf("b"), first.likeOnPhone)
        assertEquals(setOf("a"), first.likeOnDesktop)
        assertEquals(setOf("a", "b"), first.agreed)

        val unlikedOnPhone = planLikesSync(setOf("b"), setOf("a", "b"), shared, lastAgreed = setOf("a", "b"))
        assertEquals(setOf("a"), unlikedOnPhone.unlikeOnDesktop)
        assertEquals(emptySet<String>(), unlikedOnPhone.unlikeOnPhone + unlikedOnPhone.likeOnPhone + unlikedOnPhone.likeOnDesktop)

        val likedOnDesktop = planLikesSync(setOf("b"), setOf("b", "c"), shared, lastAgreed = setOf("b"))
        assertEquals(setOf("c"), likedOnDesktop.likeOnPhone)
        assertEquals(setOf("b", "c"), likedOnDesktop.agreed)

        val songMissingForNow = planLikesSync(setOf("b"), setOf("b"), shared = setOf("b"), lastAgreed = setOf("b", "gone"))
        assertEquals(setOf("b", "gone"), songMissingForNow.agreed)
    }

    @Test
    fun settingsTranslateToTheComputerAndBack() {
        val phone = BackupAppPreferences(
            themeAccent = "ELECTRIC_CYAN", customAccentColor = 0xFF123456, crossfadeSeconds = 4,
            replayGainEnabled = true, lyricsFontSize = "LARGE", lyricsAlignment = "LEFT", trackRowDensity = "COMPACT"
        )
        val keepsAlbumGain = JsonObject(mapOf("player/replayGainMode" to JsonPrimitive("album")))
        val sent = desktopSettingsFrom(phone, ServiceSettings(lrcLibEnabled = false), keepsAlbumGain)
        assertEquals(JsonPrimitive("cyan"), sent["ui/accentName"])
        assertEquals(JsonPrimitive("#123456"), sent["ui/customAccentColor"])
        assertEquals(JsonPrimitive(3), sent["player/crossfadeSeconds"])
        assertEquals(JsonPrimitive("album"), sent["player/replayGainMode"])
        assertEquals(JsonPrimitive(32), sent["lyrics/fontSize"])
        assertEquals(JsonPrimitive(false), sent["services/lrclib"])

        val back = BackupAppPreferences().withDesktopSettings(sent)
        assertEquals("ELECTRIC_CYAN", back.themeAccent)
        assertEquals(0xFF123456, back.customAccentColor)
        assertEquals(2, back.crossfadeSeconds)
        assertEquals("LARGE", back.lyricsFontSize)
        assertEquals("LEFT", back.lyricsAlignment)
        assertEquals("COMPACT", back.trackRowDensity)
        assertEquals(false, desktopServiceStates(sent)[ExternalService.LRCLIB])

        val radiusSnapped = BackupAppPreferences().withDesktopSettings(JsonObject(mapOf("ui/albumArtRadius" to JsonPrimitive(11))))
        assertEquals(8, radiusSnapped.albumArtCornerRadiusDp)
    }

    @Test
    fun findsASongSentFromTheDesktopByTitleAndArtist() {
        val library = listOf(testSong("a", artist = "Ann", title = "One"), testSong("b", artist = "Bo", title = "Two"))

        val sent = listOf(HandoffTrack(" two", "BO "), HandoffTrack("Two", "Ann"), HandoffTrack("One", "ann"), HandoffTrack("TWO", "bo"))
        assertEquals(listOf("b", "a"), findAllInLibrary(sent, library).map { it.id })
    }

    @Test
    fun parsesDesktopRemoteAddress() {
        assertEquals(DesktopAddress("192.168.1.20", 47800, "ABC234"), parseDesktopAddress(" 192.168.1.20:47800#abc234 "))
        assertNull(parseDesktopAddress("192.168.1.20:47800"))
        assertNull(parseDesktopAddress("192.168.1.20#ABC234"))
        assertNull(parseDesktopAddress("192.168.1.20:70000#ABC234"))
        assertNull(parseDesktopAddress("192.168.1.20:47800#"))
    }

    @Test
    fun comparesReleaseVersions() {
        assertTrue(isNewer("1.4.0", "1.3.9"))
        assertFalse(isNewer("1.3.1", "1.3.1"))
        assertFalse(isNewer("1.3.0", "1.3.1"))
    }

    @Test
    fun boundsProtocolLines() {
        assertEquals("room-code", BufferedReader(StringReader("room-code\r\n")).readLineBounded(16))
        assertThrows(IOException::class.java) {
            BufferedReader(StringReader("too-long")).readLineBounded(3)
        }
    }

    @Test
    fun validatesRelayOffsetsAndCredentialMigration() {
        assertFalse(isInvalidLocalRange(0, null))
        assertTrue(isInvalidLocalRange(1, null))
        assertFalse(isInvalidLocalRange(2, 3))
        assertTrue(isInvalidLocalRange(3, 3))
        assertTrue(ByteArrayInputStream(byteArrayOf(1, 2, 3)).skipFully(3))
        assertFalse(ByteArrayInputStream(byteArrayOf(1, 2, 3)).skipFully(4))
        assertEquals("legacy", credentialToMigrate("legacy", null))
        assertNull(credentialToMigrate("legacy", "encrypted"))
    }

    @Test
    fun restoresPersistedCustomizationOrderAndArtworkAccent() {
        assertEquals(
            listOf(
                DefaultLibraryTab.ALBUMS, DefaultLibraryTab.SONGS, DefaultLibraryTab.ARTISTS,
                DefaultLibraryTab.GENRES, DefaultLibraryTab.PLAYLISTS, DefaultLibraryTab.FOLDERS
            ),
            orderedEnumValues("ALBUMS,SONGS,ALBUMS,REMOVED", DefaultLibraryTab.entries)
        )

        val accent = artworkAccentFromPixels(IntArray(16) { 0xFFFF3020.toInt() })
        assertTrue(accent != null && (accent shr 16 and 0xFF) > (accent and 0xFF))
        assertNull(artworkAccentFromPixels(IntArray(16) { 0xFF777777.toInt() }))
        assertEquals(ThemeAccent.RECORD_RED.colorValue, normalizeArtworkAccent(0.5, 0.5, 0.5))
        assertFalse(isSeekablePlayback(0L))
        assertTrue(isSeekablePlayback(1L))
    }

    @Test
    fun ranksInstantMixCandidatesByLocalMetadata() {
        assertEquals(3, instantMixAffinity("Seed", listOf("Rock"), "Other", listOf("rock")))
        assertEquals(2, instantMixAffinity("Seed", listOf("Rock"), "seed", listOf("Jazz")))
        assertEquals(5, instantMixAffinity("Seed", listOf("Rock"), "Seed", listOf("Rock")))
        assertEquals(0, instantMixAffinity("Seed", listOf("Rock"), "Other", listOf("Jazz")))
    }

    @Test
    fun classifiesExtendedCutsAtFiveMinuteBoundary() {
        assertFalse(isExtendedCut(4 * 60_000L + 59_000L))
        assertFalse(isExtendedCut(5 * 60_000L))
        assertTrue(isExtendedCut(5 * 60_000L + 1_000L))
    }

    @Test
    fun retainsArtworkThumbnailsWhenUiIsHiddenButNotUnderMemoryPressure() {
        for (level in listOf(0, 5, 9, 20, 39)) {
            assertFalse("trim level $level", shouldClearArtworkThumbnails(level))
        }
        for (level in listOf(10, 15, 19, 40, 60, 80)) {
            assertTrue("trim level $level", shouldClearArtworkThumbnails(level))
        }
    }

    @Test
    fun tracksSessionSkipsWithCooldownAndCompletion() {
        var simulatedTime = 10_000L
        val tracker = SessionSkipTracker(clock = { simulatedTime }, cooldownMs = 30_000L)

        tracker.recordSkip("song-skip", elapsedMs = 5_000L, durationMs = 180_000L)
        assertTrue(tracker.isSkipped("song-skip"))
        assertEquals(setOf("song-skip"), tracker.skippedSongIds())

        tracker.recordSkip("song-normal", elapsedMs = 60_000L, durationMs = 180_000L)
        assertFalse(tracker.isSkipped("song-normal"))

        tracker.recordCompletion("song-skip")
        assertFalse(tracker.isSkipped("song-skip"))

        tracker.recordSkip("song-skip", elapsedMs = 5_000L, durationMs = 180_000L)
        assertTrue(tracker.isSkipped("song-skip"))
        simulatedTime += 35_000L
        assertFalse(tracker.isSkipped("song-skip"))
    }

    @Test
    fun smartShuffleDefersSkippedAndAvoidsArtistClusters() {
        val s1 = testSong("1", artist = "Artist A")
        val s2 = testSong("2", artist = "Artist A")
        val s3 = testSong("3", artist = "Artist B")
        val s4 = testSong("4", artist = "Artist C")

        val shuffledUpcoming = SmartShuffle.shuffleUpcoming(
            upcoming = listOf(s1, s2, s3, s4),
            skippedIds = setOf("1")
        )
        assertEquals("1", shuffledUpcoming.last().id)

        val (shuffledAll, _) = SmartShuffle.shuffleAll(
            songs = listOf(s1, s2, s3, s4),
            recentHistory = listOf(s1)
        )
        assertFalse(shuffledAll.first().id == "1")

        val multiSongs = listOf(
            testSong("1", artist = "Beatles"),
            testSong("2", artist = "Beatles"),
            testSong("3", artist = "Beatles"),
            testSong("4", artist = "Pink Floyd"),
            testSong("5", artist = "Queen"),
            testSong("6", artist = "Radiohead")
        )
        val (antiClustered, _) = SmartShuffle.shuffleAll(multiSongs)
        for (i in 0 until antiClustered.size - 1) {
            if (antiClustered[i].artist == "Beatles") {
                assertFalse(antiClustered[i + 1].artist == "Beatles")
            }
        }
    }

    @Test
    fun ranksMixCandidatesWithCollaborationsAndEras() {
        val collabsScore = instantMixAffinity(
            seedArtist = "Daft Punk feat. Pharrell Williams",
            seedGenres = emptyList(),
            candidateArtist = "Pharrell Williams",
            candidateGenres = emptyList()
        )
        assertEquals(1, collabsScore)

        val closeEra = instantMixAffinity(
            seedArtist = "Artist A",
            seedGenres = emptyList(),
            candidateArtist = "Artist B",
            candidateGenres = emptyList(),
            seedYear = 2020,
            candidateYear = 2023
        )
        assertEquals(1, closeEra)

        val farEra = instantMixAffinity(
            seedArtist = "Artist A",
            seedGenres = emptyList(),
            candidateArtist = "Artist B",
            candidateGenres = emptyList(),
            seedYear = 2020,
            candidateYear = 1985
        )
        assertEquals(0, farEra)
    }

    @Test
    fun duplicatesMatchTitleAndArtistIgnoringCaseWhenDurationsAreClose() {
        val groups = findDuplicateGroups(listOf(
            testSong("a", artist = "Ann", title = "One", durationMs = 200_000L),
            testSong("b", artist = "ann ", title = "ONE", durationMs = 201_500L),
            testSong("c", artist = "Ann", title = "One (Live)", durationMs = 200_000L),
            testSong("d", artist = "Bo", title = "Two", durationMs = 180_000L),
            testSong("e", artist = "Bo", title = "Two", durationMs = 240_000L),
            testSong("f", artist = "Ann", title = "One", durationMs = 300_000L),
            testSong("g", artist = "Cy", title = "One", durationMs = 100_500L),
            testSong("h", artist = "Cy", title = "One", durationMs = 100_000L),
            testSong("i", artist = "Dee", title = "Three", durationMs = 200_000L),
            testSong("j", artist = "Dee", title = "Three", durationMs = 202_900L),
            testSong("k", artist = "Dee", title = "Three", durationMs = 205_800L)
        ))
        assertEquals(listOf(listOf("a", "b"), listOf("h", "g"), listOf("i", "j")), groups.map { group -> group.map { it.id } })
    }

    private fun testSong(
        id: String,
        artist: String = "Artist",
        title: String = "Title",
        genres: List<String> = emptyList(),
        releaseYear: Int? = null,
        durationMs: Long = 180_000L
    ): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        albumId = "album-$id",
        durationMs = durationMs,
        contentUri = org.mockito.Mockito.mock(Uri::class.java),
        source = MusicSource.Local,
        genres = genres,
        releaseYear = releaseYear
    )
}
