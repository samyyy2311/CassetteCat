package `in`.caffeinelabs.cassettecat.data.scrobble

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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

private val ScrobbleAccount.enabledKey get() = booleanPreferencesKey("${id}_enabled")
private val ScrobbleAccount.usernameKey get() = stringPreferencesKey("${id}_username")
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
                libreFm = accountConfig(prefs, ScrobbleAccount.LIBRE_FM),
                lastFm = accountConfig(prefs, ScrobbleAccount.LAST_FM)
            )
        })
    }

    private fun accountConfig(prefs: Preferences, account: ScrobbleAccount) = ScrobbleAccountConfig(
        enabled = prefs[account.enabledKey] ?: false,
        username = prefs[account.usernameKey] ?: "",
        sessionKey = credentialStore.getScrobbleSessionKey(account.id).orEmpty()
    )

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

    suspend fun saveAccount(account: ScrobbleAccount, username: String, sessionKey: String) {
        credentialStore.saveScrobbleSessionKey(account.id, sessionKey)
        context.scrobbleDataStore.edit { prefs ->
            prefs[account.usernameKey] = username
            prefs[account.enabledKey] = true
        }
    }

    suspend fun setAccountEnabled(account: ScrobbleAccount, enabled: Boolean) {
        context.scrobbleDataStore.edit { prefs ->
            prefs[account.enabledKey] = enabled
        }
    }

    suspend fun disconnectAccount(account: ScrobbleAccount) {
        credentialStore.clearScrobbleSessionKey(account.id)
        context.scrobbleDataStore.edit { prefs ->
            prefs[account.usernameKey] = ""
            prefs[account.enabledKey] = false
        }
    }

    private suspend fun migrateLegacyCredentials() {
        val prefs = context.scrobbleDataStore.data.first()
        val listenBrainzToken = prefs[LISTENBRAINZ_TOKEN]
        val libreFmSession = prefs[LIBREFM_SESSION_KEY]
        if (listenBrainzToken.isNullOrEmpty() && libreFmSession.isNullOrEmpty()) return
        credentialToMigrate(listenBrainzToken, credentialStore.getListenBrainzToken())
            ?.let(credentialStore::saveListenBrainzToken)
        credentialToMigrate(libreFmSession, credentialStore.getScrobbleSessionKey(ScrobbleAccount.LIBRE_FM.id))
            ?.let { credentialStore.saveScrobbleSessionKey(ScrobbleAccount.LIBRE_FM.id, it) }
        context.scrobbleDataStore.edit {
            it.remove(LISTENBRAINZ_TOKEN)
            it.remove(LIBREFM_SESSION_KEY)
        }
    }
}
