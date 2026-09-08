package com.rubcut.ainews

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownServiceException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [PlainHttp] is the entire path to a user's own `http://` server, so it is held
 * up against a real socket instead of a mock: the request line, the `Host`
 * header, the framing of a response (Content-Length, chunked, read until the peer
 * hangs up) and redirects. A mistake here means nobody reaches Ollama — and that
 * is precisely what a network security config on its own cannot prove.
 */
class PlainHttpTest {

    /** Answers with [respond], recording each request exactly as it arrived. */
    private class Server(private val respond: (request: String, hit: Int) -> String) : AutoCloseable {
        private val socket = ServerSocket(0)
        private val hits = AtomicInteger()
        val requests = CopyOnWriteArrayList<String>()
        val port: Int get() = socket.localPort
        val baseUrl: String get() = "http://127.0.0.1:$port"

        init {
            // Daemon threads, so a failing test can never hang the whole run.
            Thread {
                while (!socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull()
                    if (client != null) {
                        Thread { serve(client) }.apply { isDaemon = true }.start()
                    }
                }
            }.apply { isDaemon = true }.start()
        }

        private fun serve(client: Socket) {
            client.use { connection ->
                val request = connection.getInputStream().readRequest()
                requests += request
                val raw = respond(request, hits.incrementAndGet())
                val output = connection.getOutputStream()
                output.write(raw.toByteArray(Charsets.UTF_8))
                output.flush()
            }
        }

        override fun close() {
            runCatching { socket.close() }
        }
    }

    @Test(timeout = 20_000)
    fun `sends path and query and keeps the headers it was given`() {
        Server({ _, _ -> ok("""{"data":[]}""") }).use { server ->
            val response = PlainHttp.request(
                url = "${server.baseUrl}/v1/models?limit=200",
                method = "GET",
                headers = mapOf("Authorization" to "Bearer secret", "anthropic-version" to "2023-06-01"),
                body = null
            )
            assertEquals(200, response.status)
            assertEquals("""{"data":[]}""", response.body)

            val request = server.requests.single()
            assertTrue(request, request.startsWith("GET /v1/models?limit=200 HTTP/1.1"))
            assertTrue(request, "Host: 127.0.0.1:${server.port}" in request)
            assertTrue(request, "Authorization: Bearer secret" in request)
            assertTrue(request, "anthropic-version: 2023-06-01" in request)
            // No body was asked for, so none may be announced.
            assertTrue(request, "Content-Length" !in request)
        }
    }

    @Test(timeout = 20_000)
    fun `posts a utf8 body with an exact content length`() {
        Server({ _, _ -> ok("{}") }).use { server ->
            val body = """{"model":"llama3.2","messages":[{"content":"привет"}]}"""
            val response = PlainHttp.request(
                "${server.baseUrl}/v1/chat/completions", "POST", emptyMap(), body
            )
            assertEquals(200, response.status)

            val request = server.requests.single()
            assertTrue(request, request.startsWith("POST /v1/chat/completions HTTP/1.1"))
            assertTrue(request, "Content-Length: ${body.toByteArray(Charsets.UTF_8).size}" in request)
            assertTrue(request, request.endsWith(body))
        }
    }

    @Test(timeout = 20_000)
    fun `reassembles a chunked response`() {
        val framed = "5\r\nhello\r\n7\r\n world!\r\n0\r\n\r\n"
        Server({ _, _ ->
            "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n$framed"
        }).use { server ->
            val response = PlainHttp.request("${server.baseUrl}/v1/models", "GET", emptyMap(), null)
            assertEquals(200, response.status)
            assertEquals("hello world!", response.body)
        }
    }

    @Test(timeout = 20_000)
    fun `reads a body that has no length until the server hangs up`() {
        Server({ _, _ -> "HTTP/1.0 200 OK\r\n\r\n{\"ok\":true}" }).use { server ->
            val response = PlainHttp.request("${server.baseUrl}/v1/models", "GET", emptyMap(), null)
            assertEquals("""{"ok":true}""", response.body)
        }
    }

    @Test(timeout = 20_000)
    fun `hands an error body back untouched so the provider message survives`() {
        val error = """{"error":{"message":"invalid api key"}}"""
        Server({ _, _ ->
            "HTTP/1.1 401 Unauthorized\r\n" +
                "Content-Length: ${error.toByteArray(Charsets.UTF_8).size}\r\n\r\n$error"
        }).use { server ->
            val response = PlainHttp.request(
                "${server.baseUrl}/v1/chat/completions", "POST", emptyMap(), "{}"
            )
            assertEquals(401, response.status)
            assertEquals(error, response.body)
            // Which is what lets the settings screen show a reason, not a number.
            assertEquals("invalid api key", ApiErrors.extract(error))
        }
    }

    @Test(timeout = 20_000)
    fun `follows a redirect when reading, but never on a post`() {
        Server({ _, hit ->
            if (hit == 1) {
                "HTTP/1.1 302 Found\r\nLocation: /v2/models\r\nContent-Length: 0\r\n\r\n"
            } else {
                ok("""{"data":[]}""")
            }
        }).use { server ->
            val get = PlainHttp.request("${server.baseUrl}/v1/models", "GET", emptyMap(), null)
            assertEquals(200, get.status)
            assertEquals(2, server.requests.size)
            assertTrue(server.requests[1], server.requests[1].startsWith("GET /v2/models HTTP/1.1"))

            val post = PlainHttp.request("${server.baseUrl}/v1/chat/completions", "POST", emptyMap(), "{}")
            // The bounce only came first, so a POST is passed through as it is.
            assertEquals(200, post.status)
            assertEquals(3, server.requests.size)
        }
    }

    @Test(timeout = 20_000)
    fun `names the port it connected to`() {
        Server({ _, _ -> ok("{}") }).use { server ->
            PlainHttp.request("${server.baseUrl}/v1/models", "GET", emptyMap(), null)
            assertTrue("Host: 127.0.0.1:${server.port}" in server.requests.single())
        }
    }

    @Test(timeout = 20_000)
    fun `recognises both wordings of the platform refusal`() {
        val libcore = IOException("Cleartext HTTP traffic to 192.168.1.10 not permitted")
        val okhttp = UnknownServiceException(
            "CLEARTEXT communication to 192.168.1.10 not permitted by network security policy"
        )
        for (error in listOf(libcore, okhttp)) {
            val blocked = error.orCleartextBlocked("192.168.1.10")
            assertTrue("$error", blocked is Http.CleartextBlocked)
            assertEquals("192.168.1.10", (blocked as Http.CleartextBlocked).host)
        }
    }

    @Test(timeout = 20_000)
    fun `leaves every other io failure alone`() {
        val timeout = SocketTimeoutException("Read timed out")
        assertSame(timeout, timeout.orCleartextBlocked("127.0.0.1"))
        // Without a host there is nothing to name, so the original stays.
        val refused = IOException("Connection refused")
        assertSame(refused, refused.orCleartextBlocked(null))
    }

    private fun ok(body: String): String =
        "HTTP/1.1 200 OK\r\nContent-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n" +
            "Content-Type: application/json\r\n\r\n$body"
}

/** Reads one request — headers plus body — the way a server has to. */
private fun InputStream.readRequest(): String = buildString {
    var length = 0
    while (true) {
        val line = readHeaderLine() ?: break
        if (line.isEmpty()) break
        if (line.lowercase().startsWith("content-length:")) {
            length = line.substringAfter(':').trim().toIntOrNull() ?: 0
        }
        append(line).append('\n')
    }
    if (length > 0) append(readAtLeast(length).toString(Charsets.UTF_8))
}

private fun InputStream.readHeaderLine(): String? {
    val bytes = ByteArrayOutputStream(64)
    while (true) {
        val byte = read()
        if (byte == -1) return if (bytes.size() == 0) null else bytes.utf8()
        if (byte == '\n'.code) return bytes.utf8()
        bytes.write(byte)
    }
}

private fun InputStream.readAtLeast(count: Int): ByteArray {
    val buffer = ByteArray(count)
    var filled = 0
    while (filled < count) {
        val read = read(buffer, filled, count - filled)
        if (read < 0) break
        filled += read
    }
    return buffer.copyOf(filled)
}

private fun ByteArrayOutputStream.utf8(): String = toString("UTF-8").trimEnd('\r')
