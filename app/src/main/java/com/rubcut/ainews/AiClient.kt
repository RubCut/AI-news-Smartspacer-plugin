package com.rubcut.ainews

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * One entry point for every backend: it picks the right dialect, sends the
 * same prompt and returns the same stories, so the rest of the plugin never
 * has to care who wrote them.
 */
object AiClient {

    /** Everything the clients need to reach an endpoint. */
    data class Config(
        val provider: AiProvider,
        val baseUrl: String,
        val apiKey: String,
        val model: String
    )

    suspend fun generate(
        config: Config,
        topic: String,
        count: Int,
        language: String,
        length: StoryLength
    ): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = NewsPrompt.build(topic, count, language, length)
            val source = "$topic · ${config.model}"
            when (config.provider.flavor) {
                ApiFlavor.GEMINI -> GeminiClient.generate(config, prompt, length, source)
                ApiFlavor.ANTHROPIC -> AnthropicClient.generate(config, prompt, length, source)
                ApiFlavor.OPENAI -> OpenAiClient.generate(config, prompt, length, source)
            }
        }
    }

    /**
     * Lists the models the key can use. Doubles as the API key test: a bad key
     * fails here with the provider's own error message.
     */
    suspend fun listModels(config: Config): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            when (config.provider.flavor) {
                ApiFlavor.GEMINI -> GeminiClient.listModels(config)
                ApiFlavor.ANTHROPIC -> AnthropicClient.listModels(config)
                ApiFlavor.OPENAI -> OpenAiClient.listModels(config)
            }
        }
    }

    /** Shared JSON helpers for the clients below. */
    internal fun JSONObject.putUserMessage(prompt: String) = apply {
        put("messages", JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        }))
    }

    internal fun sortModels(models: List<String>) = models.distinct().sorted()
}
