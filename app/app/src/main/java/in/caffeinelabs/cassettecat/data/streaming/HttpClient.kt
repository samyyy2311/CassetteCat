package `in`.caffeinelabs.cassettecat.data.streaming

import `in`.caffeinelabs.cassettecat.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Follows the blackout setting. While it's on, nothing leaves the home network, whichever part
// of the app asks, so a feature that forgets to check the setting still can't reach the internet.
@Volatile
var blackoutActive = false

fun checkBlackout(host: String) {
    if (blackoutActive && !isHomeNetworkHost(host)) throw IOException("Blackout mode is on")
}

// In blackout a name outside the home network isn't even looked up, as the lookup itself goes out. A name that
// looks local can still lead to the internet, so only home network addresses are used.
private object BlackoutDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        checkBlackout(hostname)
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (!blackoutActive) return addresses
        return addresses.filter { isHomeNetworkHost(it.hostAddress.orEmpty()) }
            .ifEmpty { throw UnknownHostException("Blackout mode is on") }
    }
}

val sharedHttpClient: OkHttpClient = OkHttpClient.Builder()
    .sslSocketFactory(tofuSslSocketFactory, tofuTrustManager)
    .hostnameVerifier(pinnedHostnameVerifier)
    .connectionPool(ConnectionPool(5, 1, TimeUnit.MINUTES))
    // Hard ceiling on total duration; per-read timeouts reset when servers trickle bytes.
    .callTimeout(45, TimeUnit.SECONDS)
    .dns(BlackoutDns)
    // A network interceptor runs for every connection, so a redirect is checked as well.
    .addNetworkInterceptor { chain ->
        checkBlackout(chain.request().url.host)
        chain.proceed(chain.request())
    }
    // Requests for your server go to whichever of its addresses answers where you are.
    .addInterceptor(ServerAddresses.Rewriter)
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "CassetteCat/${BuildConfig.VERSION_NAME} (https://github.com/samyyy2311/CassetteCat)")
            .build()
        chain.proceed(request)
    }
    .build()

val sharedJson: Json = Json { ignoreUnknownKeys = true }

suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation { cancel() }
}
