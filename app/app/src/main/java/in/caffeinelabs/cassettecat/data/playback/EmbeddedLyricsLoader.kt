package `in`.caffeinelabs.cassettecat.data.playback

import android.content.Context
import `in`.caffeinelabs.cassettecat.data.library.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/** Reads ID3 USLT tags from local MP3 files when the playback metadata omits them. */
class EmbeddedLyricsLoader(private val context: Context) {
    suspend fun loadFor(song: Song): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(song.contentUri)?.use { input ->
                val header = input.readNBytesCompat(10)
                if (header.size != 10 || header.copyOfRange(0, 3).decodeToString() != "ID3") return@use null

                val version = header[3].toInt() and 0xFF
                if (version !in 3..4) return@use null
                val tagSize = synchSafeInt(header, 6).coerceAtMost(MAX_TAG_BYTES)
                val tag = input.readNBytesCompat(tagSize)
                extractUslt(tag, version)
            }
        }.getOrNull()
    }

    private fun extractUslt(tag: ByteArray, version: Int): String? {
        var offset = 0
        while (offset + 10 <= tag.size) {
            val id = tag.copyOfRange(offset, offset + 4).decodeToString()
            if (id.all { it == '\u0000' }) return null
            val size = if (version == 4) synchSafeInt(tag, offset + 4) else bigEndianInt(tag, offset + 4)
            val payloadStart = offset + 10
            val payloadEnd = payloadStart + size
            if (size < 0 || payloadEnd > tag.size) return null
            if (id == "USLT") return parseUsltFrame(tag.copyOfRange(payloadStart, payloadEnd))
            offset = payloadEnd
        }
        return null
    }

    private fun synchSafeInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

    private fun bigEndianInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private companion object { const val MAX_TAG_BYTES = 4 * 1024 * 1024 }
}

// InputStream.readNBytes(Int) requires API 33; app's minSdk is 26.
private fun InputStream.readNBytesCompat(n: Int): ByteArray {
    val buffer = ByteArray(n)
    var totalRead = 0
    while (totalRead < n) {
        val read = read(buffer, totalRead, n - totalRead)
        if (read == -1) break
        totalRead += read
    }
    return if (totalRead == n) buffer else buffer.copyOf(totalRead)
}
