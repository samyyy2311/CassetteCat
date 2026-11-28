package `in`.caffeinelabs.cassettecat.data.streaming

import android.graphics.Bitmap
import android.net.Uri
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val THUMBNAIL_CACHE_BYTES = 16 * 1024 * 1024
private const val FULL_CACHE_BYTES = 24 * 1024 * 1024

// In-memory two-tier cache (thumbnail and full-res) for remote artwork.
class RemoteAlbumArtLoader {
    private val cache = ArtworkCache(THUMBNAIL_CACHE_BYTES, FULL_CACHE_BYTES)

    fun clearCache() = cache.clear()

    fun trimCaches(level: Int) = cache.trim(level)

    fun peek(artUri: Uri, thumbnail: Boolean = true): Bitmap? = cache.get(artUri.toString(), thumbnail)

    suspend fun load(artUri: Uri, thumbnail: Boolean = true): Bitmap? {
        val url = artUri.toString()
        return cache.getOrLoad(url, thumbnail) { fetch(url, it) }
    }

    private fun fetch(url: String, maxDimension: Int): Bitmap? = runCatching {
        val saved = diskDir?.let { File(it, savedName(url)) }
        if (saved != null && saved.exists()) return@runCatching decodeSampledBitmap(saved.readBytes(), maxDimension = maxDimension)
        sharedHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            val bytes = response.body.bytes()
            saved?.let { file -> runCatching { file.parentFile?.mkdirs(); file.writeBytes(bytes) } }
            decodeSampledBitmap(bytes, maxDimension = maxDimension)
        }
    }.getOrNull()

    companion object {
        /**
         * Where covers from your server are kept on the phone, so they aren't downloaded again each time the app
         * starts. Set once when the app starts.
         */
        @Volatile
        var diskDir: File? = null

        // A cover's file is named after its address without the sign-in parts, which change between sign-ins, or
        // the server's host, which changes with its away address.
        internal fun savedName(url: String): String {
            val parsed = url.toHttpUrlOrNull() ?: return sha256(url)
            val kept = parsed.queryParameterNames.filterNot { it in SIGN_IN_PARAMETERS }.sorted()
                .joinToString("&") { name -> "$name=${parsed.queryParameterValues(name).joinToString(",")}" }
            return sha256(parsed.encodedPath + "?" + kept) + ".img"
        }

        private val SIGN_IN_PARAMETERS = setOf("u", "t", "s", "p", "v", "c", "f", "api_key", "apiKey")

        private fun sha256(text: String): String =
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }.take(40)

        @Volatile
        private var instance: RemoteAlbumArtLoader? = null

        fun getInstance(): RemoteAlbumArtLoader =
            instance ?: synchronized(this) {
                instance ?: RemoteAlbumArtLoader().also { instance = it }
            }
    }
}
