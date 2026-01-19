package org.tomasino.stutter.tokenizer

import java.util.Locale

class TokenClassifier {
    fun classify(text: String, languageTag: String?): Token {
        val locale = resolveLocale(languageTag)
        val segments = segmentsFor(text, locale)
        val wordLikeSegments = segments.filter { it.isWordLike }
        val wordLength = wordLikeSegments.sumOf { it.text.length }

        val lastSegment = segments.lastOrNull()?.text.orEmpty()
        val endsSentence = SENTENCE_END_REGEX.containsMatchIn(lastSegment)

        val nonWordPunc = segments.filter { !it.isWordLike && !it.isWhitespace }
        val hasOtherPunc = nonWordPunc.isNotEmpty() && !endsSentence

        val isNumeric = wordLikeSegments.isNotEmpty() && wordLikeSegments.all { it.text.all(Char::isDigit) }
        val isShortWord = wordLength < SHORT_WORD_THRESHOLD
        val isLongWord = wordLength >= LONG_WORD_THRESHOLD

        return Token(
            text = text,
            isSentenceEnd = endsSentence,
            isOtherPunctuation = hasOtherPunc,
            isNumeric = isNumeric,
            isShortWord = isShortWord,
            isLongWord = isLongWord,
        )
    }

    fun wordLength(text: String, languageTag: String?): Int {
        val locale = resolveLocale(languageTag)
        val segments = segmentsFor(text, locale)
        return segments.filter { it.isWordLike }.sumOf { it.text.length }
    }

    private fun resolveLocale(languageTag: String?): Locale {
        return languageTag
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { Locale.forLanguageTag(it) }
            ?: Locale.getDefault()
    }
}
