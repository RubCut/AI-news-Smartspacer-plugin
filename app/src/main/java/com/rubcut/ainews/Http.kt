package com.rubcut.ainews

import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Tiny HTTP helper shared by every backend client. */
internal object Http {

    class ApiException(message: String) : Exception(message)

    /**
     * The platform refused an unencrypted request, because the host is not one of
     * the loopback entries in `res/xml/network_security_config.xml`. It is reported
     * as its own type so the UI can explain it in the app's language instead of
     * printing a framework message.
     */
    class CleartextBlocked(val host: String) :
        IOException("Cleartext HTTP traffic to $host not permitted")

    fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String? = null,
        readTimeoutMs: Int = 120_000
    ): String {
        val target = URL(url)
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

    /** Names the platform's own cleartext refusal, which arrives as a plain IOException. */
    private fun IOException.orCleartextBlocked(host: String?): IOException =
        if (host != null && message?.contains("cleartext", ignoreCase = true) == true) {
            CleartextBlocked(host)
        } else {
            this
        }
}
