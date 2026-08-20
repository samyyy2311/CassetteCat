package `in`.caffeinelabs.cassettecat.data.radio

import androidx.core.net.toUri
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RadioStation(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val favicon: String? = null,
    val tags: String = "",
    val country: String = "",
    val bitrate: Int = 0
)

fun RadioStation.toSong(): Song = Song(
    id = "radio:$uuid",
    title = name,
    artist = tags.split(",").firstOrNull { it.isNotBlank() }?.trim() ?: country.ifBlank { "Radio" },
    album = "",
    albumId = "",
    durationMs = 0L,
    contentUri = streamUrl.toUri(),
    source = MusicSource.Radio,
    artUri = favicon?.takeIf { it.isNotBlank() }?.let { it.toUri() }
)

fun customRadioStation(name: String, streamUrl: String): RadioStation = RadioStation(
    uuid = "custom:${UUID.randomUUID()}",
    name = name,
    streamUrl = streamUrl
)

enum class RadioSortOrder(val label: String, val apiValue: String) {
    POPULARITY("Popularity", "votes"),
    TRENDING("Trending", "clicktrend"),
    NAME("Name", "name"),
    COUNTRY("Country", "country"),
    BITRATE("Bitrate", "bitrate")
}
