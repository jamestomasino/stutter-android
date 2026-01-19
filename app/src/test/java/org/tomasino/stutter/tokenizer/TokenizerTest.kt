package org.tomasino.stutter.tokenizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenizerTest {
    private val tokenizer = IcuTokenizer()

    @Test
    fun englishTokenizationBoundaries() {
        val tokens = tokenizer.tokenize("Hello, world! This is a test.", "en")
        val texts = tokens.map { it.text }

        assertEquals(
            listOf("Hello,", "world!", "This", "is", "a", "test."),
            texts,
        )
    }

    @Test
    fun germanCompoundWordsStayWhole() {
        val input = "Donaudampfschifffahrtsgesellschaftskapitän"
        val tokens = tokenizer.tokenize(input, "de")

        assertEquals(1, tokens.size)
        assertEquals(input, tokens.first().text)
    }

    @Test
    fun frenchPunctuationHandling() {
        val tokens = tokenizer.tokenize("Bonjour, le monde ! C'est la vie.", "fr")
        val texts = tokens.map { it.text }

        assertTrue(texts.contains("Bonjour,"))
        assertTrue(texts.contains("le"))
    }

    @Test
    fun japaneseTokenizationIsNonEmpty() {
        val tokens = tokenizer.tokenize("こんにちは世界。テスト", "ja")

        assertTrue(tokens.isNotEmpty())
        assertTrue(tokens.all { it.text.isNotBlank() })
    }

    @Test
    fun punctuationFlagsAndNumericDetection() {
        val tokens = tokenizer.tokenize("Wait... 123 ok!", "en")
        val waitToken = tokens.firstOrNull { it.text.startsWith("Wait") }
        val numericToken = tokens.firstOrNull { it.text == "123" }
        val okToken = tokens.firstOrNull { it.text.startsWith("ok") }

        assertNotNull(waitToken)
        assertTrue(waitToken!!.isSentenceEnd)
        assertFalse(waitToken.isOtherPunctuation)

        assertNotNull(numericToken)
        assertTrue(numericToken!!.isNumeric)

        assertNotNull(okToken)
        assertTrue(okToken!!.isShortWord)
    }
}
