package com.rubcut.ainews

object Constants {
    const val TARGET_AUTHORITY = "com.rubcut.ainews.target.news"
    const val DEFAULT_REFRESH_PERIOD_MINUTES = 60
    const val MIN_REFRESH_MINUTES = 15
    const val MAX_REFRESH_MINUTES = 480

    /** Short headline length limit used on the smartspace. */
    const val SHORT_TITLE_MAX_CHARS = 42

    val DEFAULT_FEED = "https://news.google.com/rss/search?q=artificial+intelligence&hl=en-US&gl=US&ceid=US:en"
}
