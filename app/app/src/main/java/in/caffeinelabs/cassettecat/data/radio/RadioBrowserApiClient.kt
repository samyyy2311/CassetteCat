package `in`.caffeinelabs.cassettecat.data.radio

import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.data.streaming.sharedJson
import java.net.InetAddress
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request

private const val DISCOVERY_HOST = "all.api.radio-browser.info"

@Serializable
private data class RadioBrowserStationResponse(
    val stationuuid: String,
    val name: String,
    val url_resolved: String,
    val favicon: String? = null,
    val tags: String = "",
    val country: String = "",
    val bitrate: Int = 0
)

@Serializable
private data class RadioBrowserNamedCountResponse(val name: String, val stationcount: Int = 0)

// The docs explicitly say never hit a single server directly: DNS-resolve the discovery
// host to the current mirror pool, shuffle, and fall back to the next mirror on failure.
private object RadioBrowserServers {
    @Volatile private var cached: List<String> = emptyList()
    private val mutex = Mutex()

    suspend fun list(): List<String> {
        cached.takeIf { it.isNotEmpty() }?.let { return it }
        return mutex.withLock {
            cached.takeIf { it.isNotEmpty() }?.let { return@withLock it }
            val discovered = runCatching {
                InetAddress.getAllByName(DISCOVERY_HOST)
                    .map { it.canonicalHostName }
                    .filter { it.isNotBlank() && it != DISCOVERY_HOST }
                    .distinct()
                    .shuffled()
            }.getOrDefault(emptyList())
            val servers = discovered.ifEmpty { listOf(DISCOVERY_HOST) }
            cached = servers
            servers
        }
    }
}

class RadioBrowserApiClient {
    suspend fun search(
        query: String = "",
        country: String? = null,
        state: String? = null,
        language: String? = null,
        tag: String? = null,
        sort: RadioSortOrder = RadioSortOrder.POPULARITY,
        reverse: Boolean = true,
        limit: Int = 100
    ): List<RadioStation>? {
        val params = buildString {
            append("order=${sort.apiValue}&reverse=$reverse&lastcheckok=1")
            if (query.isNotBlank()) append("&name=${query.urlEncode()}")
            if (!country.isNullOrBlank()) append("&country=${country.urlEncode()}")
            if (!state.isNullOrBlank()) append("&state=${state.urlEncode()}")
            if (!language.isNullOrBlank()) append("&language=${language.urlEncode()}")
            if (!tag.isNullOrBlank()) append("&tag=${tag.urlEncode()}")
            append("&limit=$limit")
        }
        return get("/search?$params")
    }

    suspend fun topStations(
        country: String? = null,
        state: String? = null,
        language: String? = null,
        tag: String? = null,
        sort: RadioSortOrder = RadioSortOrder.POPULARITY,
        reverse: Boolean = true,
        limit: Int = 100
    ): List<RadioStation>? =
        search(country = country, state = state, language = language, tag = tag, sort = sort, reverse = reverse, limit = limit)

    suspend fun countries(): List<String> = getNames("/countries")

    suspend fun languages(): List<String> = getNames("/languages")

    suspend fun tags(): List<String> = getNames("/tags")

    suspend fun states(country: String? = null): List<String> =
        getNames(if (country.isNullOrBlank()) "/states" else "/states/${country.urlEncode()}")

    // Radio Browser asks clients to ping this on play so their popularity/vote ranking
    // stays meaningful; best-effort, failure is silently ignored.
    suspend fun trackClick(uuid: String) {
        withContext(Dispatchers.IO) {
            tryServers(path = "/json/url/${uuid.urlEncode()}", parse = { it })
        }
    }

    private suspend fun getNames(path: String): List<String> = withContext(Dispatchers.IO) {
        tryServers(
            path = "/json$path",
            parse = { body ->
                sharedJson.decodeFromString<List<RadioBrowserNamedCountResponse>>(body)
                    .filter { it.stationcount > 0 && it.name.isNotBlank() }
                    .map { it.name }
                    .sorted()
            }
        ) ?: emptyList()
    }

    private suspend fun get(path: String): List<RadioStation>? = withContext(Dispatchers.IO) {
        tryServers(
            path = "/json/stations$path",
            parse = { body ->
                sharedJson.decodeFromString<List<RadioBrowserStationResponse>>(body)
                    .filter { it.url_resolved.isNotBlank() }
                    .map {
                        RadioStation(
                            uuid = it.stationuuid,
                            name = it.name,
                            streamUrl = it.url_resolved,
                            favicon = it.favicon,
                            tags = it.tags,
                            country = it.country,
                            bitrate = it.bitrate
                        )
                    }
            }
        )
    }

    private suspend fun <T> tryServers(path: String, parse: (String) -> T): T? {
        for (server in RadioBrowserServers.list()) {
            val request = Request.Builder()
                .url("https://$server$path")
                .build()
            val result = runCatching {
                val body = sharedHttpClient.newCall(request).execute().use {
                    if (!it.isSuccessful) return@runCatching null
                    it.body.string()
                }
                parse(body)
            }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
}
