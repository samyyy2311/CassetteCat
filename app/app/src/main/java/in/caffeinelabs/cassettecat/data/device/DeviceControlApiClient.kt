package `in`.caffeinelabs.cassettecat.data.device

import android.net.Network
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@Serializable
data class DevicePlaybackStatus(
    val isPlaying: Boolean,
    val trackTitle: String,
    val trackArtist: String,
    val positionMs: Long,
    val durationMs: Long,
    val volumePercent: Int,
    val shuffleEnabled: Boolean,
    val repeatMode: Int
)

@Serializable
data class DeviceFileEntry(val name: String, val path: String, val isDirectory: Boolean, val sizeBytes: Long)

@Serializable
private data class PlaybackActionRequest(val action: String)

@Serializable
private data class VolumeRequest(val percent: Int)

@Serializable
private data class SeekRequest(val positionMs: Long)

@Serializable
private data class DeviceNameRequest(val name: String)

@Serializable
private data class WifiModeRequest(val mode: String)

@Serializable
private data class OtaFromUrlRequest(val url: String)

@Serializable
private data class SetTimeRequest(val epochMs: Long)

@Serializable
private data class OkResponse(val ok: Boolean, val error: String? = null)

class DeviceControlApiClient {
    private fun <T> postJson(host: String, port: Int, path: String, body: T, serializer: kotlinx.serialization.KSerializer<T>, network: Network?): Boolean {
        val request = Request.Builder()
            .url("http://$host:$port$path")
            .post(sharedJson.encodeToString(serializer, body).toRequestBody("application/json".toMediaType()))
            .build()
        val response = deviceHttpClient(network).newCall(request).execute()
        return response.use { it.isSuccessful }
    }

    suspend fun getPlaybackStatus(host: String, port: Int = 80, network: Network? = null): DevicePlaybackStatus? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/playback").build()
                val response = deviceHttpClient(network).newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) return@runCatching null
                    sharedJson.decodeFromString<DevicePlaybackStatus>(it.body.string())
                }
            }.getOrNull()
        }

    suspend fun sendPlaybackAction(host: String, port: Int = 80, action: String, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/playback", PlaybackActionRequest(action), PlaybackActionRequest.serializer(), network) }
                .getOrDefault(false)
        }

    suspend fun setVolume(host: String, port: Int = 80, percent: Int, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/volume", VolumeRequest(percent), VolumeRequest.serializer(), network) }
                .getOrDefault(false)
        }

    suspend fun seek(host: String, port: Int = 80, positionMs: Long, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/seek", SeekRequest(positionMs), SeekRequest.serializer(), network) }
                .getOrDefault(false)
        }

    suspend fun renameDevice(host: String, port: Int = 80, name: String, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/device/name", DeviceNameRequest(name), DeviceNameRequest.serializer(), network) }
                .getOrDefault(false)
        }

    suspend fun setWifiMode(host: String, port: Int = 80, mode: String, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/wifi/mode", WifiModeRequest(mode), WifiModeRequest.serializer(), network) }
                .getOrDefault(false)
        }

    private fun postEmpty(host: String, port: Int, path: String, network: Network?): Boolean {
        val request = Request.Builder().url("http://$host:$port$path").post("".toRequestBody()).build()
        val response = deviceHttpClient(network).newCall(request).execute()
        return response.use { it.isSuccessful }
    }

    suspend fun factoryReset(host: String, port: Int = 80, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postEmpty(host, port, "/api/device/reset", network) }.getOrDefault(false)
        }

    suspend fun restartDevice(host: String, port: Int = 80, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postEmpty(host, port, "/api/device/restart", network) }.getOrDefault(false)
        }

    suspend fun rescanLibrary(host: String, port: Int = 80, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postEmpty(host, port, "/api/library/rescan", network) }.getOrDefault(false)
        }

    suspend fun syncDeviceTime(host: String, port: Int = 80, epochMs: Long, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/device/time", SetTimeRequest(epochMs), SetTimeRequest.serializer(), network) }
                .getOrDefault(false)
        }

    suspend fun listFiles(host: String, port: Int = 80, path: String, network: Network? = null): List<DeviceFileEntry>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "http://$host:$port/api/files".toHttpUrl().newBuilder().addQueryParameter("path", path).build()
                val request = Request.Builder().url(url).build()
                val response = deviceHttpClient(network).newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) return@runCatching null
                    sharedJson.decodeFromString<List<DeviceFileEntry>>(it.body.string())
                }
            }.getOrNull()
        }

    suspend fun deleteFile(host: String, port: Int = 80, path: String, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "http://$host:$port/api/files".toHttpUrl().newBuilder().addQueryParameter("path", path).build()
                val request = Request.Builder().url(url).delete().build()
                val response = deviceHttpClient(network).newCall(request).execute()
                response.use { it.isSuccessful }
            }.getOrDefault(false)
        }

    suspend fun updateFirmwareFromUrl(host: String, port: Int = 80, url: String, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/ota/from-url", OtaFromUrlRequest(url), OtaFromUrlRequest.serializer(), network) }
                .getOrDefault(false)
        }

    suspend fun uploadFirmware(host: String, port: Int = 80, firmwareFile: File, network: Network? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", firmwareFile.name, firmwareFile.readBytes().toRequestBody("application/octet-stream".toMediaType()))
                    .build()
                val request = Request.Builder().url("http://$host:$port/api/ota").post(body).build()
                val response = deviceHttpClient(network).newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) return@runCatching false
                    sharedJson.decodeFromString<OkResponse>(it.body.string()).ok
                }
            }.getOrDefault(false)
        }
}
