package `in`.caffeinelabs.cassettecat.data.scrobble

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.scrobbleDataStore by preferencesDataStore(name = "scrobble_settings")

private val LISTENBRAINZ_ENABLED = booleanPreferencesKey("listenbrainz_enabled")
private val LISTENBRAINZ_TOKEN = stringPreferencesKey("listenbrainz_token")
private val LISTENBRAINZ_USER = stringPreferencesKey("listenbrainz_user")

private val LIBREFM_ENABLED = booleanPreferencesKey("librefm_enabled")
private val LIBREFM_USERNAME = stringPreferencesKey("librefm_username")
private val LIBREFM_SESSION_KEY = stringPreferencesKey("librefm_session_key")

class ScrobbleSettingsRepository(private val context: Context) {
    val settings: Flow<ScrobbleSettings> = context.scrobbleDataStore.data.map { prefs ->
        ScrobbleSettings(
            listenBrainz = ListenBrainzConfig(
                enabled = prefs[LISTENBRAINZ_ENABLED] ?: false,
                userToken = prefs[LISTENBRAINZ_TOKEN] ?: "",
                userName = prefs[LISTENBRAINZ_USER] ?: ""
            ),
            libreFm = LibreFmConfig(
                enabled = prefs[LIBREFM_ENABLED] ?: false,
                username = prefs[LIBREFM_USERNAME] ?: "",
                sessionKey = prefs[LIBREFM_SESSION_KEY] ?: ""
            )
        )
    }

    suspend fun saveListenBrainz(token: String, userName: String, enabled: Boolean = true) {
        context.scrobbleDataStore.edit { prefs ->
            prefs[LISTENBRAINZ_TOKEN] = token
            prefs[LISTENBRAINZ_USER] = userName
            prefs[LISTENBRAINZ_ENABLED] = enabled
        }
    }

    suspend fun setListenBrainzEnabled(enabled: Boolean) {
        context.scrobbleDataStore.edit { prefs ->
            prefs[LISTENBRAINZ_ENABLED] = enabled
        }
    }

    suspend fun disconnectListenBrainz() {
        context.scrobbleDataStore.edit { prefs ->
            prefs[LISTENBRAINZ_TOKEN] = ""
            prefs[LISTENBRAINZ_USER] = ""
            prefs[LISTENBRAINZ_ENABLED] = false
        }
    }

    suspend fun saveLibreFm(username: String, sessionKey: String, enabled: Boolean = true) {
        context.scrobbleDataStore.edit { prefs ->
            prefs[LIBREFM_USERNAME] = username
            prefs[LIBREFM_SESSION_KEY] = sessionKey
            prefs[LIBREFM_ENABLED] = enabled
        }
    }

    suspend fun setLibreFmEnabled(enabled: Boolean) {
        context.scrobbleDataStore.edit { prefs ->
            prefs[LIBREFM_ENABLED] = enabled
        }
    }

    suspend fun disconnectLibreFm() {
        context.scrobbleDataStore.edit { prefs ->
            prefs[LIBREFM_USERNAME] = ""
            prefs[LIBREFM_SESSION_KEY] = ""
            prefs[LIBREFM_ENABLED] = false
        }
    }
}
