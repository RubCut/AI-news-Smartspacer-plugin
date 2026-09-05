package com.rubcut.ainews

/** The single prompt every backend is asked with. */
object NewsPrompt {

    fun build(topic: String, count: Int, language: String, length: StoryLength) = """
        You are a news editor. Write $count distinct news articles about: "$topic".
        Base them on what you know; keep them plausible, factual in tone and self-contained.
        Language of the output: $language.

        For every item return:
        - "short": a very short headline for a home screen widget, max ${Constants.SHORT_TITLE_MAX_CHARS} characters, no trailing period.
        - "title": the full headline, max 120 characters.
        - "body": the article itself — ${length.instruction}.

        Format "body" with GitHub flavoured Markdown and use it generously:
        **bold** for key facts and names, *italics* for emphasis, `##` and `###`
        section headings, "- " bulleted lists, "1. " numbered lists, "> " for
        quotes from people, --- for a separator and [text](https://url) links.
        Separate paragraphs with a blank line. Do not wrap the whole body in a
        code block and do not repeat the headline as the first line.

        Reply with nothing but a JSON array of objects with exactly the keys
        "short", "title" and "body".
    """.trimIndent()
}
