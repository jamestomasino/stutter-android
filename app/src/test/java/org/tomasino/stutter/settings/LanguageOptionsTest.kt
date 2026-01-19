package org.tomasino.stutter.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageOptionsTest {
    @Test
    fun defaultsMatchSpec() {
        val defaults = LanguageOptions.DEFAULT

        assertEquals(true, defaults.autoDetectFromHtml)
        assertNull(defaults.defaultLanguageTag)
    }

    @Test
    fun normalizesBlankLanguageTags() {
        val options = LanguageOptions(autoDetectFromHtml = true, defaultLanguageTag = "   ").normalized()

        assertNull(options.defaultLanguageTag)
    }
}
