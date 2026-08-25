package `in`.caffeinelabs.cassettecat.data.library

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val HISTORY_LIMIT = 12
private val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history")
private val KEY_RECENT_QUERIES = stringPreferencesKey("recent_queries")

class SearchHistoryRepository(private val context: Context) {

    val recentQueries: Flow<List<String>> = context.searchHistoryDataStore.data.map { it.decode() }

    suspend fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        context.searchHistoryDataStore.edit { prefs ->
            val withoutQuery = prefs.decode().filterNot { it.equals(trimmed, ignoreCase = true) }
            prefs[KEY_RECENT_QUERIES] = sharedJson.encodeToString((listOf(trimmed) + withoutQuery).take(HISTORY_LIMIT))
        }
    }

    suspend fun removeQuery(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            prefs[KEY_RECENT_QUERIES] = sharedJson.encodeToString(prefs.decode().filterNot { it.equals(query, ignoreCase = true) })
        }
    }

    suspend fun clearHistory() {
        context.searchHistoryDataStore.edit { prefs -> prefs[KEY_RECENT_QUERIES] = sharedJson.encodeToString(emptyList<String>()) }
    }

    private fun Preferences.decode(): List<String> =
        this[KEY_RECENT_QUERIES]?.let { runCatching { sharedJson.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()

    companion object {
        @Volatile
        private var instance: SearchHistoryRepository? = null

        fun getInstance(context: Context): SearchHistoryRepository =
            instance ?: synchronized(this) {
                instance ?: SearchHistoryRepository(context.applicationContext).also { instance = it }
            }
    }
}
