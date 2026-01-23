package org.tomasino.stutter.extractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorTest {
    private val extractor = BasicExtractor()

    @Test
    fun extractsMainContentAndLanguageTag() {
        val html = readFixture("article.html")
        val result = extractor.extract(html) as ExtractResult.Success

        assertTrue(result.content.text.contains("quick brown fox"))
        assertEquals("Sample Article", result.content.title)
        assertEquals("en", result.content.languageTag)
    }

    @Test
    fun fallsBackWhenExtractorFails() {
        val html = readFixture("fallback.html")
        val result = extractor.extract(html) as ExtractResult.Success

        assertTrue(result.content.text.contains("First paragraph content"))
    }

    @Test
    fun returnsErrorWhenContentEmpty() {
        val result = extractor.extract("<html><body><div></div></body></html>")

        assertTrue(result is ExtractResult.Error)
    }

    @Test
    fun extractsLoginLikePagesAsContent() {
        val html = readFixture("login.html")
        val result = extractor.extract(html) as ExtractResult.Success

        assertTrue(result.content.text.isNotBlank())
    }

    private fun readFixture(name: String): String {
        val resource = javaClass.classLoader?.getResource("fixtures/$name")
            ?: error("Missing fixture: $name")
        return resource.readText()
    }
}
