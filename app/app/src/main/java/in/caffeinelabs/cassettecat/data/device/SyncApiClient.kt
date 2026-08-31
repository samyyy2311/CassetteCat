package `in`.caffeinelabs.cassettecat.data.device

import android.net.Network
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@Serializable
data class RemoteSongEntry(val path: String, val sizeBytes: Long)

@Serializable
private data class SyncSongMetadata(val path: String, val title: String, val artist: String, val sizeBytes: Long)

internal fun remoteSyncPath(song: Song, fileName: String): String = "${song.artist}/${song.album}/$fileName"

@Serializable
private data class SyncUploadResponse(val ok: Boolean, val error: String? = null)

class SyncApiClient {
    suspend fun getLibraryManifest(host: String, port: Int = 80, network: Network? = null): List<RemoteSongEntry>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("http://$host:$port/api/library")
                    .build()

                val response = deviceHttpClient(network).newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) return@runCatching null
                    sharedJson.decodeFromString<List<RemoteSongEntry>>(it.body.string())
                }
            }.getOrNull()
        }

    suspend fun uploadSong(host: String, port: Int = 80, song: Song, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = song.filePath ?: return@runCatching false
                val file = File(path)
                if (!file.exists()) return@runCatching false

                val metadata = SyncSongMetadata(
                    path = remoteSyncPath(song, file.name),
                    title = song.title,
                    artist = song.artist,
                    sizeBytes = file.length()
                )
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "metadata",
                        null,
                        sharedJson.encodeToString(SyncSongMetadata.serializer(), metadata).toRequestBody("application/json".toMediaType())
                    )
                    .addFormDataPart("file", file.name, file.asRequestBody("audio/*".toMediaType()))
                    .build()

                val request = Request.Builder()
                    .url("http://$host:$port/api/sync/songs")
                    .post(body)
                    .build()

                val response = deviceUploadHttpClient(network).newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) return@runCatching false
                    sharedJson.decodeFromString<SyncUploadResponse>(it.body.string()).ok
                }
            }.getOrDefault(false)
        }

    suspend fun deleteSong(host: String, port: Int = 80, path: String, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "http://$host:$port/api/sync/songs".toHttpUrl().newBuilder()
                    .addQueryParameter("path", path)
                    .build()
                val request = Request.Builder().url(url).delete().build()
                val response = deviceHttpClient(network).newCall(request).execute()
                response.use { it.isSuccessful }
            }.getOrDefault(false)
        }
}
