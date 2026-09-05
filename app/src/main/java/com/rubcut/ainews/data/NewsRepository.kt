package com.rubcut.ainews.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Very small persistent store for news items.
 *
 * Items live in SharedPreferences as JSON so that the Smartspacer process
 * (which binds our ContentProvider) and our own UI always agree on state.
 * Dismissed ids are remembered so a dismissed target never comes back.
 */
object NewsRepository {

    private const val PREFS = "ai_news"
    private const val KEY_ITEMS = "items"
    private const val KEY_DISMISSED = "dismissed"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<NewsItem> {
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: run {
            // First run: seed with a sample item so the target is visible.
            val seed = listOf(sample())
            save(context, seed)
            return seed
        }
        return parse(raw)
    }

    /** Items that are still allowed to be shown on the smartspace. */
    fun getVisible(context: Context): List<NewsItem> {
        val dismissed = dismissedIds(context)
        return getAll(context).filterNot { dismissed.contains(it.id) }
            .sortedByDescending { it.timestamp }
    }

    fun get(context: Context, id: String): NewsItem? = getAll(context).firstOrNull { it.id == id }

    @Synchronized
    fun add(context: Context, item: NewsItem) {
        val items = getAll(context).filterNot { it.id == item.id } + item
        save(context, items.sortedByDescending { it.timestamp }.take(50))
    }

    @Synchronized
    fun dismiss(context: Context, id: String) {
        val dismissed = dismissedIds(context).toMutableSet()
        dismissed.add(id)
        prefs(context).edit().putStringSet(KEY_DISMISSED, dismissed).apply()
    }

    @Synchronized
    fun clearDismissed(context: Context) {
        prefs(context).edit().remove(KEY_DISMISSED).apply()
    }

    private fun dismissedIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_DISMISSED, emptySet()) ?: emptySet()

    @Synchronized
    private fun save(context: Context, items: List<NewsItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("shortTitle", item.shortTitle)
                put("title", item.title)
                put("body", item.body)
                put("source", item.source)
                put("url", item.url ?: "")
                put("timestamp", item.timestamp)
            })
        }
        prefs(context).edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun parse(raw: String): List<NewsItem> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            NewsItem(
                id = o.getString("id"),
                shortTitle = o.getString("shortTitle"),
                title = o.getString("title"),
                body = o.optString("body"),
                source = o.optString("source"),
                url = o.optString("url").takeIf { it.isNotBlank() },
                timestamp = o.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }.getOrDefault(emptyList())

    private fun sample() = NewsItem(
        id = "sample",
        shortTitle = "AI News",
        title = "Welcome to AI News for Smartspacer",
        body = "This is a sample story. Tap the target on your home screen to open the " +
            "full article here. Use \"Close & dismiss\" to remove the target from " +
            "Smartspacer, or \"Close\" to just close this window and keep it around.",
        source = "AI News plugin"
    )
}
