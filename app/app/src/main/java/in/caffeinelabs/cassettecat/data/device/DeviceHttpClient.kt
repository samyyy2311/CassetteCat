package `in`.caffeinelabs.cassettecat.data.device

import android.net.Network
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// A local device should answer in milliseconds if it's actually there; sharedHttpClient's
// internet-scale timeouts would otherwise leave every screen spinning for ~10s+ when it's not.
fun deviceHttpClient(network: Network?): OkHttpClient {
    val builder = sharedHttpClient.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
    if (network != null) builder.socketFactory(network.socketFactory)
    return builder.build()
}
