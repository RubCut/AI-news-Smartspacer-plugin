package com.rubcut.ainews

import org.json.JSONArray
import org.json.JSONObject

/** Google Gemini `generateContent` dialect. */
internal object GeminiClient {

    fun generate(
        config: AiClient.Config,
        prompt: String,
        length: StoryLength,
        source: String
    ): List<NewsItem> {
        val url = Http.join(config.baseUrl, "models/${config.model}:generateContent")
        val response = Http.request(
            url = url,
            method = "POST",
            headers = headers(config),
            body = body(prompt, length).toString()
        )
        return NewsJsonParser.parse(extractText(response), source)
    }

    fun listModels(config: AiClient.Config): List<String> {
        val response = Http.request(
            url = Http.join(config.baseUrl, "models?pageSize=200"),
            method = "GET",
            headers = headers(config),
            readTimeoutMs = 30_000
        )
        val models = JSONObject(response).optJSONArray("models") ?: JSONArray()
        val names = (0 until models.length()).mapNotNull { index ->
            val model = models.getJSONObject(index)
            val methods = model.optJSONArray("supportedGenerationMethods")
            val supported = (0 until (methods?.length() ?: 0))
                .any { methods!!.optString(it) == "generateContent" }
            // Names come back as "models/gemini-2.5-flash".
            model.optString("name").removePrefix("models/")
                .takeIf { supported && it.isNotBlank() }
        }
        return AiClient.sortModels(names)
    }

    private fun headers(config: AiClient.Config) = buildMap {
        if (config.apiKey.isNotBlank()) put("x-goog-api-key", config.apiKey)
    }

    private fun body(prompt: String, length: StoryLength) = JSONObject().apply {
        put("contents", JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        }))
        put("generationConfig", JSONObject().apply {
            put("temperature", 0.9)
            put("maxOutputTokens", length.outputTokens)
            put("responseMimeType", "application/json")
            // Structured output means no fragile text parsing.
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

    private fun extractText(response: String): String {
        val root = JSONObject(response)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            // A blocked prompt comes back with a reason instead of candidates.
            val blocked = root.optJSONObject("promptFeedback")?.optString("blockReason")
            throw Http.ApiException(
                if (!blocked.isNullOrBlank()) "Blocked by the model: $blocked"
                else "Model returned no candidates"
            )
        }
        val candidate = candidates.getJSONObject(0)
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            val finish = candidate.optString("finishReason")
            throw Http.ApiException(
                if (finish.isNotBlank()) "Model returned no content ($finish)"
                else "Model returned no content"
            )
        }
        return buildString {
            for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text"))
        }
    }
}
