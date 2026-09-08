package com.rubcut.ainews

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * Just enough HTTP/1.1 over a plain socket to reach the user's own server when
 * the platform declines to do it — the `Allow unencrypted connections` switch,
 * see [Http.request].
 *
 * TLS is deliberately not handled here: `https://` keeps going through
 * `HttpURLConnection`, so certificates, trust anchors and the platform's own
 * checks stay in charge of everything that leaves the device.
 *
 * Small on purpose, because a bigger surface would be a bigger liability: no
 * keep-alive (`Connection: close`), no compression (`Accept-Encoding:
 * identity`), no cookies, and redirects are followed for `GET` only — which is
 * what `HttpURLConnection` did with `instanceFollowRedirects` anyway.
 */
internal object PlainHttp {

    /** Response bodies above this size are refused; a news digest is a few KB. */
    private const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024

    private const val MAX_REDIRECTS = 5

    /** Status and body of the response that ended the exchange. */
    class Response(val status: Int, val body: String)

    private class Raw(val status: Int, val body: ByteArray, val location: String?)

    fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        connectTimeoutMs: Int = 20_000,
        readTimeoutMs: Int = 120_000
    ): Response {
        var target = URL(url)
        var followed = 0
        while (true) {
            val response = exchange(target, method, headers, body, connectTimeoutMs, readTimeoutMs)
            val location = response.location
            if (response.status in 300..399 && location != null && method == "GET" && followed < MAX_REDIRECTS) {
                target = URL(target, location)
                if (!target.protocol.equals("http", ignoreCase = true)) {
                    throw IOException("Redirected away from plain HTTP, which this path does not speak")
                }
                followed++
                continue
            }
            return Response(response.status, String(response.body, Charsets.UTF_8))
        }
    }

    private fun exchange(
        target: URL,
        method: String,
        headers: Map<String, String>,
        body: String?,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): Raw {
        val host = target.host?.takeIf { it.isNotBlank() }
            ?: throw IOException("No host in $target")
        val port = if (target.port != -1) target.port else 80
        val payload = body?.toByteArray(Charsets.UTF_8)
        val request = StringBuilder().apply {
            append(method).append(' ').append(requestPath(target)).append(" HTTP/1.1\r\n")
            // Servers behind a vhost need the port back when it is not the default one.
            if (target.port != -1) append("Host: ").append(host).append(':').append(port)
            else append("Host: ").append(host)
            append("\r\n")
            append("Accept: application/json\r\n")
            append("Accept-Encoding: identity\r\n")
            append("Connection: close\r\n")
            headers.forEach { (name, value) -> append(name).append(": ").append(value).append("\r\n") }
            if (payload != null) {
                append("Content-Type: application/json\r\n")
                append("Content-Length: ").append(payload.size).append("\r\n")
            }
            append("\r\n")
        }.toString()

        val socket = Socket()
        return try {
            socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
            socket.soTimeout = readTimeoutMs
            socket.getOutputStream().use { output ->
                output.write(request.toByteArray(Charsets.UTF_8))
                if (payload != null) output.write(payload)
                output.flush()
            }
            socket.getInputStream().readResponse()
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun requestPath(target: URL): String =
        target.file?.takeIf { it.startsWith("/") && it.isNotBlank() } ?: "/"

    private fun InputStream.readResponse(): Raw {
        val statusLine = readStatusLine() ?: throw IOException("The server closed the connection")
        val status = statusLine.split(' ').getOrNull(1)?.toIntOrNull()
            ?: throw IOException("Unexpected status line: $statusLine")
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readStatusLine() ?: break
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) {
                headers[line.substring(0, separator).trim().lowercase()] =
                    line.substring(separator + 1).trim()
            }
        }
        val body = if (headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true) {
            readChunked()
        } else {
            val length = headers["content-length"]?.toIntOrNull()
            if (length != null) readExactly(length) else readToEnd()
        }
        return Raw(status, body, headers["location"])
    }

    /** Reads one CRLF terminated line, or null at a clean end of stream. */
    private fun InputStream.readStatusLine(): String? {
        val bytes = ByteArrayOutputStream(64)
        while (true) {
            val byte = read()
            if (byte == -1) return if (bytes.size() == 0) null else bytes.utf8()
            if (byte == '\n'.code) return bytes.utf8()
            bytes.write(byte)
        }
    }

    private fun InputStream.readChunked(): ByteArray {
        val body = ByteArrayOutputStream()
        while (true) {
            val sizeLine = readStatusLine() ?: throw IOException("Truncated chunked response")
            val size = sizeLine.substringBefore(';').trim().toIntOrNull(16)
                ?: throw IOException("Malformed chunked response")
            if (size == 0) {
                // Optional trailers, then the blank line that ends the body.
                while (true) {
                    val trailer = readStatusLine() ?: break
                    if (trailer.isEmpty()) break
                }
                break
            }
            if (body.size() + size > MAX_RESPONSE_BYTES) {
                throw IOException("The response is larger than 4 MB")
            }
            val chunk = readExactly(size)
            if (chunk.size != size) throw IOException("Truncated chunked response")
            body.write(chunk, 0, chunk.size)
            readStatusLine() // the CRLF that closes a chunk
        }
        return body.toByteArray()
    }

    private fun InputStream.readExactly(count: Int): ByteArray {
        val buffer = ByteArray(count)
        var filled = 0
        while (filled < count) {
            val read = read(buffer, filled, count - filled)
            if (read < 0) break
            filled += read
        }
        return if (filled == count) buffer else buffer.copyOf(filled)
    }

    /** For the rare server that answers HTTP/1.0 style: read until it hangs up. */
    private fun InputStream.readToEnd(): ByteArray {
        val body = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (body.size() + read > MAX_RESPONSE_BYTES) {
                throw IOException("The response is larger than 4 MB")
            }
            body.write(buffer, 0, read)
        }
        return body.toByteArray()
    }

    private fun ByteArrayOutputStream.utf8(): String =
        toString("UTF-8").trimEnd('\r')
}
