package org.tomasino.stutter.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TextHandlingOptionsTest {
    @Test
    fun defaultsMatchSpec() {
        val defaults = TextHandlingOptions.DEFAULT

        assertEquals(13, defaults.maxWordLength)
        assertEquals(false, defaults.showFlankers)
        assertEquals(false, defaults.inputShelfCollapsed)
    }

    @Test
    fun maxWordLengthClamps() {
        val below = TextHandlingOptions(
            maxWordLength = 1,
            showFlankers = false,
            inputShelfCollapsed = false,
        ).clamped()
        val above = TextHandlingOptions(
            maxWordLength = 200,
            showFlankers = true,
            inputShelfCollapsed = true,
        ).clamped()

        assertEquals(TextHandlingOptions.MIN_MAX_WORD_LENGTH, below.maxWordLength)
        assertEquals(TextHandlingOptions.MAX_MAX_WORD_LENGTH, above.maxWordLength)
    }
}
