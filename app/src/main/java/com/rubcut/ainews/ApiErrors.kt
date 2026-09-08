package com.rubcut.ainews

import android.content.Context
import org.json.JSONObject

/** Pulls a human readable message out of the many error shapes APIs return. */
internal object ApiErrors {

    fun extract(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            when (val error = root.opt("error")) {
                is JSONObject -> error.optString("message")
                    .ifBlank { error.optString("type") }
                    .ifBlank { null }
                is String -> error.ifBlank { null }
                else -> root.optString("message").ifBlank { null }
                    ?: root.optString("detail").ifBlank { null }
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
            // Non-JSON bodies (HTML error pages, proxies) still say something.
            ?: raw.trim().take(160).takeIf { it.isNotBlank() }
    }

    /**
     * What to show for a failure that never got an answer from the provider:
     * framework errors are translated, everything else keeps its own message.
     */
    fun messageFor(context: Context, error: Throwable): String = when (error) {
        is Http.CleartextBlocked ->
            context.getString(R.string.error_cleartext_blocked, error.host)
        else -> error.message?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName
    }
}
