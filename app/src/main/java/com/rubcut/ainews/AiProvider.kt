package com.rubcut.ainews

/**
 * Which AI backend generates the stories. Only Gemini is wired up for now,
 * the enum exists so other providers can be slotted in later.
 */
enum class AiProvider(val id: String, val label: String) {
    GEMINI("gemini", "Google Gemini");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: GEMINI
    }
}
