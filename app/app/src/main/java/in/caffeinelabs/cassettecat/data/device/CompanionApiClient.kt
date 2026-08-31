package `in`.caffeinelabs.cassettecat.data.device

import android.net.Network
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class WifiProvisionRequest(
    val ssid: String,
    val passphrase: String
)

class CompanionApiClient {
    suspend fun getStatus(host: String, port: Int = 80, network: Network? = null): CompanionStatus? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("http://$host:$port/api/status")
                .build()

            val response = deviceHttpClient(network).newCall(request).execute()
            response.use {
                if (!it.isSuccessful) return@runCatching null
                val body = it.body.string()
                sharedJson.decodeFromString<CompanionStatus>(body)
            }
        }.getOrNull()
    }

    suspend fun provisionWifi(host: String, port: Int = 80, ssid: String, passphrase: String, network: Network? = null): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val payload = sharedJson.encodeToString(WifiProvisionRequest.serializer(), WifiProvisionRequest(ssid, passphrase))
            val request = Request.Builder()
                .url("http://$host:$port/api/wifi")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = deviceHttpClient(network).newCall(request).execute()
            response.use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
