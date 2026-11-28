@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.data.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// SimpleCache must only be opened once per process for a directory.
@UnstableApi
object DownloadCache {
    @Volatile private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: run {
            // Synchronous read to configure evictor limit on initialization.
            val maxBytes = runBlocking { DownloadSettingsRepository(context).maxCacheBytes.first() }
            SimpleCache(
                File(context.filesDir, "song_downloads"),
                LeastRecentlyUsedCacheEvictor(maxBytes),
                StandaloneDatabaseProvider(context)
            ).also { cache = it }
        }
    }
}

// Server stream URLs rotate auth salts; key on the item ID so cached audio persists across sessions.
@UnstableApi
val StreamCacheKeyFactory = CacheKeyFactory { dataSpec ->
    val uri = dataSpec.uri
    uri.getQueryParameter("id")?.let { "subsonic:$it" }
        ?: uri.pathSegments.let { segments ->
            val audioIndex = segments.indexOf("Audio")
            if (audioIndex >= 0) segments.getOrNull(audioIndex + 1)?.let { "jellyfin:$it" } else null
        }
        ?: uri.toString()
}
