package com.rubcut.ainews

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin Gemini REST client: asks the model for news on a topic and gets back
 * a JSON array of stories (short headline, full headline, body).
 *
 * No SDK is used on purpose — one HTTPS call keeps the plugin tiny.
 */
object GeminiClient {

    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
    private const val ENDPOINT = "$BASE/models/%s:generateContent"
    private const val MODELS_ENDPOINT = "$BASE/models?pageSize=200"

    /**
     * Asks the API which models this key may use, keeping only those that can
     * actually generate content. Doubles as an API key test.
     */
    suspend fun listModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(MODELS_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("x-goog-api-key", apiKey)
            }
            try {
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                if (code !in 200..299) error(extractApiError(text) ?: "HTTP $code")

                val models = JSONObject(text).optJSONArray("models") ?: JSONArray()
                (0 until models.length()).mapNotNull { i ->
                    val model = models.getJSONObject(i)
                    val methods = model.optJSONArray("supportedGenerationMethods")
                    val supportsGenerate = (0 until (methods?.length() ?: 0))
                        .any { methods!!.getString(it) == "generateContent" }
                    // Names come back as "models/gemini-2.5-flash".
                    model.optString("name").removePrefix("models/")
                        .takeIf { supportsGenerate && it.isNotBlank() }
                }.sorted()
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun generate(
        apiKey: String,
        model: String,
        topic: String,
        count: Int,
        language: String,
        length: StoryLength
    ): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(String.format(ENDPOINT, model))
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }
            try {
                connection.outputStream.use {
                    it.write(
                        requestBody(topic, count, language, length).toString().toByteArray()
                    )
                }
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                if (code !in 200..299) {
                    error(extractApiError(text) ?: "HTTP $code")
                }
                parseStories(text, topic, model)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun requestBody(
        topic: String,
        count: Int,
        language: String,
        length: StoryLength
    ) = JSONObject().apply {
        val prompt = """
            You are a news editor. Write $count distinct news articles about: "$topic".
            Base them on what you know; keep them plausible, factual in tone and self-contained.
            Language of the output: $language.

            For every item return:
            - "short": a very short headline for a home screen widget, max ${Constants.SHORT_TITLE_MAX_CHARS} characters, no trailing period.
            - "title": the full headline, max 120 characters.
            - "body": the article itself — ${length.instruction}.

            Format "body" with GitHub flavoured Markdown and use it generously:
            **bold** for key facts and names, *italics* for emphasis, `##` and `###`
            section headings, "- " bulleted lists, "1. " numbered lists, "> " for
            quotes from people, --- for a separator and [text](https://url) links.
            Separate paragraphs with a blank line. Do not wrap the whole body in a
            code block and do not repeat the headline as the first line.
        """.trimIndent()

        put("contents", JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        }))
        put("generationConfig", JSONObject().apply {
            put("temperature", 0.9)
            put("maxOutputTokens", length.outputTokens)
            put("responseMimeType", "application/json")
            // Ask for structured output so no fragile text parsing is needed.
            put("responseSchema", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("short", JSONObject().put("type", "STRING"))
                        put("title", JSONObject().put("type", "STRING"))
                        put("body", JSONObject().put("type", "STRING"))
                    })
                    put("required", JSONArray().put("short").put("title").put("body"))
                })
            })
        })
    }

    private fun parseStories(response: String, topic: String, model: String): List<NewsItem> {
        val root = JSONObject(response)
        val candidates = root.optJSONArray("candidates")
            ?: error(extractApiError(response) ?: "Empty response")
        if (candidates.length() == 0) error("Model returned no candidates")
        val parts = candidates.getJSONObject(0)
            .optJSONObject("content")?.optJSONArray("parts")
            ?: error("Model returned no content")
        val text = buildString {
            for (i in 0 until parts.length()) {
                append(parts.getJSONObject(i).optString("text"))
            }
        }.trim().removeSurrounding("```json", "```").trim().removeSurrounding("```").trim()

        val array = JSONArray(text)
        val now = System.currentTimeMillis()
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val title = o.optString("title").ifBlank { o.optString("short") }
            val short = o.optString("short").ifBlank { title }
            NewsItem(
                id = "${now}_$i",
                shortTitle = trimShort(short),
                title = title,
                body = o.optString("body"),
                source = "$topic · $model",
                url = null,
                // Keep the original order: the first story is the freshest.
                timestamp = now - i
            )
        }.filter { it.title.isNotBlank() }
    }

    private fun trimShort(raw: String): String {
        val clean = raw.trim().trimEnd('.')
        if (clean.length <= Constants.SHORT_TITLE_MAX_CHARS) return clean
        val cut = clean.take(Constants.SHORT_TITLE_MAX_CHARS)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > 20) cut.take(lastSpace) else cut).trimEnd(',', ';', ':') + "…"
    }

    private fun extractApiError(raw: String): String? = runCatching {
        JSONObject(raw).getJSONObject("error").getString("message")
    }.getOrNull()
}
