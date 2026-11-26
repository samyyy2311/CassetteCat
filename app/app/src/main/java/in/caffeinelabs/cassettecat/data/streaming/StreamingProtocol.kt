package `in`.caffeinelabs.cassettecat.data.streaming

import java.net.InetAddress

enum class StreamingProtocol { SUBSONIC, JELLYFIN }

/**
 * Completes a typed server address. One starting with http:// or https:// is kept. Without either, an address on the
 * home network gets http://, as servers there rarely have a certificate, and anything else gets https://.
 */
fun completeServerUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    val hostAndPort = trimmed.substringBefore('/')
    val host = if (hostAndPort.startsWith("[")) {
        hostAndPort.substringAfter('[').substringBefore(']')
    } else {
        hostAndPort.substringBefore(':')
    }
    return (if (isHomeNetworkHost(host)) "http://" else "https://") + trimmed
}

internal fun isHomeNetworkHost(host: String): Boolean {
    val name = host.lowercase()
    if (name == "localhost" || listOf(".local", ".lan", ".home.arpa", ".internal", ".localdomain").any(name::endsWith)) {
        return true
    }
    val ipv4 = name.split('.').map { it.toIntOrNull() }
    if (ipv4.size == 4 && ipv4.all { it != null && it in 0..255 }) {
        val (first, second) = ipv4.map { it!! }
        // 100.64.0.0/10 is shared address space, used by Tailscale for devices on a private network.
        return first == 10 || first == 127 || (first == 172 && second in 16..31) || (first == 192 && second == 168) ||
            (first == 169 && second == 254) || (first == 100 && second in 64..127)
    }
    if (':' in name) {
        // An IPv6 literal is parsed without a lookup, which also covers expanded forms such as 0:0:0:0:0:0:0:1.
        val address = runCatching { InetAddress.getByName(name) }.getOrNull() ?: return false
        return address.isLoopbackAddress || address.isLinkLocalAddress || (address.address[0].toInt() and 0xfe) == 0xfc
    }
    // A single word, such as a server's own name, only resolves on the local network.
    return '.' !in name
}
