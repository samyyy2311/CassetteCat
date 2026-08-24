package `in`.caffeinelabs.cassettecat.data.scrobble

import kotlinx.serialization.Serializable

@Serializable
data class ListenBrainzConfig(
    val enabled: Boolean = false,
    val userToken: String = "",
    val userName: String = ""
)

@Serializable
data class LibreFmConfig(
    val enabled: Boolean = false,
    val username: String = "",
    val sessionKey: String = ""
)

@Serializable
data class ScrobbleSettings(
    val listenBrainz: ListenBrainzConfig = ListenBrainzConfig(),
    val libreFm: LibreFmConfig = LibreFmConfig()
)
