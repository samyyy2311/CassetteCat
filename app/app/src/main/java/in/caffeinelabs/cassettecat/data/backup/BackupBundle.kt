package `in`.caffeinelabs.cassettecat.data.backup

import `in`.caffeinelabs.cassettecat.data.library.FolderFilterConfig
import `in`.caffeinelabs.cassettecat.data.library.PlaylistCoverType
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerLevels
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettings
import `in`.caffeinelabs.cassettecat.data.stats.Milestone
import `in`.caffeinelabs.cassettecat.data.stats.MonthlyStats
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import kotlinx.serialization.Serializable

@Serializable
data class BackupBundle(
    val version: Int = 1,
    val createdAt: Long,
    val serviceSettings: ServiceSettings,
    val equalizer: EqualizerLevels,
    val favoriteIds: Set<String>,
    val folderFilter: FolderFilterConfig,
    val playlists: List<BackupPlaylist>,
    // keyed by YearMonth.toString(), e.g. "2026-06"
    val listeningStatsMonthly: Map<String, MonthlyStats>,
    val listeningMilestones: List<Milestone>,
    // keyed by StreamingProtocol.name
    val streamingServers: Map<String, StreamingServerConfig>
)

@Serializable
data class BackupPlaylist(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val coverType: PlaylistCoverType,
    val coverValue: String?,
    // only set when coverType == IMAGE; base64 of the downscaled JPEG
    val coverImageBase64: String? = null
)
