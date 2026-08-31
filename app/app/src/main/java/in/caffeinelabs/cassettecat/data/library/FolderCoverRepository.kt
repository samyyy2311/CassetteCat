package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.folderCoverDataStore by preferencesDataStore(name = "folder_covers")
private val FOLDER_COVERS = stringPreferencesKey("folder_covers_json")

class FolderCoverRepository(private val context: Context) {
    val folderCovers: Flow<Map<String, String>> = context.folderCoverDataStore.data.map { prefs ->
        prefs[FOLDER_COVERS]?.let { runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull() }.orEmpty()
    }

    suspend fun setCover(folderPath: String, coverPath: String) {
        context.folderCoverDataStore.edit { prefs ->
            val current = prefs[FOLDER_COVERS]?.let { runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull() }.orEmpty()
            prefs[FOLDER_COVERS] = sharedJson.encodeToString(current + (folderPath to coverPath))
        }
    }

    suspend fun clearCover(folderPath: String) {
        context.folderCoverDataStore.edit { prefs ->
            val current = prefs[FOLDER_COVERS]?.let { runCatching { sharedJson.decodeFromString<Map<String, String>>(it) }.getOrNull() }.orEmpty()
            prefs[FOLDER_COVERS] = sharedJson.encodeToString(current - folderPath)
        }
    }
}
