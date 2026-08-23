package `in`.caffeinelabs.cassettecat.data.streaming

import `in`.caffeinelabs.cassettecat.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val sharedHttpClient: OkHttpClient = OkHttpClient.Builder()
    .sslSocketFactory(tofuSslSocketFactory, tofuTrustManager)
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "CassetteCat/${BuildConfig.VERSION_NAME} (https://github.com/samyyy2311/CassetteCat)")
            .build()
        chain.proceed(request)
    }
    .build()
val sharedJson: Json = Json { ignoreUnknownKeys = true }

// Mirrors PlaybackRepository's awaitController() bridging idiom, giving real
// coroutine cancellation (e.g. backing out of a connect screen mid-request).
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation { cancel() }
}
