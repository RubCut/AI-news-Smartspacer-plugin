package com.rubcut.ainews

/**
 * The REST dialect a backend speaks. Three cover essentially the whole market:
 * Gemini's `generateContent`, Anthropic's `/messages` and the OpenAI
 * `/chat/completions` shape that everybody else copied.
 */
enum class ApiFlavor {
    GEMINI,
    ANTHROPIC,
    OPENAI
}
