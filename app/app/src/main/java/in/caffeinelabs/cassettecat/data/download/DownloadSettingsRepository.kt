package `in`.caffeinelabs.cassettecat.data.download

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.downloadSettingsDataStore by preferencesDataStore(name = "download_settings")
private val MAX_CACHE_BYTES = longPreferencesKey("max_cache_bytes")
private val AUTO_DOWNLOAD_FAVORITES = booleanPreferencesKey("auto_download_favorites")
internal const val DEFAULT_MAX_CACHE_BYTES = 2L * 1024 * 1024 * 1024
internal val DOWNLOAD_CACHE_LIMIT_OPTIONS_MB = listOf(512, 1024, 2048, 5120, 10240)

class DownloadSettingsRepository(private val context: Context) {
    val maxCacheBytes: Flow<Long> =
        context.downloadSettingsDataStore.data.map { it[MAX_CACHE_BYTES] ?: DEFAULT_MAX_CACHE_BYTES }

    val autoDownloadFavorites: Flow<Boolean> =
        context.downloadSettingsDataStore.data.map { it[AUTO_DOWNLOAD_FAVORITES] ?: false }

    suspend fun setMaxCacheBytes(bytes: Long) {
        context.downloadSettingsDataStore.edit { it[MAX_CACHE_BYTES] = bytes }
    }

    suspend fun setAutoDownloadFavorites(enabled: Boolean) {
        context.downloadSettingsDataStore.edit { it[AUTO_DOWNLOAD_FAVORITES] = enabled }
    }
}
