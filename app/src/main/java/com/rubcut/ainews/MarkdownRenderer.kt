package com.rubcut.ainews

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan

/**
 * Renders the GitHub-flavoured Markdown subset the model is asked to produce:
 * headings, bold, italic, strikethrough, inline code, code blocks, bullet and
 * numbered lists, block quotes, horizontal rules and links.
 *
 * A tiny hand written renderer avoids pulling a Markdown library into a plugin
 * whose whole job is showing a few paragraphs of text.
 */
object MarkdownRenderer {

    private val BOLD = Regex("""(\*\*|__)(?=\S)(.+?)(?<=\S)\1""")
    private val ITALIC = Regex("""(?<![\w*_])([*_])(?=\S)(.+?)(?<=\S)\1(?![\w*_])""")
    private val STRIKE = Regex("""~~(?=\S)(.+?)(?<=\S)~~""")
    private val CODE = Regex("""`([^`\n]+)`""")
    private val LINK = Regex("""\[([^]]+)]\(([^)\s]+)[^)]*\)""")
    private val IMAGE = Regex("""!\[([^]]*)]\([^)]*\)""")

    fun render(markdown: String): CharSequence {
        val out = SpannableStringBuilder()
        var inCodeBlock = false
        val codeBlock = StringBuilder()
        var numbering = 0

        // Normalise: the model sometimes escapes newlines or uses \r\n.
        val lines = markdown.replace("\\n", "\n").replace("\r\n", "\n").split("\n")

        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()

            if (line.trimStart().startsWith("```")) {
                if (inCodeBlock) {
                    appendCodeBlock(out, codeBlock.toString().trimEnd('\n'))
                    codeBlock.clear()
                }
                inCodeBlock = !inCodeBlock
                return@forEachIndexed
            }
            if (inCodeBlock) {
                codeBlock.append(line).append('\n')
                return@forEachIndexed
            }

            if (line.isBlank()) {
                numbering = 0
                if (out.isNotEmpty()) out.append("\n")
                return@forEachIndexed
            }

            if (out.isNotEmpty()) out.append("\n")
            val start = out.length

            when {
                // Horizontal rule
                line.matches(Regex("""\s*([-*_])\s*\1\s*\1[\s\-*_]*""")) -> {
                    out.append("────────")
                    out.setSpan(
                        StyleSpan(Typeface.BOLD), start, out.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Headings: # … ######
                line.trimStart().startsWith("#") -> {
                    val trimmed = line.trimStart()
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    val text = trimmed.drop(level).trim()
                    appendInline(out, text)
                    val size = when (level) {
                        1 -> 1.45f
                        2 -> 1.3f
                        3 -> 1.18f
                        else -> 1.08f
                    }
                    out.setSpan(RelativeSizeSpan(size), start, out.length, SPAN)
                    out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, SPAN)
                }

                // Block quote
                line.trimStart().startsWith(">") -> {
                    appendInline(out, line.trimStart().removePrefix(">").trim())
                    out.setSpan(QuoteSpan(Color.GRAY), start, out.length, SPAN)
                    out.setSpan(StyleSpan(Typeface.ITALIC), start, out.length, SPAN)
                }

                // Bullet list: -, * or +
                line.matches(Regex("""\s*[-*+]\s+.*""")) -> {
                    numbering = 0
                    appendInline(out, line.trimStart().drop(1).trim())
                    out.setSpan(BulletSpan(GAP), start, out.length, SPAN)
                }

                // Numbered list
                line.matches(Regex("""\s*\d+[.)]\s+.*""")) -> {
                    numbering += 1
                    val text = line.trimStart().substringAfter(' ').trim()
                    out.append("$numbering. ")
                    appendInline(out, text)
                    out.setSpan(LeadingMarginSpan.Standard(0, GAP), start, out.length, SPAN)
                }

                else -> appendInline(out, line)
            }
        }

        if (inCodeBlock && codeBlock.isNotEmpty()) {
            appendCodeBlock(out, codeBlock.toString().trimEnd('\n'))
        }
        return out
    }

    private fun appendCodeBlock(out: SpannableStringBuilder, code: String) {
        if (out.isNotEmpty()) out.append("\n")
        val start = out.length
        out.append(code)
        out.setSpan(TypefaceSpan("monospace"), start, out.length, SPAN)
        out.setSpan(LeadingMarginSpan.Standard(GAP), start, out.length, SPAN)
    }

    /** Applies the inline markers inside a single line. */
    private fun appendInline(out: SpannableStringBuilder, raw: String) {
        val builder = SpannableStringBuilder(IMAGE.replace(raw) { it.groupValues[1] })

        // Links first: they change the text length the most.
        replaceAll(builder, LINK) { match, target ->
            val label = match.groupValues[1]
            val url = match.groupValues[2]
            target.replace(match.range.first, match.range.last + 1, label)
            arrayOf(URLSpan(url)) to label.length
        }
        replaceAll(builder, BOLD) { match, target ->
            val text = match.groupValues[2]
            target.replace(match.range.first, match.range.last + 1, text)
            arrayOf(StyleSpan(Typeface.BOLD)) to text.length
        }
        replaceAll(builder, STRIKE) { match, target ->
            val text = match.groupValues[1]
            target.replace(match.range.first, match.range.last + 1, text)
            arrayOf(StrikethroughSpan()) to text.length
        }
        replaceAll(builder, ITALIC) { match, target ->
            val text = match.groupValues[2]
            target.replace(match.range.first, match.range.last + 1, text)
            arrayOf(StyleSpan(Typeface.ITALIC)) to text.length
        }
        replaceAll(builder, CODE) { match, target ->
            val text = match.groupValues[1]
            target.replace(match.range.first, match.range.last + 1, text)
            arrayOf<Any>(TypefaceSpan("monospace")) to text.length
        }

        out.append(builder)
    }

    /**
     * Repeatedly applies [pattern] to [builder], letting the caller swap the
     * matched text for its content and return the spans to set on it.
     */
    private fun replaceAll(
        builder: SpannableStringBuilder,
        pattern: Regex,
        transform: (MatchResult, SpannableStringBuilder) -> Pair<Array<out Any>, Int>
    ) {
        var searchFrom = 0
        while (true) {
            val match = pattern.find(builder, searchFrom) ?: break
            val start = match.range.first
            val (spans, length) = transform(match, builder)
            spans.forEach { builder.setSpan(it, start, start + length, SPAN) }
            searchFrom = start + length
            if (searchFrom > builder.length) break
        }
    }

    private const val GAP = 28
    private const val SPAN = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
}
