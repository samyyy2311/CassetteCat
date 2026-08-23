package `in`.caffeinelabs.cassettecat.data.listeningroom

import android.content.Context
import android.net.Uri
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Semaphore
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Request

private const val RELAY_CHUNK_BYTES = 64 * 1024
private const val MAX_HTTP_LINE_LENGTH = 8 * 1024
private const val SOCKET_TIMEOUT_MS = 10_000

class RoomAudioServer(
    private val context: Context,
    private val roomToken: String,
    private val queueProvider: () -> List<Song>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private val connections = Semaphore(MAX_LISTENING_ROOM_GUESTS)

    fun start(): Int {
        val server = ServerSocket(0)
        serverSocket = server
        scope.launch {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: continue
                if (!connections.tryAcquire()) {
                    runCatching { socket.close() }
                } else {
                    scope.launch {
                        try {
                            runCatching { handleRequest(socket) }
                        } finally {
                            connections.release()
                        }
                    }
                }
            }
        }
        return server.localPort
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        scope.cancel()
    }

    private fun handleRequest(socket: Socket) {
        socket.use {
            socket.soTimeout = SOCKET_TIMEOUT_MS
            val reader = socket.getInputStream().bufferedReader()
            val requestLine = reader.readLineBounded(MAX_HTTP_LINE_LENGTH) ?: return
            val query = parseQuery(requestLine)
            if (query["token"] != roomToken) {
                writeHeaders(BufferedOutputStream(socket.getOutputStream()), 403, "Forbidden", null, "text/plain")
                return
            }
            val rangeStart = readRangeStart(reader)
            val output = BufferedOutputStream(socket.getOutputStream())
            val queue = queueProvider()
            val song = resolveSong(queue, query)
            if (song == null) {
                writeHeaders(output, 404, "Not Found", contentLength = null, contentType = "text/plain")
                return
            }
            when (song.source) {
                MusicSource.Local -> relayLocalFile(output, song, rangeStart)
                else -> relayRemoteFile(output, song, rangeStart)
            }
        }
    }

    private fun parseQuery(requestLine: String): Map<String, String> {
        val path = requestLine.split(" ").getOrNull(1) ?: return emptyMap()
        val queryPart = path.substringAfter('?', "")
        if (queryPart.isEmpty()) return emptyMap()
        return queryPart.split("&").mapNotNull { pair ->
            val separatorIndex = pair.indexOf('=')
            if (separatorIndex < 0) return@mapNotNull null
            pair.substring(0, separatorIndex) to Uri.decode(pair.substring(separatorIndex + 1))
        }.toMap()
    }

    private fun resolveSong(queue: List<Song>, query: Map<String, String>): Song? {
        val title = query["title"] ?: return null
        val artist = query["artist"]
        val durationMs = query["duration"]?.toLongOrNull()
        return queue.firstOrNull { song ->
            song.title == title &&
                (artist == null || song.artist == artist) &&
                (durationMs == null || abs(song.durationMs - durationMs) <= 2_000L)
        }
    }

    private fun readRangeStart(reader: BufferedReader): Long {
        var rangeStart = 0L
        var line = reader.readLineBounded(MAX_HTTP_LINE_LENGTH)
        while (!line.isNullOrEmpty()) {
            if (line.startsWith("Range:", ignoreCase = true)) {
                rangeStart = line.substringAfter("bytes=").substringBefore('-').trim().toLongOrNull() ?: 0L
            }
            line = reader.readLineBounded(MAX_HTTP_LINE_LENGTH)
        }
        return rangeStart
    }

    private fun relayLocalFile(output: BufferedOutputStream, song: Song, rangeStart: Long) {
        val totalSize = runCatching {
            context.contentResolver.openAssetFileDescriptor(song.contentUri, "r")?.use { it.length }
        }.getOrNull()?.takeIf { it > 0 }

        if (isInvalidLocalRange(rangeStart, totalSize)) {
            writeHeaders(output, 416, "Range Not Satisfiable", 0, "text/plain", totalSize)
            return
        }

        val input = runCatching { context.contentResolver.openInputStream(song.contentUri) }.getOrNull()
        if (input == null) {
            writeHeaders(output, 404, "Not Found", contentLength = null, contentType = "text/plain")
            return
        }
        input.use {
            if (!it.skipFully(rangeStart)) {
                writeHeaders(output, 416, "Range Not Satisfiable", 0, "text/plain", totalSize)
                return
            }
            val remaining = totalSize?.let { size -> size - rangeStart }
            val partial = rangeStart > 0 && totalSize != null
            writeHeaders(
                output,
                code = if (partial) 206 else 200,
                message = if (partial) "Partial Content" else "OK",
                contentLength = remaining,
                contentType = "audio/mpeg",
                totalSize = totalSize,
                rangeStart = rangeStart
            )
            copyStream(it, output)
        }
    }

    private fun relayRemoteFile(output: BufferedOutputStream, song: Song, rangeStart: Long) {
        val request = Request.Builder()
            .url(song.contentUri.toString())
            .apply { if (rangeStart > 0) header("Range", "bytes=$rangeStart-") }
            .build()
        runCatching { sharedHttpClient.newCall(request).execute() }.getOrNull()?.use { response ->
            val body = response.body
            if (!response.isSuccessful) {
                writeHeaders(output, 502, "Bad Gateway", contentLength = null, contentType = "text/plain")
                return
            }
            val partial = response.code == 206
            writeHeaders(
                output,
                code = if (partial) 206 else 200,
                message = if (partial) "Partial Content" else "OK",
                contentLength = body.contentLength().takeIf { it >= 0 },
                contentType = response.header("Content-Type") ?: "audio/mpeg"
            )
            body.byteStream().use { copyStream(it, output) }
        } ?: writeHeaders(output, 502, "Bad Gateway", contentLength = null, contentType = "text/plain")
    }

    private fun writeHeaders(
        output: BufferedOutputStream,
        code: Int,
        message: String,
        contentLength: Long?,
        contentType: String,
        totalSize: Long? = null,
        rangeStart: Long = 0L
    ) {
        val headers = buildString {
            append("HTTP/1.1 $code $message\r\n")
            append("Content-Type: $contentType\r\n")
            append("Accept-Ranges: bytes\r\n")
            append("Connection: close\r\n")
            if (contentLength != null) append("Content-Length: $contentLength\r\n")
            if (code == 206 && totalSize != null) append("Content-Range: bytes $rangeStart-${totalSize - 1}/$totalSize\r\n")
            if (code == 416 && totalSize != null) append("Content-Range: bytes */$totalSize\r\n")
            append("\r\n")
        }
        output.write(headers.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun copyStream(input: InputStream, output: BufferedOutputStream) {
        val buffer = ByteArray(RELAY_CHUNK_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
        }
        output.flush()
    }
}

internal fun isInvalidLocalRange(rangeStart: Long, totalSize: Long?): Boolean =
    rangeStart < 0 ||
        (rangeStart > 0 && totalSize == null) ||
        (totalSize != null && rangeStart >= totalSize)

internal fun InputStream.skipFully(byteCount: Long): Boolean {
    var remaining = byteCount
    while (remaining > 0) {
        val skipped = skip(remaining)
        if (skipped > 0) {
            remaining -= skipped
        } else if (read() == -1) {
            return false
        } else {
            remaining--
        }
    }
    return true
}
