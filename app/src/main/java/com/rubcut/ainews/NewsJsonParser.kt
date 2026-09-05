package com.rubcut.ainews

import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns whatever JSON a model replied with into stories.
 *
 * Models are only mostly obedient: some wrap the array in a code fence, some
 * return an object with an "articles"/"news" key, some add prose around it.
 * All of those shapes are recovered here so a stray format never loses a batch.
 */
object NewsJsonParser {

    fun parse(raw: String, source: String): List<NewsItem> {
        val array = extractArray(raw) ?: error("Model did not return JSON")
        val now = System.currentTimeMillis()
        return (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            val title = item.optString("title").ifBlank { item.optString("headline") }
                .ifBlank { item.optString("short") }
            val short = item.optString("short").ifBlank { title }
            val body = item.optString("body").ifBlank { item.optString("content") }
            if (title.isBlank()) return@mapNotNull null
            NewsItem(
                id = "${now}_$i",
                shortTitle = trimShort(short),
                title = title.trim(),
                body = body.trim(),
                source = source,
                url = null,
                // Keep the model's order: the first story is the freshest.
                timestamp = now - i
            )
        }
    }

    private fun extractArray(raw: String): JSONArray? {
        val cleaned = stripCodeFence(raw.trim())
        runCatching { return JSONArray(cleaned) }
        runCatching {
            val obj = JSONObject(cleaned)
            KEYS.forEach { key -> obj.optJSONArray(key)?.let { return it } }
            // A single story returned as a bare object.
            if (obj.has("title") || obj.has("short")) return JSONArray().put(obj)
        }
        // Last resort: cut out the outermost [ ... ] found in the text.
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start in 0..<end) {
            runCatching { return JSONArray(cleaned.substring(start, end + 1)) }
        }
        return null
    }

    private fun stripCodeFence(text: String): String {
        if (!text.startsWith("```")) return text
        return text.removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    fun trimShort(raw: String): String {
        val clean = raw.trim().trimEnd('.').trim()
        if (clean.length <= Constants.SHORT_TITLE_MAX_CHARS) return clean
        val cut = clean.take(Constants.SHORT_TITLE_MAX_CHARS)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > 20) cut.take(lastSpace) else cut).trimEnd(',', ';', ':') + "…"
    }

    private val KEYS = listOf("articles", "news", "items", "stories", "results", "data")
}
