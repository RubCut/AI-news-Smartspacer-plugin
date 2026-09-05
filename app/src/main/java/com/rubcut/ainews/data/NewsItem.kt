package com.rubcut.ainews.data

data class NewsItem(
    val id: String,
    /** Short headline shown on the Smartspacer target. */
    val shortTitle: String,
    /** Full, human readable headline shown in the news activity. */
    val title: String,
    val body: String,
    val source: String = "",
    val url: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
