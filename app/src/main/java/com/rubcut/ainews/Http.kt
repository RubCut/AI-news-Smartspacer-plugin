package com.rubcut.ainews

import android.security.NetworkSecurityPolicy
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Tiny HTTP helper shared by every backend client. */
internal object Http {

    class ApiException(message: String) : Exception(message)

    /**
     * The platform refused an unencrypted request, because the host is not one of
     * the loopback entries in `res/xml/network_security_config.xml`. Reported as
     * its own type so the UI can explain it in the app's language — and point at
     * the `Allow unencrypted connections` switch — instead of printing a framework
     * message. That switch routes around this by going through [PlainHttp].
     */
    class CleartextBlocked(val host: String) :
        IOException("Cleartext HTTP traffic to $host not permitted")

    fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String? = null,
        readTimeoutMs: Int = 120_000,
        allowInsecureHttp: Boolean = false
    ): String {
        val target = URL(url)
        // Plain HTTP is refused for every host but the loopback ones listed in
        // the network security config. `Allow unencrypted connections` lets the
        // user override that for a server of their own, by sending the request
        // over a socket instead; https never takes that path, so hosted
        // providers keep the platform's certificate and trust checks.
        if (allowInsecureHttp &&
            target.protocol.equals("http", ignoreCase = true) &&
            !cleartextPermittedByPlatform(target.host)
        ) {
            val response = PlainHttp.request(
                url = url,
                method = method,
                headers = headers,
                body = body,
                readTimeoutMs = readTimeoutMs
            )
            if (response.status !in 200..299) {
                throw ApiException(ApiErrors.extract(response.body) ?: "HTTP ${response.status}")
            }
            return response.body
        }
        val connection = (target.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            body?.let { payload ->
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                throw ApiException(ApiErrors.extract(text) ?: "HTTP $code")
            }
            return text
        } catch (error: IOException) {
            throw error.orCleartextBlocked(target.host)
        } finally {
            connection.disconnect()
        }
    }

    /** Joins a base URL and a path without doubling or dropping the slash. */
    fun join(baseUrl: String, path: String): String =
        baseUrl.trimEnd('/') + "/" + path.trimStart('/')

    /**
     * Whether the platform already allows plain HTTP to this host, i.e. whether it
     * is one of the loopback entries of the network security config. Asked rather
     * than hardcoded, so editing that file stays the single source of truth. Should
     * the framework refuse to answer, the old path is kept and its refusal turned
     * into [CleartextBlocked] a few lines below.
     */
    private fun cleartextPermittedByPlatform(host: String?): Boolean =
        host == null || runCatching {
            NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(host)
        }.getOrDefault(true)
}

/**
 * Turns the platform's cleartext refusal into [Http.CleartextBlocked], leaving
 * every other IO failure — timeouts, refused connections, truncated bodies —
 * exactly as it was. Both spellings in the wild are covered: `HttpURLConnection`
 * says "Cleartext HTTP traffic to host not permitted", the okhttp underneath it
 * says "CLEARTEXT communication to host not permitted by network security policy".
 *
 * Top level and internal rather than private, so those two wordings are pinned by
 * a test instead of by a guess.
 */
internal fun IOException.orCleartextBlocked(host: String?): IOException =
    if (host != null && message?.contains("cleartext", ignoreCase = true) == true) {
        Http.CleartextBlocked(host)
    } else {
        this
    }
