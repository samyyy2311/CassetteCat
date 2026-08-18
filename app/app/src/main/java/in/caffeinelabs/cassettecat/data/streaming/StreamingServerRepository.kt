package `in`.caffeinelabs.cassettecat.data.streaming

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlinx.serialization.Serializable

private val Context.streamingDataStore by preferencesDataStore(name = "streaming_servers")
private val DEVICE_ID = stringPreferencesKey("device_id")

// Non-secret config only, one server per protocol (see MusicSource in Song.kt).
// Passwords/access tokens live in CredentialStore instead.
@Serializable
data class StreamingServerConfig(
    val serverUrl: String = "",
    val username: String = "",
    val userId: String? = null,
    val connected: Boolean = false
)

class StreamingServerRepository(private val context: Context) {
    private fun urlKey(protocol: StreamingProtocol) = stringPreferencesKey("${protocol.name}_url")
    private fun usernameKey(protocol: StreamingProtocol) = stringPreferencesKey("${protocol.name}_username")
    private fun userIdKey(protocol: StreamingProtocol) = stringPreferencesKey("${protocol.name}_user_id")
    private fun connectedKey(protocol: StreamingProtocol) = booleanPreferencesKey("${protocol.name}_connected")

    fun config(protocol: StreamingProtocol): Flow<StreamingServerConfig> =
        context.streamingDataStore.data.map { prefs ->
            StreamingServerConfig(
                serverUrl = prefs[urlKey(protocol)] ?: "",
                username = prefs[usernameKey(protocol)] ?: "",
                userId = prefs[userIdKey(protocol)],
                connected = prefs[connectedKey(protocol)] ?: false
            )
        }

    suspend fun setConfig(protocol: StreamingProtocol, config: StreamingServerConfig) {
        context.streamingDataStore.edit { prefs ->
            prefs[urlKey(protocol)] = config.serverUrl
            prefs[usernameKey(protocol)] = config.username
            if (config.userId != null) prefs[userIdKey(protocol)] = config.userId
            prefs[connectedKey(protocol)] = config.connected
        }
    }

    suspend fun disconnect(protocol: StreamingProtocol) {
        setConfig(protocol, StreamingServerConfig())
    }

    // Stable device identity for Jellyfin's device list; not a secret, so it lives here.
    suspend fun deviceId(): String {
        val existing = context.streamingDataStore.data.map { it[DEVICE_ID] }.first()
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        context.streamingDataStore.edit { it[DEVICE_ID] = generated }
        return generated
    }
}
