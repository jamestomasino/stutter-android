package org.tomasino.stutter.hyphenation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tomasino.stutter.tokenizer.IcuTokenizer
import org.tomasino.stutter.tokenizer.TokenClassifier
import org.tomasino.stutter.tokenizer.splitLongTokens

class HyphenationFallbackTest {
    private val hyphenator = FallbackHyphenator()

    @Test
    fun splitsLongWords() {
        val segments = hyphenator.split("supercalifragilistic", "en", 5)

        assertTrue(segments.size > 1)
    }

    @Test
    fun segmentsRespectMaxLength() {
        val maxLength = 4
        val segments = hyphenator.split("supercalifragilistic", "en", maxLength)

        val classifier = TokenClassifier()
        segments.forEach { segment ->
            val length = classifier.wordLength(segment, "en")
            assertTrue(length <= maxLength)
        }
    }

    @Test
    fun segmentsAreNotEmpty() {
        val segments = hyphenator.split("supercalifragilistic", "en", 3)

        assertTrue(segments.all { it.isNotEmpty() })
    }

    @Test
    fun punctuationAdjacentDoesNotBreakSplit() {
        val segments = hyphenator.split("supercalifragilistic!", "en", 6)

        assertTrue(segments.last().endsWith("!"))
    }

    @Test
    fun splitLongTokensPreservesSentenceEnd() {
        val tokenizer = IcuTokenizer()
        val tokens = tokenizer.tokenize("supercalifragilistic!", "en")
        val splitTokens = splitLongTokens(tokens, "en", 6, hyphenator)

        assertTrue(splitTokens.size > 1)
        assertTrue(splitTokens.last().isSentenceEnd)
        assertEquals("!", splitTokens.last().text.takeLast(1))
    }

    @Test
    fun splitLongTokensAddsHyphenToIntermediateSegments() {
        val tokenizer = IcuTokenizer()
        val tokens = tokenizer.tokenize("supercalifragilistic", "en")
        val splitTokens = splitLongTokens(tokens, "en", 6, hyphenator)

        assertTrue(splitTokens.size > 1)
        splitTokens.dropLast(1).forEach { token ->
            assertTrue(token.text.endsWith("-"))
        }
        assertTrue(splitTokens.last().text.last().isLetter())
    }
}
