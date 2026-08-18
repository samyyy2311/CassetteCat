package `in`.caffeinelabs.cassettecat.data.scrobble

import kotlinx.serialization.Serializable

enum class ScrobbleProvider(val label: String, val website: String) {
    LISTENBRAINZ("ListenBrainz", "https://listenbrainz.org"),
    LIBRE_FM("Libre.fm", "https://libre.fm")
}

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
