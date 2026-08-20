@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.data.playback

import androidx.media3.common.Tracks
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import kotlin.math.pow

data class ReplayGain(val trackGainDb: Float?, val trackPeak: Float?, val albumGainDb: Float?)

private val GAIN_SUFFIX_REGEX = Regex("(?i)\\s*db\\s*$")

fun extractReplayGain(tracks: Tracks): ReplayGain? {
    var trackGainDb: Float? = null
    var trackPeak: Float? = null
    var albumGainDb: Float? = null

    for (group in tracks.groups) {
        for (i in 0 until group.length) {
            val metadata = group.getTrackFormat(i).metadata ?: continue
            for (j in 0 until metadata.length()) {
                val entry = metadata.get(j)
                val key: String?
                val value: String?
                when (entry) {
                    is TextInformationFrame -> { key = entry.description; value = entry.values.firstOrNull() }
                    is VorbisComment -> { key = entry.key; value = entry.value }
                    is InternalFrame -> { key = entry.description; value = entry.text }
                    else -> { key = null; value = null }
                }
                if (key == null || value == null) continue
                when (key.uppercase()) {
                    "REPLAYGAIN_TRACK_GAIN" -> trackGainDb = parseGainDb(value)
                    "REPLAYGAIN_TRACK_PEAK" -> trackPeak = value.trim().toFloatOrNull()
                    "REPLAYGAIN_ALBUM_GAIN" -> albumGainDb = parseGainDb(value)
                }
            }
        }
    }

    return if (trackGainDb != null || albumGainDb != null) ReplayGain(trackGainDb, trackPeak, albumGainDb) else null
}

private fun parseGainDb(raw: String): Float? = raw.replace(GAIN_SUFFIX_REGEX, "").trim().toFloatOrNull()

fun ReplayGain?.volumeMultiplier(replayGainEnabled: Boolean = true, preAmpDb: Int = 0): Float {
    if (!replayGainEnabled) {
        if (preAmpDb == 0) return 1f
        return 10.0.pow(preAmpDb / 20.0).toFloat().coerceIn(0.1f, 3f)
    }
    val rawGainDb = this?.trackGainDb ?: this?.albumGainDb
    val gainDb = if (rawGainDb != null) (rawGainDb + preAmpDb) else preAmpDb.toFloat()
    val linear = 10.0.pow(gainDb / 20.0).toFloat()
    val peak = this?.trackPeak?.takeIf { it > 0f } ?: return linear.coerceIn(0.1f, 3f)
    return (if (linear * peak > 1f) 1f / peak else linear).coerceIn(0.1f, 3f)
}
