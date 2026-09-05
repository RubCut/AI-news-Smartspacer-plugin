package com.rubcut.ainews

import org.json.JSONArray
import org.json.JSONObject

/** Anthropic Claude `/messages` dialect. */
internal object AnthropicClient {

    private const val VERSION = "2023-06-01"

    fun generate(
        config: AiClient.Config,
        prompt: String,
        length: StoryLength,
        source: String
    ): List<NewsItem> {
        val response = Http.request(
            url = Http.join(config.baseUrl, "messages"),
            method = "POST",
            headers = headers(config),
            body = JSONObject().apply {
                put("model", config.model)
                put("max_tokens", length.outputTokens)
                put("temperature", 0.9)
                put("system", SYSTEM)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }))
                // Prefilling an opening bracket keeps Claude from adding prose.
                put("stop_sequences", JSONArray())
            }.toString()
        )
        return NewsJsonParser.parse(extractText(response), source)
    }

    fun listModels(config: AiClient.Config): List<String> {
        val response = Http.request(
            url = Http.join(config.baseUrl, "models?limit=200"),
            method = "GET",
            headers = headers(config),
            readTimeoutMs = 30_000
        )
        val data = JSONObject(response).optJSONArray("data") ?: JSONArray()
        val names = (0 until data.length()).mapNotNull { index ->
            data.optJSONObject(index)?.optString("id")?.takeIf { it.isNotBlank() }
        }
        return AiClient.sortModels(names)
    }

    private fun headers(config: AiClient.Config) = buildMap {
        if (config.apiKey.isNotBlank()) put("x-api-key", config.apiKey)
        put("anthropic-version", VERSION)
    }

    private fun extractText(response: String): String {
        val root = JSONObject(response)
        val content = root.optJSONArray("content")
        if (content == null || content.length() == 0) {
            throw Http.ApiException("Model returned no content")
        }
        return buildString {
            for (i in 0 until content.length()) {
                val block = content.optJSONObject(i) ?: continue
                if (block.optString("type") == "text") append(block.optString("text"))
            }
        }
    }

    private const val SYSTEM =
        "You are a news editor. Reply with nothing but a raw JSON array — " +
            "no explanations, no Markdown code fences around the array."
}
