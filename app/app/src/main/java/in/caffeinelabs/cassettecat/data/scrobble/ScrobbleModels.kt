package `in`.caffeinelabs.cassettecat.data.scrobble

import kotlinx.serialization.Serializable

@Serializable
data class ListenBrainzConfig(
    val enabled: Boolean = false,
    val userToken: String = "",
    val userName: String = ""
)

/** An account on an Audioscrobbler network; [id] names its settings and stored session key. */
enum class ScrobbleAccount(val id: String) {
    LIBRE_FM("librefm"),
    LAST_FM("lastfm");

    val client: AudioscrobblerClient?
        get() = when (this) {
            LIBRE_FM -> AudioscrobblerClient.libreFm
            LAST_FM -> AudioscrobblerClient.lastFm
        }
}

@Serializable
data class ScrobbleAccountConfig(
    val enabled: Boolean = false,
    val username: String = "",
    val sessionKey: String = ""
)

@Serializable
data class ScrobbleSettings(
    val listenBrainz: ListenBrainzConfig = ListenBrainzConfig(),
    val libreFm: ScrobbleAccountConfig = ScrobbleAccountConfig(),
    val lastFm: ScrobbleAccountConfig = ScrobbleAccountConfig()
) {
    fun account(account: ScrobbleAccount): ScrobbleAccountConfig = when (account) {
        ScrobbleAccount.LIBRE_FM -> libreFm
        ScrobbleAccount.LAST_FM -> lastFm
    }
}
