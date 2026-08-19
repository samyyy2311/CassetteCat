package `in`.caffeinelabs.cassettecat.data.listeningroom

import android.content.Context
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Request

private const val RELAY_CHUNK_BYTES = 64 * 1024

/**
 * Relays the host's currently playing track to Listening Room guests over plain HTTP, for
 * guests with no local or server copy of the song. Always serves whatever song is playing
 * right now; the request path/query is ignored server-side and exists only so the guest's
 * player opens a fresh connection whenever the current track changes. LAN-only, no auth
 * beyond already being connected to the room's own socket.
 */
class RoomAudioServer(
    private val context: Context,
    private val currentSongProvider: () -> Song?
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    fun start(): Int {
        val server = ServerSocket(0)
        serverSocket = server
        scope.launch {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: continue
                scope.launch { runCatching { handleRequest(socket) } }
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
            val rangeStart = readRangeStart(socket)
            val output = BufferedOutputStream(socket.getOutputStream())
            val song = currentSongProvider()
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

    private fun readRangeStart(socket: Socket): Long {
        val reader = socket.getInputStream().bufferedReader()
        var rangeStart = 0L
        var line = reader.readLine()
        while (!line.isNullOrEmpty()) {
            if (line.startsWith("Range:", ignoreCase = true)) {
                rangeStart = line.substringAfter("bytes=").substringBefore('-').trim().toLongOrNull() ?: 0L
            }
            line = reader.readLine()
        }
        return rangeStart
    }

    private fun relayLocalFile(output: BufferedOutputStream, song: Song, rangeStart: Long) {
        val totalSize = runCatching {
            context.contentResolver.openAssetFileDescriptor(song.contentUri, "r")?.use { it.length }
        }.getOrNull()?.takeIf { it > 0 }

        val input = runCatching { context.contentResolver.openInputStream(song.contentUri) }.getOrNull()
        if (input == null) {
            writeHeaders(output, 404, "Not Found", contentLength = null, contentType = "text/plain")
            return
        }
        input.use {
            if (rangeStart > 0) it.skip(rangeStart)
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
            if (!response.isSuccessful || body == null) {
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
