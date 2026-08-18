package `in`.caffeinelabs.cassettecat.data.device

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class WifiProvisionRequest(
    val ssid: String,
    val passphrase: String
)

class CompanionApiClient {
    suspend fun getStatus(host: String, port: Int = 80): CompanionStatus? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("http://$host:$port/api/status")
                .header("User-Agent", "CassetteCat/0.1.0")
                .build()

            val response = sharedHttpClient.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) return@runCatching null
                val body = it.body.string()
                json.decodeFromString<CompanionStatus>(body)
            }
        }.getOrNull()
    }

    suspend fun provisionWifi(host: String, port: Int = 80, ssid: String, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(WifiProvisionRequest.serializer(), WifiProvisionRequest(ssid, passphrase))
            val request = Request.Builder()
                .url("http://$host:$port/api/wifi")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = sharedHttpClient.newCall(request).execute()
            response.use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
