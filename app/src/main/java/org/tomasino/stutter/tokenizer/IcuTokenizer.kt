package org.tomasino.stutter.tokenizer

import java.util.Locale

class IcuTokenizer : Tokenizer {
    private val classifier = TokenClassifier()

    override fun tokenize(text: String, languageTag: String?): List<Token> {
        if (text.isBlank()) return emptyList()

        val locale = languageTag
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { Locale.forLanguageTag(it) }
            ?: Locale.getDefault()

        val segments = segmentsFor(text, locale)
        val noSpaceLanguages = setOf("ja", "zh", "th", "lo", "km", "my")
        val isSpaceDelimited = !noSpaceLanguages.contains(locale.language)

        val tokens = if (isSpaceDelimited) {
            tokenizeSpaceDelimited(segments)
        } else {
            tokenizeNonSpaceDelimited(segments)
        }

        return tokens.map { tokenText -> classifier.classify(tokenText, languageTag) }
    }

    private fun tokenizeSpaceDelimited(segments: List<Segment>): List<String> {
        val tokens = mutableListOf<String>()
        val buffer = StringBuilder()
        for (segment in segments) {
            if (segment.isWhitespace) {
                if (buffer.isNotEmpty()) {
                    tokens.add(buffer.toString())
                    buffer.setLength(0)
                }
                continue
            }
            buffer.append(segment.text)
        }
        if (buffer.isNotEmpty()) {
            tokens.add(buffer.toString())
        }
        return tokens
    }

    private fun tokenizeNonSpaceDelimited(segments: List<Segment>): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < segments.size) {
            if (segments[i].isWhitespace) {
                i++
                continue
            }

            var prefix = ""
            var core = ""
            var suffix = ""

            while (i < segments.size && segments[i].isTrivialPunctuation()) {
                val next = segments.getOrNull(i + 1)
                if (next != null && !next.isWhitespace && !next.isTrivialPunctuation()) {
                    prefix += segments[i].text
                    i++
                } else {
                    break
                }
            }

            if (i < segments.size && !segments[i].isWhitespace && !segments[i].isTrivialPunctuation()) {
                core = segments[i].text
                i++
            }

            while (i < segments.size && segments[i].isTrivialPunctuation()) {
                suffix += segments[i].text
                i++
            }

            val token = (prefix + core + suffix).trim()
            if (token.isNotEmpty()) {
                tokens.add(token)
            }
        }

        return tokens
    }
}
