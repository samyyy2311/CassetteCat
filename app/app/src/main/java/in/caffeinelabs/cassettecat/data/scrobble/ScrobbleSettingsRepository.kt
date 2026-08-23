package `in`.caffeinelabs.cassettecat.data.scrobble

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private val Context.scrobbleDataStore by preferencesDataStore(name = "scrobble_settings")

private val LISTENBRAINZ_ENABLED = booleanPreferencesKey("listenbrainz_enabled")
private val LISTENBRAINZ_TOKEN = stringPreferencesKey("listenbrainz_token")
private val LISTENBRAINZ_USER = stringPreferencesKey("listenbrainz_user")

private val LIBREFM_ENABLED = booleanPreferencesKey("librefm_enabled")
private val LIBREFM_USERNAME = stringPreferencesKey("librefm_username")
private val LIBREFM_SESSION_KEY = stringPreferencesKey("librefm_session_key")

internal fun credentialToMigrate(legacy: String?, encrypted: String?): String? =
    legacy?.takeIf { it.isNotEmpty() && encrypted.isNullOrEmpty() }

class ScrobbleSettingsRepository(private val context: Context) {
    private val credentialStore = CredentialStore(context)

    val settings: Flow<ScrobbleSettings> = flow {
        migrateLegacyCredentials()
        emitAll(context.scrobbleDataStore.data.map { prefs ->
            ScrobbleSettings(
                listenBrainz = ListenBrainzConfig(
                    enabled = prefs[LISTENBRAINZ_ENABLED] ?: false,
                    userToken = credentialStore.getListenBrainzToken().orEmpty(),
                    userName = prefs[LISTENBRAINZ_USER] ?: ""
                ),
                libreFm = LibreFmConfig(
                    enabled = prefs[LIBREFM_ENABLED] ?: false,
                    username = prefs[LIBREFM_USERNAME] ?: "",
                    sessionKey = credentialStore.getLibreFmSessionKey().orEmpty()
                )
            )
        })
    }

    suspend fun saveListenBrainz(token: String, userName: String, enabled: Boolean = true) {
        credentialStore.saveListenBrainzToken(token)
        context.scrobbleDataStore.edit { prefs ->
            prefs.remove(LISTENBRAINZ_TOKEN)
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
        credentialStore.clearListenBrainzToken()
        context.scrobbleDataStore.edit { prefs ->
            prefs.remove(LISTENBRAINZ_TOKEN)
            prefs[LISTENBRAINZ_USER] = ""
            prefs[LISTENBRAINZ_ENABLED] = false
        }
    }

    suspend fun saveLibreFm(username: String, sessionKey: String, enabled: Boolean = true) {
        credentialStore.saveLibreFmSessionKey(sessionKey)
        context.scrobbleDataStore.edit { prefs ->
            prefs[LIBREFM_USERNAME] = username
            prefs.remove(LIBREFM_SESSION_KEY)
            prefs[LIBREFM_ENABLED] = enabled
        }
    }

    suspend fun setLibreFmEnabled(enabled: Boolean) {
        context.scrobbleDataStore.edit { prefs ->
            prefs[LIBREFM_ENABLED] = enabled
        }
    }

    suspend fun disconnectLibreFm() {
        credentialStore.clearLibreFmSessionKey()
        context.scrobbleDataStore.edit { prefs ->
            prefs[LIBREFM_USERNAME] = ""
            prefs.remove(LIBREFM_SESSION_KEY)
            prefs[LIBREFM_ENABLED] = false
        }
    }

    private suspend fun migrateLegacyCredentials() {
        val prefs = context.scrobbleDataStore.data.first()
        val listenBrainzToken = prefs[LISTENBRAINZ_TOKEN]
        val libreFmSession = prefs[LIBREFM_SESSION_KEY]
        if (listenBrainzToken.isNullOrEmpty() && libreFmSession.isNullOrEmpty()) return
        credentialToMigrate(listenBrainzToken, credentialStore.getListenBrainzToken())
            ?.let(credentialStore::saveListenBrainzToken)
        credentialToMigrate(libreFmSession, credentialStore.getLibreFmSessionKey())
            ?.let(credentialStore::saveLibreFmSessionKey)
        context.scrobbleDataStore.edit {
            it.remove(LISTENBRAINZ_TOKEN)
            it.remove(LIBREFM_SESSION_KEY)
        }
    }
}
