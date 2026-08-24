package `in`.caffeinelabs.cassettecat.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.PlaylistCoverType
import `in`.caffeinelabs.cassettecat.data.library.PlaylistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = PlaylistRepository(app)

    val playlists: StateFlow<List<Playlist>> =
        repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String, onCreated: (Playlist) -> Unit) {
        viewModelScope.launch { onCreated(repository.create(name)) }
    }

    fun create(name: String, songIds: List<String>, onCreated: (Playlist) -> Unit = {}) {
        viewModelScope.launch { onCreated(repository.create(name, songIds)) }
    }

    fun rename(id: String, name: String) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun addSong(playlistId: String, songId: String) {
        viewModelScope.launch { repository.addSong(playlistId, songId) }
    }

    fun addSongs(playlistId: String, songIds: List<String>) {
        viewModelScope.launch { repository.addSongs(playlistId, songIds) }
    }

    fun removeSong(playlistId: String, songId: String) {
        viewModelScope.launch { repository.removeSong(playlistId, songId) }
    }

    fun setCover(playlistId: String, type: PlaylistCoverType, value: String?) {
        viewModelScope.launch { repository.setCover(playlistId, type, value) }
    }

    fun clearCover(playlistId: String) {
        viewModelScope.launch { repository.clearCover(playlistId) }
    }
}
