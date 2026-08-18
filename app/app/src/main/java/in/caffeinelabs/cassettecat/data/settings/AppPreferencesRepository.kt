package `in`.caffeinelabs.cassettecat.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")
private val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
private val LISTENING_STATS_ENABLED = booleanPreferencesKey("listening_stats_enabled")

data class AppPreferences(
    val wifiOnlyDownloads: Boolean = false,
    val listeningStatsEnabled: Boolean = true
)

class AppPreferencesRepository(private val context: Context) {
    val preferences: Flow<AppPreferences> = context.appPreferencesDataStore.data.map { prefs ->
        AppPreferences(
            wifiOnlyDownloads = prefs[WIFI_ONLY_DOWNLOADS] ?: false,
            listeningStatsEnabled = prefs[LISTENING_STATS_ENABLED] ?: true
        )
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[WIFI_ONLY_DOWNLOADS] = enabled }
    }

    suspend fun setListeningStatsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[LISTENING_STATS_ENABLED] = enabled }
    }
}
