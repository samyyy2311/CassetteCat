package `in`.caffeinelabs.cassettecat.data.streaming

import android.content.ComponentCallbacks2

// Keep thumbnails when the UI is hidden; memory pressure still clears both caches.
@Suppress("DEPRECATION")
internal fun shouldClearArtworkThumbnails(level: Int): Boolean =
    level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
        level in ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW until ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
