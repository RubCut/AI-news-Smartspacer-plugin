package com.rubcut.ainews

import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI `/chat/completions` dialect — also spoken by DeepSeek, OpenRouter,
 * Groq, Mistral, xAI, Qwen, Together, Perplexity, Cerebras, Ollama and most
 * self-hosted gateways.
 */
internal object OpenAiClient {

    fun generate(
        config: AiClient.Config,
        prompt: String,
        length: StoryLength,
        source: String
    ): List<NewsItem> {
        val response = try {
            post(config, prompt, length, jsonMode = true)
        } catch (error: Http.ApiException) {
            // Not every gateway supports response_format; retry without it.
            if (looksLikeJsonModeRejection(error.message)) {
                post(config, prompt, length, jsonMode = false)
            } else {
                throw error
            }
        }
        return NewsJsonParser.parse(extractText(response), source)
    }

    fun listModels(config: AiClient.Config): List<String> {
        val response = Http.request(
            url = Http.join(config.baseUrl, "models"),
            method = "GET",
            headers = headers(config),
            readTimeoutMs = 30_000
        )
        val root = JSONObject(response)
        val data = root.optJSONArray("data") ?: root.optJSONArray("models") ?: JSONArray()
        val names = (0 until data.length()).mapNotNull { index ->
            when (val item = data.opt(index)) {
                is JSONObject -> item.optString("id").ifBlank { item.optString("name") }
                is String -> item
                else -> null
            }?.takeIf { it.isNotBlank() }
        }
        return AiClient.sortModels(names)
    }

    private fun post(
        config: AiClient.Config,
        prompt: String,
        length: StoryLength,
        jsonMode: Boolean
    ): String = Http.request(
        url = Http.join(config.baseUrl, "chat/completions"),
        method = "POST",
        headers = headers(config),
        body = JSONObject().apply {
            put("model", config.model)
            put("temperature", 0.9)
            put("max_tokens", length.outputTokens)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            if (jsonMode) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
        }.toString()
    )

    private fun headers(config: AiClient.Config) = buildMap {
        if (config.apiKey.isNotBlank()) put("Authorization", "Bearer ${config.apiKey}")
        if (config.provider == AiProvider.OPENROUTER) {
            // OpenRouter asks clients to identify themselves.
            put("HTTP-Referer", AiProvider.PROJECT_URL)
            put("X-Title", "AI News plugin")
        }
    }

    private fun extractText(response: String): String {
        val root = JSONObject(response)
        val choices = root.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw Http.ApiException("Model returned no choices")
        }
        val message = choices.getJSONObject(0).optJSONObject("message")
            ?: throw Http.ApiException("Model returned no message")
        return when (val content = message.opt("content")) {
            is String -> content
            // Some gateways return the OpenAI "parts" array shape instead.
            is JSONArray -> buildString {
                for (i in 0 until content.length()) {
                    val part = content.opt(i)
                    if (part is JSONObject) append(part.optString("text")) else append(part)
                }
            }
            else -> throw Http.ApiException("Model returned no text")
        }
    }

    private fun looksLikeJsonModeRejection(message: String?): Boolean {
        val text = message?.lowercase() ?: return false
        return "response_format" in text || "json_object" in text || "json mode" in text
    }

    private const val SYSTEM =
        "You are a news editor. Reply with nothing but a raw JSON array of " +
            "objects with the keys \"short\", \"title\" and \"body\"."
}
