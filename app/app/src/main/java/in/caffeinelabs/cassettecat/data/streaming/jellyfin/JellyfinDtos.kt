package `in`.caffeinelabs.cassettecat.data.streaming.jellyfin

import kotlinx.serialization.Serializable

// Property names deliberately match Jellyfin's PascalCase JSON verbatim (its backend
// is .NET) rather than adding @SerialName annotations to force lowerCamelCase.

@Serializable
data class JellyfinAuthRequest(val Username: String, val Pw: String)

@Serializable
data class JellyfinAuthResult(val User: JellyfinUser, val AccessToken: String)

@Serializable
data class JellyfinUser(val Id: String)

@Serializable
data class JellyfinItemsResponse(val Items: List<JellyfinItem> = emptyList())

@Serializable
data class JellyfinItem(
    val Id: String,
    val Name: String,
    val AlbumArtist: String? = null,
    val Album: String? = null,
    val AlbumId: String? = null,
    val ProductionYear: Int? = null,
    val RunTimeTicks: Long? = null,
    val ImageTags: JellyfinImageTags? = null,
    val UserData: JellyfinUserData? = null,
    val Genres: List<String> = emptyList()
)

@Serializable
data class JellyfinImageTags(val Primary: String? = null)

@Serializable
data class JellyfinUserData(val IsFavorite: Boolean = false)
