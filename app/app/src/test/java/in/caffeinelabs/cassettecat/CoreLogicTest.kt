package `in`.caffeinelabs.cassettecat

import `in`.caffeinelabs.cassettecat.data.listeningroom.readLineBounded
import `in`.caffeinelabs.cassettecat.data.listeningroom.isInvalidLocalRange
import `in`.caffeinelabs.cassettecat.data.listeningroom.skipFully
import `in`.caffeinelabs.cassettecat.data.playback.adjustLyricsSync
import `in`.caffeinelabs.cassettecat.data.playback.parseLrc
import `in`.caffeinelabs.cassettecat.data.scrobble.credentialToMigrate
import `in`.caffeinelabs.cassettecat.data.update.isNewer
import `in`.caffeinelabs.cassettecat.data.settings.DefaultLibraryTab
import `in`.caffeinelabs.cassettecat.data.settings.ThemeAccent
import `in`.caffeinelabs.cassettecat.data.settings.orderedEnumValues
import `in`.caffeinelabs.cassettecat.ui.theme.artworkAccentFromPixels
import `in`.caffeinelabs.cassettecat.ui.theme.normalizeArtworkAccent
import `in`.caffeinelabs.cassettecat.ui.playback.instantMixAffinity
import `in`.caffeinelabs.cassettecat.ui.screens.library.isExtendedCut
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.isSeekablePlayback
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringReader
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
}
