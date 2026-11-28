package `in`.caffeinelabs.cassettecat.data.streaming

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Which of a server's addresses to use right now. A server can have an away address beside its home one, such as a
 * domain name or a Tailscale address for when you're out. Whichever answers is used, home network addresses first,
 * and every request, stream and cover for either address goes there. Checked again whenever the network changes.
 */
object ServerAddresses {
    private class Server(val protocol: StreamingProtocol, val home: String, val away: String, val active: String)

    @Volatile
    private var servers: List<Server> = emptyList()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recheck = Channel<Unit>(Channel.CONFLATED)
    private var started = false

    /** [url] sent to the address in use, when it starts with either of a server's addresses. */
    fun rewrite(url: String): String {
        for (server in servers) {
            for (base in listOf(server.home, server.away)) {
                if (base != server.active && startsWithBase(url, base)) return server.active + url.substring(base.length)
            }
        }
        return url
    }

    fun start(context: Context) {
        synchronized(this) {
            if (started) return
            started = true
        }
        val app = context.applicationContext
        app.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    recheck.trySend(Unit)
                }

                override fun onLost(network: Network) {
                    recheck.trySend(Unit)
                }
            }
        )
        val repository = StreamingServerRepository(app)
        scope.launch {
            combine(repository.config(StreamingProtocol.SUBSONIC), repository.config(StreamingProtocol.JELLYFIN)) { _, _ -> }
                .collect { recheck.trySend(Unit) }
        }
        scope.launch {
            while (true) {
                recheck.receive()
                // A network change comes as several callbacks a moment apart, so they're checked once together.
                delay(SETTLE_MS)
                refresh(repository)
            }
        }
    }

    private suspend fun refresh(repository: StreamingServerRepository) {
        servers = StreamingProtocol.entries.mapNotNull { protocol ->
            val config = repository.config(protocol).first()
            val home = config.serverUrl.trimEnd('/')
            val away = config.awayUrl.trimEnd('/')
            if (!config.connected || home.isBlank() || away.isBlank()) return@mapNotNull null
            // Nothing answering, as on a train between networks, keeps the address already in use.
            val previous = servers.firstOrNull { it.protocol == protocol && it.home == home && it.away == away }
            val active = byHomeNetworkFirst(listOf(home, away)).firstOrNull { answers(protocol, it) }
                ?: previous?.active ?: home
            Server(protocol, home, away, active)
        }
    }

    // A home network address is tried first, as it's faster at home and doesn't answer anywhere else.
    internal fun byHomeNetworkFirst(addresses: List<String>): List<String> =
        addresses.sortedByDescending { isHomeNetworkHost(hostOf(it)) }

    private fun answers(protocol: StreamingProtocol, base: String): Boolean = runCatching {
        // Any answer means the server is there; these need no sign-in, or turn down the missing one.
        val path = if (protocol == StreamingProtocol.JELLYFIN) "/System/Info/Public" else "/rest/ping.view"
        val request = Request.Builder().url(base + path).tag(Probe::class.java, Probe).build()
        probeClient.newCall(request).execute().use { true }
    }.getOrDefault(false)

    private object Probe

    private val probeClient by lazy { sharedHttpClient.newBuilder().callTimeout(PROBE_TIMEOUT_S, TimeUnit.SECONDS).build() }

    /** Sends requests for a server to the address in use; a probe goes where it was asked to. */
    internal object Rewriter : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (request.tag(Probe::class.java) != null) return chain.proceed(request)
            val url = request.url.toString()
            val rewritten = rewrite(url)
            return chain.proceed(if (rewritten == url) request else request.newBuilder().url(rewritten).build())
        }
    }

    // The base must end where a path, query or nothing begins, so :4533 isn't taken for :45330.
    internal fun startsWithBase(url: String, base: String): Boolean =
        url.startsWith(base) && (url.length == base.length || url[base.length] in "/?#")

    private fun hostOf(url: String): String {
        val afterScheme = url.substringAfter("://")
        val hostAndPort = afterScheme.substringBefore('/')
        return if (hostAndPort.startsWith("[")) hostAndPort.substringAfter('[').substringBefore(']') else hostAndPort.substringBefore(':')
    }

    private const val SETTLE_MS = 1_000L
    private const val PROBE_TIMEOUT_S = 4L
}
