package `in`.caffeinelabs.cassettecat.data.streaming

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * How good a song streamed from your server sounds, chosen separately for Wi-Fi and for mobile data, so data isn't
 * spent on files larger than you want there. 0 is the original file; anything else asks the server to convert it to
 * MP3 at that many kbps, which Jellyfin and most Subsonic servers can do. Downloads always keep the original.
 */
object StreamQuality {
    val choicesKbps = listOf(0, 320, 256, 192, 128)

    @Volatile
    var wifiKbps = 0

    @Volatile
    var mobileKbps = 0

    /** The limit for the connection in use: a metered one, such as mobile data or a hotspot, counts as mobile. */
    fun currentKbps(context: Context): Int =
        if (context.getSystemService(ConnectivityManager::class.java).isActiveNetworkMetered) mobileKbps else wifiKbps

    /** [url] asking its server for at most [kbps], when it's a server stream; anything else is left as it is. */
    fun limit(url: String, kbps: Int): String {
        if (kbps <= 0) return url
        val parsed = url.toHttpUrlOrNull() ?: return url
        val path = parsed.encodedPath
        val limits = when {
            path.endsWith("/rest/stream.view") -> listOf("maxBitRate" to "$kbps", "format" to "mp3")
            // The universal stream converts to MP3 at audioBitRate whenever the file is over the limit.
            path.contains("/Audio/") && path.endsWith("/universal") ->
                listOf("maxStreamingBitrate" to "${kbps * 1000}", "audioBitRate" to "${kbps * 1000}")
            else -> return url
        }
        val builder = parsed.newBuilder()
        limits.forEach { (name, value) -> builder.setQueryParameter(name, value) }
        return builder.build().toString()
    }
}
