package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterConfig
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterMode
import `in`.caffeinelabs.cassettecat.data.library.LibraryFolderRepository
import `in`.caffeinelabs.cassettecat.data.library.resolveFolderPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LibraryScanViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = LibraryFolderRepository(app)

    private val _config = MutableStateFlow(FolderFilterConfig())
    val config: StateFlow<FolderFilterConfig> = _config.asStateFlow()

    init {
        viewModelScope.launch { _config.value = repository.folderFilterConfig.first() }
    }

    fun setMode(mode: FolderFilterMode) {
        _config.value = _config.value.copy(mode = mode)
    }

    fun addFolder(treeUri: Uri) {
        val path = resolveFolderPath(getApplication(), treeUri) ?: return
        _config.value = _config.value.copy(folders = _config.value.folders + path)
    }

    fun removeFolder(path: String) {
        _config.value = _config.value.copy(folders = _config.value.folders - path)
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.setFolderFilter(_config.value)
            onSaved()
        }
    }

    fun skip(onSkipped: () -> Unit) {
        viewModelScope.launch {
            repository.setFolderFilter(FolderFilterConfig())
            onSkipped()
        }
    }
}
