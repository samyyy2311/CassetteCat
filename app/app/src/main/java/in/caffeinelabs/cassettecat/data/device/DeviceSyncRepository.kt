package `in`.caffeinelabs.cassettecat.data.device

import android.net.Network
import `in`.caffeinelabs.cassettecat.data.library.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface SyncItemState {
    data object Queued : SyncItemState
    data object Uploading : SyncItemState
    data object Done : SyncItemState
    data class Failed(val message: String) : SyncItemState
}

class DeviceSyncRepository {
    private val apiClient = SyncApiClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncStates = MutableStateFlow<Map<String, SyncItemState>>(emptyMap())
    val syncStates: StateFlow<Map<String, SyncItemState>> = _syncStates.asStateFlow()

    private val _remoteManifest = MutableStateFlow<List<RemoteSongEntry>?>(null)
    val remoteManifest: StateFlow<List<RemoteSongEntry>?> = _remoteManifest.asStateFlow()

    suspend fun refreshManifest(host: String, port: Int, network: Network?) {
        _remoteManifest.value = apiClient.getLibraryManifest(host, port, network)
    }

    // No manifest fetched yet: treat everything as pending rather than nothing.
    fun pendingSongs(localSongs: List<Song>): List<Song> {
        val remoteByPath = _remoteManifest.value?.associateBy { it.path } ?: return localSongs
        return localSongs.filter { song ->
            val path = song.filePath ?: return@filter false
            val entry = remoteByPath[path]
            entry == null || entry.sizeBytes != File(path).length()
        }
    }

    fun syncSongs(songs: List<Song>, host: String, port: Int, network: Network?) {
        val pending = songs.filter { it.filePath != null }
        _syncStates.value = _syncStates.value + pending.associate { it.id to SyncItemState.Queued }
        scope.launch {
            for (song in pending) {
                _syncStates.value = _syncStates.value + (song.id to SyncItemState.Uploading)
                val ok = apiClient.uploadSong(host, port, song, network)
                _syncStates.value = _syncStates.value + (
                    song.id to if (ok) SyncItemState.Done else SyncItemState.Failed("Upload failed")
                )
            }
            refreshManifest(host, port, network)
        }
    }

    fun release() = scope.cancel()
}
