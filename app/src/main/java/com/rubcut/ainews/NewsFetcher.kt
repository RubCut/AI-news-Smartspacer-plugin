package com.rubcut.ainews

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Minimal RSS / Atom reader — no third party networking libraries needed.
 */
object NewsFetcher {

    suspend fun fetch(feedUrl: String): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "AiNewsSmartspacer/1.0")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    error("HTTP ${connection.responseCode}")
                }
                connection.inputStream.use { parse(it) }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parse(input: InputStream): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val items = mutableListOf<NewsItem>()
        var title: String? = null
        var link: String? = null
        var description: String? = null
        var date: String? = null
        var source: String? = null
        var inItem = false
        var text = StringBuilder()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase()
                    if (name == "item" || name == "entry") {
                        inItem = true
                        title = null; link = null; description = null; date = null; source = null
                    }
                    if (inItem && name == "link") {
                        // Atom puts the url in an attribute.
                        parser.getAttributeValue(null, "href")?.let { link = it }
                    }
                    text = StringBuilder()
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> text.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase()
                    val value = text.toString().trim()
                    if (inItem) {
                        when (name) {
                            "title" -> title = value
                            "link" -> if (link.isNullOrBlank() && value.isNotBlank()) link = value
                            "description", "summary", "content" ->
                                if (description.isNullOrBlank()) description = value
                            "pubdate", "published", "updated" -> date = value
                            "source", "author", "name" ->
                                if (source.isNullOrBlank() && value.isNotBlank()) source = value
                            "item", "entry" -> {
                                inItem = false
                                val fullTitle = title?.let { stripHtml(it) }
                                if (!fullTitle.isNullOrBlank()) {
                                    items.add(
                                        NewsItem(
                                            id = (link ?: fullTitle).hashCode().toString(),
                                            shortTitle = shorten(fullTitle),
                                            title = fullTitle,
                                            body = stripHtml(description ?: ""),
                                            source = source.orEmpty(),
                                            url = link,
                                            timestamp = parseDate(date)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    text = StringBuilder()
                }
            }
        }
        return items
    }

    /** Trims a headline down to something that fits on the smartspace. */
    fun shorten(title: String): String {
        // Google News style: "Headline - Publisher" — the publisher is noise here.
        val head = title.substringBeforeLast(" - ", title).trim()
        if (head.length <= Constants.SHORT_TITLE_MAX_CHARS) return head
        val cut = head.take(Constants.SHORT_TITLE_MAX_CHARS)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > 20) cut.take(lastSpace) else cut).trimEnd(',', '.', ';', ':') + "…"
    }

    private fun stripHtml(raw: String): String = raw
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()

    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    private fun parseDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        dateFormats.forEach { pattern ->
            runCatching {
                return SimpleDateFormat(pattern, Locale.US).parse(raw)!!.time
            }
        }
        return System.currentTimeMillis()
    }
}
