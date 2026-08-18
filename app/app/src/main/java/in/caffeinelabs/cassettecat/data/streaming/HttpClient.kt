package `in`.caffeinelabs.cassettecat.data.streaming

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Shared instance for both API clients, no reason for each to hold its own connection pool.
val sharedHttpClient: OkHttpClient = OkHttpClient()

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
