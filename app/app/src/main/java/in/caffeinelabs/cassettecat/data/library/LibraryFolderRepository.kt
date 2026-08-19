package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.libraryFolderDataStore by preferencesDataStore(name = "library_folders")
private val FOLDER_MODE = stringPreferencesKey("folder_filter_mode")
private val FOLDER_PATHS = stringSetPreferencesKey("folder_filter_paths")

enum class FolderFilterMode { NONE, WHITELIST, BLACKLIST }

@Serializable
data class FolderFilterConfig(
    val mode: FolderFilterMode = FolderFilterMode.NONE,
    val folders: Set<String> = emptySet()
)

class LibraryFolderRepository(private val context: Context) {
    val folderFilterConfig: Flow<FolderFilterConfig> = context.libraryFolderDataStore.data.map { prefs ->
        FolderFilterConfig(
            mode = prefs[FOLDER_MODE]?.let { runCatching { FolderFilterMode.valueOf(it) }.getOrNull() }
                ?: FolderFilterMode.NONE,
            folders = prefs[FOLDER_PATHS] ?: emptySet()
        )
    }

    suspend fun setFolderFilter(config: FolderFilterConfig) {
        context.libraryFolderDataStore.edit { prefs ->
            prefs[FOLDER_MODE] = config.mode.name
            prefs[FOLDER_PATHS] = config.folders
        }
    }
}

// Only need the path string to filter MediaStore's DATA column; tree Uri is resolved
// once and discarded, no takePersistableUriPermission needed.
fun resolveFolderPath(context: Context, treeUri: Uri): String? {
    val docId = DocumentsContract.getTreeDocumentId(treeUri)
    val parts = docId.split(":", limit = 2)
    val volume = parts.getOrNull(0) ?: return null
    val relativePath = parts.getOrElse(1) { "" }

    if (volume == "primary") {
        @Suppress("DEPRECATION")
        return "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
    }

    // Secondary volumes (SD cards): StorageVolume.getDirectory() is API 30+, a
    // documented gap on 26-29 rather than a reflection-based fallback.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val storageManager = context.getSystemService(StorageManager::class.java)
        val volumeDir = storageManager?.storageVolumes
            ?.firstOrNull { it.uuid == volume }
            ?.directory
        if (volumeDir != null) return "${volumeDir.absolutePath}/$relativePath"
    }
    return null
}
