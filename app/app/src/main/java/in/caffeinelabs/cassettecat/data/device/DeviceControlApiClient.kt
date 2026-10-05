package `in`.caffeinelabs.cassettecat.data.device

import android.net.Network
import android.os.Build
import `in`.caffeinelabs.cassettecat.data.stats.Listen
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest

@Serializable
data class DevicePlaybackStatus(
    val isPlaying: Boolean,
    val trackTitle: String,
    val trackArtist: String,
    val positionMs: Long,
    val durationMs: Long,
    val volumePercent: Int,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
    // Only the desktop app reports artwork and hand-off requests.
    val artworkKey: String? = null,
    val handoffRequested: Boolean = false,
    val deviceName: String? = null
)

@Serializable
data class DesktopQueueTrack(
    val index: Int,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkKey: String? = null
)

@Serializable
private data class DesktopQueue(val tracks: List<DesktopQueueTrack>)

@Serializable
private data class QueueTrackRequest(val index: Int)

@Serializable
private data class QueueMoveRequest(val from: Int, val to: Int)

@Serializable
data class HandoffTrack(val title: String, val artist: String)

@Serializable
data class PhoneCheckIn(val title: String, val artist: String, val isPlaying: Boolean)

@Serializable
data class PhoneCheckInReply(
    val commands: List<String> = emptyList(),
    val playNext: List<HandoffTrack> = emptyList(),
    val likesRevision: Int? = null
)

@Serializable
data class DesktopLikes(val library: List<String>, val liked: List<String>, val revision: Int)

@Serializable
private data class LikesChange(val like: List<String>, val unlike: List<String>)

@Serializable
private data class DesktopListens(val listens: List<Listen>)

@Serializable
private data class PairingRequest(val name: String)

@Serializable
private data class PairingTicket(val id: String)

@Serializable
data class PairingStatus(val status: String, val code: String? = null)

@Serializable
data class DesktopPlaylist(val name: String, val tracks: List<HandoffTrack>)

@Serializable
private data class DesktopPlaylists(val playlists: List<DesktopPlaylist>)

@Serializable
data class PlaylistCopyResult(val matched: Int, val total: Int)

@Serializable
private data class HandoffRequest(val tracks: List<HandoffTrack>, val index: Int, val positionMs: Long, val playing: Boolean)

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
private data class OkResponse(val ok: Boolean)

/**
 * [onCodeRejected] hears of pairing codes the desktop app refused, so a phone left with an old one stops using it.
 * [onUnreachable] hears when the desktop app did not answer at all, so the phone can look for it again.
 */
class DeviceControlApiClient(
    private val onCodeRejected: ((String) -> Unit)? = null,
    private val onUnreachable: (() -> Unit)? = null
) {
    private fun client(network: Network?): OkHttpClient {
        val base = deviceHttpClient(network)
        if (onCodeRejected == null && onUnreachable == null) return base
        return base.newBuilder().addInterceptor { chain ->
            val response = try {
                chain.proceed(chain.request())
            } catch (e: IOException) {
                onUnreachable?.invoke()
                throw e
            }
            response.also {
                val code = chain.request().header("Authorization")?.removePrefix("Bearer ")
                if (it.code == 401 && code != null) onCodeRejected?.invoke(code)
            }
        }.build()
    }

    /** Whether the desktop app accepts [token]; null when it did not answer or is refusing attempts for now. */
    suspend fun requestPairing(host: String, port: Int): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("http://$host:$port/api/pair-request")
                    .post(sharedJson.encodeToString(PairingRequest.serializer(), PairingRequest(deviceName)).toRequestBody("application/json".toMediaType()))
                    .build()
                deviceHttpClient(null).newCall(request).execute().use {
                    if (it.isSuccessful) sharedJson.decodeFromString<PairingTicket>(it.body.string()).id else null
                }
            }.getOrNull()
        }

    suspend fun pairingStatus(host: String, port: Int, id: String): PairingStatus? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/pair-request?id=$id").build()
                deviceHttpClient(null).newCall(request).execute().use {
                    if (it.isSuccessful) sharedJson.decodeFromString<PairingStatus>(it.body.string()) else null
                }
            }.getOrNull()
        }

    suspend fun acceptsPairingCode(host: String, port: Int, token: String): Boolean? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/playback").withPairingCode(token).build()
                deviceHttpClient(null).newCall(request).execute().use {
                    when {
                        it.isSuccessful -> true
                        it.code == 401 -> false
                        else -> null
                    }
                }
            }.getOrNull()
        }

    private fun <T> postJson(host: String, port: Int, path: String, body: T, serializer: kotlinx.serialization.KSerializer<T>, network: Network?, token: String? = null): Boolean {
        val request = Request.Builder()
            .url("http://$host:$port$path")
            .post(sharedJson.encodeToString(serializer, body).toRequestBody("application/json".toMediaType()))
            .withPairingCode(token)
            .build()
        val response = client(network).newCall(request).execute()
        return response.use { it.isSuccessful }
    }

    suspend fun getPlaybackStatus(host: String, port: Int = 80, network: Network? = null, token: String? = null): DevicePlaybackStatus? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/playback").withPairingCode(token).build()
                val response = client(network).newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) return@runCatching null
                    sharedJson.decodeFromString<DevicePlaybackStatus>(it.body.string())
                }
            }.getOrNull()
        }

    suspend fun sendPlaybackAction(host: String, port: Int = 80, action: String, network: Network? = null, token: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/playback", PlaybackActionRequest(action), PlaybackActionRequest.serializer(), network, token) }
                .getOrDefault(false)
        }

    suspend fun getQueue(host: String, port: Int, token: String): List<DesktopQueueTrack>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/queue").withPairingCode(token).build()
                client(null).newCall(request).execute().use {
                    if (it.isSuccessful) sharedJson.decodeFromString<DesktopQueue>(it.body.string()).tracks else null
                }
            }.getOrNull()
        }

    suspend fun playQueueTrack(host: String, port: Int, index: Int, token: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/queue", QueueTrackRequest(index), QueueTrackRequest.serializer(), null, token) }
                .getOrDefault(false)
        }

    /** Tells the desktop what this phone is playing; returns the commands and songs it queued for the phone. */
    suspend fun checkIn(host: String, port: Int, token: String, state: PhoneCheckIn): PhoneCheckInReply =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("http://$host:$port/api/phone-state")
                    .post(sharedJson.encodeToString(PhoneCheckIn.serializer(), state).toRequestBody("application/json".toMediaType()))
                    .withPairingCode(token)
                    .build()
                client(null).newCall(request).execute().use {
                    if (it.isSuccessful) sharedJson.decodeFromString<PhoneCheckInReply>(it.body.string()) else PhoneCheckInReply()
                }
            }.getOrDefault(PhoneCheckInReply())
        }

    suspend fun handOff(host: String, port: Int, token: String, tracks: List<HandoffTrack>, positionMs: Long, playing: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val request = HandoffRequest(tracks, index = 0, positionMs = positionMs, playing = playing)
            runCatching { postJson(host, port, "/api/handoff", request, HandoffRequest.serializer(), null, token) }
                .getOrDefault(false)
        }

    suspend fun playNextOnDesktop(host: String, port: Int, token: String, track: HandoffTrack): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/queue/next", track, HandoffTrack.serializer(), null, token) }
                .getOrDefault(false)
        }

    suspend fun getLikes(host: String, port: Int, token: String): DesktopLikes? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/likes").withPairingCode(token).build()
                client(null).newCall(request).execute().use {
                    if (it.isSuccessful) sharedJson.decodeFromString<DesktopLikes>(it.body.string()) else null
                }
            }.getOrNull()
        }

    suspend fun changeLikes(host: String, port: Int, token: String, like: Set<String>, unlike: Set<String>): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/likes", LikesChange(like.toList(), unlike.toList()), LikesChange.serializer(), null, token) }
                .getOrDefault(false)
        }

    suspend fun sendListens(host: String, port: Int, token: String, listens: List<Listen>): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/listens", DesktopListens(listens), DesktopListens.serializer(), null, token) }
                .getOrDefault(false)
        }

    suspend fun getListens(host: String, port: Int, token: String, since: Long): List<Listen>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/listens?since=$since").withPairingCode(token).build()
                client(null).newCall(request).execute().use {
                    if (it.isSuccessful) sharedJson.decodeFromString<DesktopListens>(it.body.string()).listens else null
                }
            }.getOrNull()
        }

    suspend fun getPlaylists(host: String, port: Int, token: String): List<DesktopPlaylist>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/playlists").withPairingCode(token).build()
                client(null).newCall(request).execute().use {
                    if (it.isSuccessful) sharedJson.decodeFromString<DesktopPlaylists>(it.body.string()).playlists else null
                }
            }.getOrNull()
        }

    suspend fun sendPlaylist(host: String, port: Int, token: String, playlist: DesktopPlaylist): PlaylistCopyResult? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("http://$host:$port/api/playlists")
                    .post(sharedJson.encodeToString(DesktopPlaylist.serializer(), playlist).toRequestBody("application/json".toMediaType()))
                    .withPairingCode(token)
                    .build()
                client(null).newCall(request).execute().use {
                    if (it.isSuccessful) sharedJson.decodeFromString<PlaylistCopyResult>(it.body.string()) else null
                }
            }.getOrNull()
        }

    suspend fun uploadBackup(host: String, port: Int, token: String, backupJson: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("http://$host:$port/api/backup")
                    .post(backupJson.toRequestBody("application/json".toMediaType()))
                    .withPairingCode(token)
                    .build()
                client(null).newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }

    suspend fun downloadBackup(host: String, port: Int, token: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("http://$host:$port/api/backup").withPairingCode(token).build()
                client(null).newCall(request).execute().use { if (it.isSuccessful) it.body.string() else null }
            }.getOrNull()
        }

    suspend fun moveQueueTrack(host: String, port: Int, from: Int, to: Int, token: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/queue/move", QueueMoveRequest(from, to), QueueMoveRequest.serializer(), null, token) }
                .getOrDefault(false)
        }

    suspend fun removeQueueTrack(host: String, port: Int, index: Int, token: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/queue/remove", QueueTrackRequest(index), QueueTrackRequest.serializer(), null, token) }
                .getOrDefault(false)
        }

    suspend fun setVolume(host: String, port: Int = 80, percent: Int, network: Network? = null, token: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/volume", VolumeRequest(percent), VolumeRequest.serializer(), network, token) }
                .getOrDefault(false)
        }

    suspend fun seek(host: String, port: Int = 80, positionMs: Long, network: Network? = null, token: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { postJson(host, port, "/api/seek", SeekRequest(positionMs), SeekRequest.serializer(), network, token) }
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
        val response = client(network).newCall(request).execute()
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
                val response = client(network).newCall(request).execute()
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
                val response = client(network).newCall(request).execute()
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
                    .addFormDataPart("file", firmwareFile.name, firmwareFile.asRequestBody("application/octet-stream".toMediaType()))
                    .build()
                val request = Request.Builder()
                    .url("http://$host:$port/api/ota")
                    .header("X-Firmware-Sha256", sha256Hex(firmwareFile))
                    .post(body)
                    .build()
                val response = deviceUploadHttpClient(network).newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) return@runCatching false
                    sharedJson.decodeFromString<OkResponse>(it.body.string()).ok
                }
            }.getOrDefault(false)
        }
}

// The hardware player has no pairing code; the desktop app requires one.
// Headers must be ASCII, so the model name shown on the desktop is reduced to it.
private val deviceName = Build.MODEL.filter { it in ' '..'~' }.ifBlank { "Android phone" }

private fun Request.Builder.withPairingCode(token: String?): Request.Builder =
    if (token == null) this else header("Authorization", "Bearer $token").header("X-Device-Name", deviceName)

private fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
