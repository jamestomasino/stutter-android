package org.tomasino.stutter.hyphenation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tomasino.stutter.tokenizer.TokenClassifier

class HyphenationPatternsTest {
    private val hyphenator = PatternHyphenator()
    private val fallback = FallbackHyphenator()
    private val classifier = TokenClassifier()
    private val repository = HyphenationPatternRepository()

    @Test
    fun germanPatternsSplitAtPlausiblePoints() {
        val word = "Donaudampfschifffahrt"
        val maxLength = 6
        val segments = hyphenator.split(word, "de", maxLength)
        val fallbackSegments = fallback.split(word, "de", maxLength)

        assertTrue(segments.size > 1)
        assertEquals(word, segments.joinToString(""))
        assertTrue(segments != fallbackSegments)
        segments.forEach { segment ->
            val length = classifier.wordLength(segment, "de")
            assertTrue(length <= maxLength)
        }
    }

    @Test
    fun englishPatternsSplitAtPlausiblePoints() {
        val word = "characterization"
        val maxLength = 5
        val segments = hyphenator.split(word, "en", maxLength)
        val fallbackSegments = fallback.split(word, "en", maxLength)

        assertTrue(segments.size > 1)
        assertEquals(word, segments.joinToString(""))
        assertTrue(segments != fallbackSegments)
        segments.forEach { segment ->
            val length = classifier.wordLength(segment, "en")
            assertTrue(length <= maxLength)
        }
    }

    @Test
    fun englishPatternsHandleExtendedLocaleTags() {
        val word = "characterization"
        val maxLength = 5
        val segments = hyphenator.split(word, "en-US-u-va-posix", maxLength)
        val fallbackSegments = fallback.split(word, "en-US-u-va-posix", maxLength)

        assertTrue(segments.size > 1)
        assertEquals(word, segments.joinToString(""))
        assertTrue(segments != fallbackSegments)
        segments.forEach { segment ->
            val length = classifier.wordLength(segment, "en-US-u-va-posix")
            assertTrue(length <= maxLength)
        }
    }

    @Test
    fun frenchPatternsSplitAtPlausiblePoints() {
        val word = "anticonstitutionnellement"
        val maxLength = 7
        val segments = hyphenator.split(word, "fr", maxLength)
        val fallbackSegments = fallback.split(word, "fr", maxLength)

        assertTrue(segments.size > 1)
        assertEquals(word, segments.joinToString(""))
        assertTrue(segments != fallbackSegments)
        segments.forEach { segment ->
            val length = classifier.wordLength(segment, "fr")
            assertTrue(length <= maxLength)
        }
    }

    @Test
    fun languageTagsFallBackToBaseLanguage() {
        val word = "characterization"
        val maxLength = 5
        val segments = hyphenator.split(word, "en-US", maxLength)
        val fallbackSegments = fallback.split(word, "en-US", maxLength)

        assertTrue(segments.size > 1)
        assertEquals(word, segments.joinToString(""))
        assertTrue(segments != fallbackSegments)
        segments.forEach { segment ->
            val length = classifier.wordLength(segment, "en-US")
            assertTrue(length <= maxLength)
        }
    }

    @Test
    fun languageTagsWithScriptOrRegionFallback() {
        val word = "Donaudampfschifffahrt"
        val maxLength = 6
        val segments = hyphenator.split(word, "de-DE", maxLength)
        val fallbackSegments = fallback.split(word, "de-DE", maxLength)

        assertTrue(segments.size > 1)
        assertEquals(word, segments.joinToString(""))
        assertTrue(segments != fallbackSegments)
        segments.forEach { segment ->
            val length = classifier.wordLength(segment, "de-DE")
            assertTrue(length <= maxLength)
        }
    }

    @Test
    fun unknownLanguageFallsBackToNaiveSplit() {
        val word = "supercalifragilistic"
        val maxLength = 5
        val segments = hyphenator.split(word, "xx-YY", maxLength)
        val fallbackSegments = fallback.split(word, "xx-YY", maxLength)

        assertEquals(fallbackSegments, segments)
        segments.forEach { segment ->
            val length = classifier.wordLength(segment, "xx-YY")
            assertTrue(length <= maxLength)
        }
    }

    @Test
    fun repositorySupportsTopLanguages() {
        val supported = listOf(
            "ar",
            "bn",
            "de",
            "en",
            "es",
            "fa",
            "fr",
            "hi",
            "id",
            "it",
            "mr",
            "pt",
            "ru",
            "ta",
            "te",
            "th",
            "tr",
            "vi",
        )

        supported.forEach { tag ->
            assertNotNull("Expected patterns for $tag", repository.load(tag))
        }
        assertNull(repository.load("ur"))
        assertNull(repository.load("sw"))
    }

    @Test
    fun fallbackUsedForUnknownLanguage() {
        val word = "supercalifragilistic"
        val maxLength = 5
        val segments = hyphenator.split(word, "xx", maxLength)
        val fallbackSegments = fallback.split(word, "xx", maxLength)

        assertEquals(fallbackSegments, segments)
    }
}
