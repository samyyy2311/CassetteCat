package `in`.caffeinelabs.cassettecat.data.backup

import android.content.Context
import android.util.Base64
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.LibraryFolderRepository
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.PlaylistCoverStorage
import `in`.caffeinelabs.cassettecat.data.library.PlaylistCoverType
import `in`.caffeinelabs.cassettecat.data.library.PlaylistRepository
import `in`.caffeinelabs.cassettecat.data.playback.EqualizerSettingsRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.stats.ListeningStatsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// Credentials (data/streaming/CredentialStore.kt) are deliberately excluded: their AES key
// lives in Android Keystore, is hardware-backed, and can't be exported or survive an
// uninstall, so including the ciphertext here would just be undecryptable dead weight.
// Playback queue/position and the onboarding flag are excluded too: session state, not data.
class BackupRepository(private val context: Context) {
    private val appPreferencesRepository = AppPreferencesRepository(context)
    private val serviceSettingsRepository = ServiceSettingsRepository(context)
    private val equalizerSettingsRepository = EqualizerSettingsRepository(context)
    private val favoritesRepository = FavoritesRepository(context)
    private val folderRepository = LibraryFolderRepository(context)
    private val playlistRepository = PlaylistRepository(context)
    private val statsRepository = ListeningStatsRepository(context)
    private val serverRepository = StreamingServerRepository(context)
    private val coverStorage = PlaylistCoverStorage(context)

    suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        val playlists = playlistRepository.playlists.first().map { playlist ->
            val coverImageBase64 = if (playlist.coverType == PlaylistCoverType.IMAGE) {
                playlist.coverValue?.let { path ->
                    runCatching { File(path).readBytes() }.getOrNull()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                }
            } else {
                null
            }
            BackupPlaylist(
                id = playlist.id,
                name = playlist.name,
                songIds = playlist.songIds,
                coverType = playlist.coverType,
                coverValue = playlist.coverValue,
                coverImageBase64 = coverImageBase64
            )
        }

        val streamingServers = StreamingProtocol.entries.associate { protocol ->
            protocol.name to serverRepository.config(protocol).first()
        }

        val bundle = BackupBundle(
            createdAt = System.currentTimeMillis(),
            serviceSettings = serviceSettingsRepository.settings.first(),
            equalizer = equalizerSettingsRepository.levels.first(),
            favoriteIds = favoritesRepository.favoriteIds.first(),
            folderFilter = folderRepository.folderFilterConfig.first(),
            playlists = playlists,
            listeningStatsMonthly = statsRepository.monthlyStats.first(),
            listeningMilestones = statsRepository.milestones.first(),
            streamingServers = streamingServers,
            appPreferences = appPreferencesRepository.exportForBackup()
        )
        sharedJson.encodeToString(bundle)
    }

    suspend fun restoreBackup(content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bundle = sharedJson.decodeFromString<BackupBundle>(content)

            bundle.appPreferences?.let { appPreferencesRepository.restoreFromBackup(it) }

            ExternalService.entries.forEach { service ->
                serviceSettingsRepository.setEnabled(service, bundle.serviceSettings.isEnabled(service))
            }
            equalizerSettingsRepository.setBandLevels(bundle.equalizer.bandLevelsMb)
            favoritesRepository.replaceAll(bundle.favoriteIds)
            folderRepository.setFolderFilter(bundle.folderFilter)
            statsRepository.replaceAll(bundle.listeningStatsMonthly, bundle.listeningMilestones)

            val restoredPlaylists = bundle.playlists.map { backupPlaylist ->
                val coverValue = if (backupPlaylist.coverType == PlaylistCoverType.IMAGE) {
                    backupPlaylist.coverImageBase64
                        ?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
                        ?.let { bytes -> coverStorage.restore(backupPlaylist.id, bytes) }
                } else {
                    backupPlaylist.coverValue
                }
                Playlist(
                    id = backupPlaylist.id,
                    name = backupPlaylist.name,
                    songIds = backupPlaylist.songIds,
                    coverType = if (coverValue == null) PlaylistCoverType.NONE else backupPlaylist.coverType,
                    coverValue = coverValue
                )
            }
            playlistRepository.replaceAll(restoredPlaylists)

            bundle.streamingServers.forEach { (protocolName, config) ->
                runCatching { StreamingProtocol.valueOf(protocolName) }.getOrNull()?.let { protocol ->
                    serverRepository.setConfig(protocol, config.copy(connected = false))
                }
            }
        }
    }
}
