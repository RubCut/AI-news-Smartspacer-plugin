package com.rubcut.ainews

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Per-target settings and cached stories.
 *
 * Everything is keyed by the Smartspacer id, so several "AI News" targets can
 * live side by side with their own feed, refresh interval and dismissed list.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ai_news", Context.MODE_PRIVATE)

    fun forTarget(smartspacerId: String) = TargetSettings(prefs, smartspacerId)

    fun clearTarget(smartspacerId: String) {
        val prefix = "$smartspacerId."
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith(prefix) }.forEach { remove(it) }
        }.apply()
    }
}

class TargetSettings(
    private val prefs: android.content.SharedPreferences,
    private val smartspacerId: String
) {

    private fun key(name: String) = "$smartspacerId.$name"

    /** Namespaced so each provider keeps its own key, model and base URL. */
    private fun providerKey(name: String) = "$smartspacerId.${aiProvider.id}.$name"

    /** What the model should write about, e.g. "AI and robotics". */
    var topic: String
        get() = prefs.getString(key("topic"), Constants.DEFAULT_TOPIC) ?: Constants.DEFAULT_TOPIC
        set(value) = prefs.edit().putString(key("topic"), value.trim()).apply()

    var aiProvider: AiProvider
        get() = AiProvider.fromId(prefs.getString(key("provider"), null))
        set(value) = prefs.edit().putString(key("provider"), value.id).apply()

    /**
     * Keys, models and base URLs are stored per provider, so switching between
     * backends and back keeps everything that was already typed in.
     */
    var apiKey: String
        get() = prefs.getString(providerKey("api_key"), "").orEmpty()
        set(value) = prefs.edit().putString(providerKey("api_key"), value.trim()).apply()

    var model: String
        get() = prefs.getString(providerKey("model"), null)?.takeIf { it.isNotBlank() }
            ?: aiProvider.defaultModel
        set(value) = prefs.edit().putString(providerKey("model"), value.trim()).apply()

    var baseUrl: String
        get() = prefs.getString(providerKey("base_url"), null)?.takeIf { it.isNotBlank() }
            ?: aiProvider.defaultBaseUrl
        set(value) = prefs.edit().putString(providerKey("base_url"), value.trim()).apply()

    /** Models fetched from the API for the current provider, if any. */
    var cachedModels: List<String>
        get() = prefs.getString(providerKey("models"), null)
            ?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        set(value) = prefs.edit()
            .putString(providerKey("models"), value.joinToString("\n")).apply()

    /** Language the stories are written in; defaults to the device language. */
    var language: String
        get() = prefs.getString(key("language"), null)
            ?: java.util.Locale.getDefault().displayLanguage
        set(value) = prefs.edit().putString(key("language"), value.trim()).apply()

    /** How long the generated articles should be. */
    var storyLength: StoryLength
        get() = StoryLength.fromId(prefs.getString(key("length"), null))
        set(value) = prefs.edit().putString(key("length"), value.id).apply()

    val isConfigured: Boolean
        get() = topic.isNotBlank() &&
            baseUrl.isNotBlank() &&
            model.isNotBlank() &&
            (apiKey.isNotBlank() || !aiProvider.requiresKey)

    var refreshIntervalMinutes: Int
        get() = prefs.getInt(key("interval"), Constants.DEFAULT_REFRESH_PERIOD_MINUTES)
        set(value) = prefs.edit().putInt(
            key("interval"),
            value.coerceIn(Constants.MIN_REFRESH_MINUTES, Constants.MAX_REFRESH_MINUTES)
        ).apply()

    /** How many stories this target may show at once. */
    var maxStories: Int
        get() = prefs.getInt(key("max"), 1)
        set(value) = prefs.edit().putInt(key("max"), value.coerceIn(1, 5)).apply()

    var lastUpdated: Long
        get() = prefs.getLong(key("updated"), 0L)
        private set(value) = prefs.edit().putLong(key("updated"), value).apply()

    var lastError: String?
        get() = prefs.getString(key("error"), null)
        private set(value) = prefs.edit().putString(key("error"), value).apply()

    // region stories

    fun getStories(): List<NewsItem> {
        val raw = prefs.getString(key("stories"), null) ?: return emptyList()
        return runCatching {
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
                    timestamp = o.optLong("timestamp", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun getVisibleStories(): List<NewsItem> {
        val dismissed = dismissedIds()
        return getStories()
            .filterNot { dismissed.contains(it.id) }
            .sortedByDescending { it.timestamp }
            .take(maxStories)
    }

    fun setStories(items: List<NewsItem>, error: String? = null) {
        val array = JSONArray()
        items.sortedByDescending { it.timestamp }.take(30).forEach { item ->
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
        prefs.edit().putString(key("stories"), array.toString()).apply()
        lastUpdated = System.currentTimeMillis()
        lastError = error
    }

    fun setError(error: String?) {
        lastError = error
    }

    fun getStory(id: String): NewsItem? = getStories().firstOrNull { it.id == id }

    // endregion

    // region dismissals

    fun dismissedIds(): Set<String> =
        prefs.getStringSet(key("dismissed"), emptySet()) ?: emptySet()

    fun dismiss(id: String) {
        val updated = dismissedIds().toMutableSet().apply { add(id) }
        // Keep the list bounded; old ids can never match new stories anyway.
        prefs.edit().putStringSet(key("dismissed"), updated.toList().takeLast(200).toSet()).apply()
    }

    /** True when stories exist but the user dismissed every single one. */
    fun hasDismissedEverything(): Boolean {
        val stories = getStories()
        if (stories.isEmpty()) return false
        val dismissed = dismissedIds()
        return stories.all { dismissed.contains(it.id) }
    }

    fun clearDismissed() {
        prefs.edit().remove(key("dismissed")).apply()
    }

    // endregion
}
