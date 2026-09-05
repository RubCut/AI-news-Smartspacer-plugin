package com.rubcut.ainews

/** How long the generated article should be. */
enum class StoryLength(
    val id: String,
    val labelRes: Int,
    /** What the model is told to write. */
    val instruction: String,
    /** Rough output budget so long stories are not cut off mid-sentence. */
    val outputTokens: Int
) {
    SHORT(
        "short",
        R.string.length_short,
        "4-6 sentences in 1-2 paragraphs",
        2048
    ),
    MEDIUM(
        "medium",
        R.string.length_medium,
        "3-4 paragraphs of 4-6 sentences each, with a short bulleted \"Key points\" list",
        4096
    ),
    LONG(
        "long",
        R.string.length_long,
        "a detailed article of 6-8 paragraphs with Markdown section headings, " +
            "a bulleted \"Key points\" list and a short closing paragraph",
        8192
    );

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}
