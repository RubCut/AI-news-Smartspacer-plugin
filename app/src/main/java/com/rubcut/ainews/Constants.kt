package com.rubcut.ainews

object Constants {
    const val TARGET_AUTHORITY = "com.rubcut.ainews.target.news"
    const val DEFAULT_REFRESH_PERIOD_MINUTES = 180
    const val MIN_REFRESH_MINUTES = 15
    const val MAX_REFRESH_MINUTES = 480

    /** Short headline length limit used on the smartspace. */
    const val SHORT_TITLE_MAX_CHARS = 42

    const val DEFAULT_TOPIC = "Artificial intelligence"
    const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"

    val GEMINI_MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash"
    )
}
