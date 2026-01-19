package org.tomasino.stutter.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageResolverTest {
    private val resolver: LanguageResolver = BasicLanguageResolver()

    @Test
    fun htmlOverridesUserDefault() {
        val result = resolver.resolve("fr", "en", "de")

        assertEquals("fr", result)
    }

    @Test
    fun userDefaultOverridesDeviceLocale() {
        val result = resolver.resolve(null, "en-US", "de")

        assertEquals("en-US", result)
    }

    @Test
    fun deviceLocaleUsedWhenNoOtherTags() {
        val result = resolver.resolve(null, null, "de")

        assertEquals("de", result)
    }

    @Test
    fun invalidTagsFallbackSafely() {
        val result = resolver.resolve(" ", "und", "en")

        assertEquals("en", result)
    }

    @Test
    fun returnsNullWhenAllInvalid() {
        val result = resolver.resolve("", "   ", "und")

        assertNull(result)
    }
}
