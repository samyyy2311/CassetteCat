package `in`.caffeinelabs.cassettecat.data.download

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// Process-wide: SimpleCache must only be opened once per process for a given directory,
// same holder shape as ArtistImageLoaderHolder/GenreArtLoaderHolder.
object DownloadCache {
    @Volatile private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: run {
            // One-time synchronous read: SimpleCache's evictor limit is fixed at
            // construction, and this only runs once per process lifetime.
            val maxBytes = runBlocking { DownloadSettingsRepository(context).maxCacheBytes.first() }
            SimpleCache(
                File(context.filesDir, "song_downloads"),
                LeastRecentlyUsedCacheEvictor(maxBytes),
                StandaloneDatabaseProvider(context)
            ).also { cache = it }
        }
    }
}

// Subsonic/Jellyfin stream URLs embed rotating auth (fresh salt per SubsonicApiClient
// instance, see SubsonicApiClient.kt), so the raw URI can't be the cache key — the "same"
// song gets a different URL after every library refresh. Derives a stable key from the
// URL's identifying part instead, shared by download and playback so both agree on
// cache identity regardless of which fresh URL was used to fetch.
val StreamCacheKeyFactory = CacheKeyFactory { dataSpec ->
    val uri = dataSpec.uri
    uri.getQueryParameter("id")?.let { "subsonic:$it" }
        ?: uri.pathSegments.let { segments ->
            val audioIndex = segments.indexOf("Audio")
            if (audioIndex >= 0) segments.getOrNull(audioIndex + 1)?.let { "jellyfin:$it" } else null
        }
        ?: uri.toString()
}
