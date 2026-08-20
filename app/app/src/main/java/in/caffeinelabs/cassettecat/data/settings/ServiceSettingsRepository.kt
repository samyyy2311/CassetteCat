package `in`.caffeinelabs.cassettecat.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.serviceSettingsDataStore by preferencesDataStore(name = "service_settings")

private val OFFLINE_BLACKOUT_MODE = booleanPreferencesKey("offline_blackout_mode")
private val DEEZER_ENABLED = booleanPreferencesKey("deezer_enabled")
private val AUDIODB_ENABLED = booleanPreferencesKey("audiodb_enabled")
private val LRCLIB_ENABLED = booleanPreferencesKey("lrclib_enabled")
private val WIKIPEDIA_ENABLED = booleanPreferencesKey("wikipedia_enabled")
private val COVER_ART_ARCHIVE_ENABLED = booleanPreferencesKey("cover_art_archive_enabled")
private val GITHUB_UPDATES_ENABLED = booleanPreferencesKey("github_updates_enabled")
private val RADIO_BROWSER_ENABLED = booleanPreferencesKey("radio_browser_enabled")

enum class ExternalService(val label: String, val description: String) {
    DEEZER("Deezer", "Artist images and album artwork"),
    AUDIODB("TheAudioDB", "Fallback for artist images"),
    LRCLIB("LrcLib", "Synced lyrics when a track has none embedded"),
    COVER_ART_ARCHIVE("Cover Art Archive", "Archival CD and vinyl artwork from MusicBrainz"),
    WIKIPEDIA("Wikipedia", "About descriptions for artists and albums"),
    GITHUB_UPDATES("GitHub", "Check for newer app releases"),
    RADIO_BROWSER("Radio Browser", "Search and browse internet radio stations")
}

@Serializable
data class ServiceSettings(
    val offlineBlackoutMode: Boolean = false,
    val deezerEnabled: Boolean = true,
    val audioDbEnabled: Boolean = true,
    val lrcLibEnabled: Boolean = true,
    val coverArtArchiveEnabled: Boolean = true,
    val wikipediaEnabled: Boolean = true,
    val githubUpdatesEnabled: Boolean = true,
    val radioBrowserEnabled: Boolean = true
) {
    fun isEnabled(service: ExternalService): Boolean = if (offlineBlackoutMode) false else when (service) {
        ExternalService.DEEZER -> deezerEnabled
        ExternalService.AUDIODB -> audioDbEnabled
        ExternalService.LRCLIB -> lrcLibEnabled
        ExternalService.COVER_ART_ARCHIVE -> coverArtArchiveEnabled
        ExternalService.WIKIPEDIA -> wikipediaEnabled
        ExternalService.GITHUB_UPDATES -> githubUpdatesEnabled
        ExternalService.RADIO_BROWSER -> radioBrowserEnabled
    }
}

class ServiceSettingsRepository(private val context: Context) {
    val settings: Flow<ServiceSettings> = context.serviceSettingsDataStore.data.map { prefs ->
        ServiceSettings(
            offlineBlackoutMode = prefs[OFFLINE_BLACKOUT_MODE] ?: false,
            deezerEnabled = prefs[DEEZER_ENABLED] ?: true,
            audioDbEnabled = prefs[AUDIODB_ENABLED] ?: true,
            lrcLibEnabled = prefs[LRCLIB_ENABLED] ?: true,
            coverArtArchiveEnabled = prefs[COVER_ART_ARCHIVE_ENABLED] ?: true,
            wikipediaEnabled = prefs[WIKIPEDIA_ENABLED] ?: true,
            githubUpdatesEnabled = prefs[GITHUB_UPDATES_ENABLED] ?: true,
            radioBrowserEnabled = prefs[RADIO_BROWSER_ENABLED] ?: true
        )
    }

    suspend fun setOfflineBlackoutMode(enabled: Boolean) {
        context.serviceSettingsDataStore.edit { prefs ->
            prefs[OFFLINE_BLACKOUT_MODE] = enabled
        }
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
        ExternalService.COVER_ART_ARCHIVE -> COVER_ART_ARCHIVE_ENABLED
        ExternalService.WIKIPEDIA -> WIKIPEDIA_ENABLED
        ExternalService.GITHUB_UPDATES -> GITHUB_UPDATES_ENABLED
        ExternalService.RADIO_BROWSER -> RADIO_BROWSER_ENABLED
    }
}
