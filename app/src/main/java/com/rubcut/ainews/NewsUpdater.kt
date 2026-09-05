package com.rubcut.ainews

import android.content.Context

/** Fetches the configured feed and stores the result for one target instance. */
object NewsUpdater {

    suspend fun refresh(context: Context, settings: TargetSettings): Boolean {
        val feed = settings.feedUrl
        if (feed.isBlank()) {
            settings.setError(context.getString(R.string.error_no_feed))
            return false
        }
        val result = NewsFetcher.fetch(feed)
        val items = result.getOrNull()
        return when {
            items == null -> {
                settings.setError(
                    context.getString(
                        R.string.error_fetch_failed,
                        result.exceptionOrNull()?.message ?: "unknown"
                    )
                )
                false
            }
            items.isEmpty() -> {
                settings.setError(context.getString(R.string.error_empty_feed))
                false
            }
            else -> {
                // Keep dismissed stories dismissed by simply storing everything;
                // the target filters them out when rendering.
                settings.setStories(items)
                true
            }
        }
    }
}
