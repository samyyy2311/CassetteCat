package `in`.caffeinelabs.cassettecat.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.serviceSettingsDataStore by preferencesDataStore(name = "service_settings")

private val DEEZER_ENABLED = booleanPreferencesKey("deezer_enabled")
private val AUDIODB_ENABLED = booleanPreferencesKey("audiodb_enabled")
private val LRCLIB_ENABLED = booleanPreferencesKey("lrclib_enabled")
private val WIKIPEDIA_ENABLED = booleanPreferencesKey("wikipedia_enabled")
private val GITHUB_UPDATES_ENABLED = booleanPreferencesKey("github_updates_enabled")

enum class ExternalService(val label: String, val description: String) {
    DEEZER("Deezer", "Artist images and album artwork"),
    AUDIODB("TheAudioDB", "Fallback for artist images"),
    LRCLIB("LrcLib", "Synced lyrics when a track has none embedded"),
    WIKIPEDIA("Wikipedia", "About descriptions for artists and albums"),
    GITHUB_UPDATES("GitHub", "Check for newer app releases")
}

@Serializable
data class ServiceSettings(
    val deezerEnabled: Boolean = true,
    val audioDbEnabled: Boolean = true,
    val lrcLibEnabled: Boolean = true,
    val wikipediaEnabled: Boolean = true,
    val githubUpdatesEnabled: Boolean = true
) {
    fun isEnabled(service: ExternalService): Boolean = when (service) {
        ExternalService.DEEZER -> deezerEnabled
        ExternalService.AUDIODB -> audioDbEnabled
        ExternalService.LRCLIB -> lrcLibEnabled
        ExternalService.WIKIPEDIA -> wikipediaEnabled
        ExternalService.GITHUB_UPDATES -> githubUpdatesEnabled
    }
}

// All default on, matching how every one of these ships enabled in the reference app.
class ServiceSettingsRepository(private val context: Context) {
    val settings: Flow<ServiceSettings> = context.serviceSettingsDataStore.data.map { prefs ->
        ServiceSettings(
            deezerEnabled = prefs[DEEZER_ENABLED] ?: true,
            audioDbEnabled = prefs[AUDIODB_ENABLED] ?: true,
            lrcLibEnabled = prefs[LRCLIB_ENABLED] ?: true,
            wikipediaEnabled = prefs[WIKIPEDIA_ENABLED] ?: true,
            githubUpdatesEnabled = prefs[GITHUB_UPDATES_ENABLED] ?: true
        )
    }

    suspend fun setEnabled(service: ExternalService, enabled: Boolean) {
        context.serviceSettingsDataStore.edit { prefs ->
            prefs[service.key()] = enabled
        }
    }

    private fun ExternalService.key() = when (this) {
        ExternalService.DEEZER -> DEEZER_ENABLED
        ExternalService.AUDIODB -> AUDIODB_ENABLED
        ExternalService.LRCLIB -> LRCLIB_ENABLED
        ExternalService.WIKIPEDIA -> WIKIPEDIA_ENABLED
        ExternalService.GITHUB_UPDATES -> GITHUB_UPDATES_ENABLED
    }
}
