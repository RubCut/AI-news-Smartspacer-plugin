package com.rubcut.ainews

import android.content.Context

/** Asks the configured AI backend for fresh stories and stores them. */
object NewsUpdater {

    suspend fun refresh(context: Context, settings: TargetSettings): Boolean {
        val provider = settings.aiProvider
        if (settings.topic.isBlank()) {
            settings.setError(context.getString(R.string.error_no_topic))
            return false
        }
        if (provider.requiresKey && settings.apiKey.isBlank()) {
            settings.setError(context.getString(R.string.error_no_key))
            return false
        }
        if (settings.baseUrl.isBlank()) {
            settings.setError(context.getString(R.string.error_no_base_url))
            return false
        }
        if (settings.model.isBlank()) {
            settings.setError(context.getString(R.string.error_no_model))
            return false
        }

        val result = AiClient.generate(
            config = settings.toClientConfig(),
            topic = settings.topic,
            count = settings.maxStories,
            language = settings.language,
            length = settings.storyLength
        )

        val items = result.getOrNull()
        return when {
            items == null -> {
                settings.setError(
                    context.getString(
                        R.string.error_generation_failed,
                        result.exceptionOrNull()?.message ?: "unknown"
                    )
                )
                false
            }
            items.isEmpty() -> {
                settings.setError(context.getString(R.string.error_empty_result))
                false
            }
            else -> {
                // Generated stories replace the old ones; dismissals only apply
                // to the ids that are still around.
                settings.setStories(items)
                true
            }
        }
    }
}

/** Bundles the endpoint details this target is configured with. */
fun TargetSettings.toClientConfig() = AiClient.Config(
    provider = aiProvider,
    baseUrl = baseUrl,
    apiKey = apiKey,
    model = model
)
