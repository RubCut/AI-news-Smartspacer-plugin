package com.rubcut.ainews

import android.content.Context

/** Asks the AI backend for fresh stories and stores them for one target. */
object NewsUpdater {

    suspend fun refresh(context: Context, settings: TargetSettings): Boolean {
        if (settings.apiKey.isBlank()) {
            settings.setError(context.getString(R.string.error_no_key))
            return false
        }
        if (settings.topic.isBlank()) {
            settings.setError(context.getString(R.string.error_no_topic))
            return false
        }

        val result = when (settings.aiProvider) {
            AiProvider.GEMINI -> GeminiClient.generate(
                apiKey = settings.apiKey,
                model = settings.model,
                topic = settings.topic,
                count = settings.maxStories,
                language = settings.language
            )
        }

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
