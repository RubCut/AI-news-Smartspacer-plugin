package com.rubcut.ainews

data class NewsItem(
    val id: String,
    /** Short headline shown on the Smartspacer target. */
    val shortTitle: String,
    /** Full headline shown in the article screen. */
    val title: String,
    val body: String,
    val source: String = "",
    val url: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
