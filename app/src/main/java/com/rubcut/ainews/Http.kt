package com.rubcut.ainews

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/** Tiny HTTP helper shared by every backend client. */
internal object Http {

    class ApiException(message: String) : Exception(message)

    fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String? = null,
        readTimeoutMs: Int = 120_000
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
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
        } finally {
            connection.disconnect()
        }
    }

    /** Joins a base URL and a path without doubling or dropping the slash. */
    fun join(baseUrl: String, path: String): String =
        baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
