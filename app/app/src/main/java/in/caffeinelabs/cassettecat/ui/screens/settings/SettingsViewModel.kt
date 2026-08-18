package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterConfig
import `in`.caffeinelabs.cassettecat.data.library.LibraryFolderRepository
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettings
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.CredentialStore
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerConfig
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingServerRepository
import `in`.caffeinelabs.cassettecat.data.update.GitHubUpdateChecker
import `in`.caffeinelabs.cassettecat.data.update.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val subsonic: StreamingServerConfig = StreamingServerConfig(),
    val jellyfin: StreamingServerConfig = StreamingServerConfig(),
    val services: ServiceSettings = ServiceSettings(),
    val folderFilter: FolderFilterConfig = FolderFilterConfig()
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val serverRepository = StreamingServerRepository(app)
    private val credentialStore = CredentialStore(app)
    private val serviceSettingsRepository = ServiceSettingsRepository(app)
    private val libraryFolderRepository = LibraryFolderRepository(app)
    private val updateChecker = GitHubUpdateChecker()
    private val currentVersion = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "0.0.0"

    val uiState: StateFlow<SettingsUiState> = combine(
        serverRepository.config(StreamingProtocol.SUBSONIC),
        serverRepository.config(StreamingProtocol.JELLYFIN),
        serviceSettingsRepository.settings,
        libraryFolderRepository.folderFilterConfig
    ) { subsonic, jellyfin, services, folderFilter -> SettingsUiState(subsonic, jellyfin, services, folderFilter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // null = not checked yet this session
    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    fun disconnect(protocol: StreamingProtocol) {
        viewModelScope.launch {
            serverRepository.disconnect(protocol)
            credentialStore.clear(protocol)
        }
    }

    fun setOfflineBlackoutMode(enabled: Boolean) {
        viewModelScope.launch { serviceSettingsRepository.setOfflineBlackoutMode(enabled) }
    }

    fun setServiceEnabled(service: ExternalService, enabled: Boolean) {
        viewModelScope.launch { serviceSettingsRepository.setEnabled(service, enabled) }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateCheckResult.value = null
            _updateCheckResult.value = updateChecker.checkForUpdate(currentVersion)
        }
    }
}
