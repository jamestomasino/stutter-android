package org.tomasino.stutter.tokenizer

import java.text.BreakIterator
import java.util.Locale

internal data class Segment(
    val text: String,
    val isWhitespace: Boolean,
    val isWordLike: Boolean,
) {
    fun isTrivialPunctuation(): Boolean {
        return !isWhitespace && !isWordLike && TRIVIAL_PUNCTUATION_REGEX.matches(text)
    }

    companion object {
        fun from(text: String): Segment {
            val isWhitespace = text.all(Char::isWhitespace)
            val isWordLike = text.any(Char::isLetterOrDigit)
            return Segment(text, isWhitespace, isWordLike)
        }
    }
}

internal fun segmentsFor(text: String, locale: Locale): List<Segment> {
    val iterator = BreakIterator.getWordInstance(locale)
    iterator.setText(text)
    val segments = mutableListOf<Segment>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        val segmentText = text.substring(start, end)
        segments.add(Segment.from(segmentText))
        start = end
        end = iterator.next()
    }
    return segments
}

internal val SENTENCE_END_REGEX = Regex("[.!?]|[。！？؟]")
internal val TRIVIAL_PUNCTUATION_REGEX =
    Regex("^[\\p{P}\\p{S}。、・：；？！「」（）【】『』［］〔〕〈〉《》]+$")

internal const val SHORT_WORD_THRESHOLD = 5
internal const val LONG_WORD_THRESHOLD = 9
