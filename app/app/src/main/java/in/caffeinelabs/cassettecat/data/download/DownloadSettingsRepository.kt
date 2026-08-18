package `in`.caffeinelabs.cassettecat.data.download

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.downloadSettingsDataStore by preferencesDataStore(name = "download_settings")
private val MAX_CACHE_BYTES = longPreferencesKey("max_cache_bytes")
internal const val DEFAULT_MAX_CACHE_BYTES = 2L * 1024 * 1024 * 1024

class DownloadSettingsRepository(private val context: Context) {
    val maxCacheBytes: Flow<Long> =
        context.downloadSettingsDataStore.data.map { it[MAX_CACHE_BYTES] ?: DEFAULT_MAX_CACHE_BYTES }

    suspend fun setMaxCacheBytes(bytes: Long) {
        context.downloadSettingsDataStore.edit { it[MAX_CACHE_BYTES] = bytes }
    }
}
