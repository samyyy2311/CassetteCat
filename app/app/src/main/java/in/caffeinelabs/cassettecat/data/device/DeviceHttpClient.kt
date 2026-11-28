package `in`.caffeinelabs.cassettecat.data.device

import android.net.Network
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// Fast LAN timeout to avoid blocking screens when local devices are unreachable.
fun deviceHttpClient(network: Network?): OkHttpClient {
    val builder = sharedHttpClient.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
    if (network != null) builder.socketFactory(network.socketFactory)
    return builder.build()
}

fun deviceUploadHttpClient(network: Network?): OkHttpClient {
    val builder = sharedHttpClient.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .callTimeout(5, TimeUnit.MINUTES)
    if (network != null) builder.socketFactory(network.socketFactory)
    return builder.build()
}
