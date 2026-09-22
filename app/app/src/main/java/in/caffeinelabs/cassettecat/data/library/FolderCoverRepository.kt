package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.folderCoverDataStore by preferencesDataStore(name = "folder_covers")
private val FOLDER_COVERS = stringPreferencesKey("folder_covers_json")

class FolderCoverRepository private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val folderCovers: StateFlow<Map<String, String>> = context.folderCoverDataStore.data
        .map { prefs ->
            prefs[FOLDER_COVERS]?.let {
                runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull()
            }.orEmpty()
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun getCoverPath(folderPath: String): String? = folderCovers.value[folderPath]

    suspend fun setCover(folderPath: String, coverPath: String) {
        context.folderCoverDataStore.edit { prefs ->
            val current = prefs[FOLDER_COVERS]?.let {
                runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull()
            }.orEmpty()
            prefs[FOLDER_COVERS] = sharedJson.encodeToString(current + (folderPath to coverPath))
        }
    }

    suspend fun clearCover(folderPath: String) {
        context.folderCoverDataStore.edit { prefs ->
            val current = prefs[FOLDER_COVERS]?.let {
                runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull()
            }.orEmpty()
            prefs[FOLDER_COVERS] = sharedJson.encodeToString(current - folderPath)
        }
    }

    companion object {
        @Volatile private var instance: FolderCoverRepository? = null

        fun getInstance(context: Context): FolderCoverRepository =
            instance ?: synchronized(this) {
                instance ?: FolderCoverRepository(context.applicationContext).also { instance = it }
            }
    }
}
