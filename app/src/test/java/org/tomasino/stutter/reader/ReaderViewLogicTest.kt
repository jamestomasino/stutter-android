package org.tomasino.stutter.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderViewLogicTest {
    private val calculator = WordLayoutCalculator()

    @Test
    fun wordPartsComputedCorrectly() {
        val parts = calculator.split("reading")

        assertEquals("re", parts.left)
        assertEquals("a", parts.center)
        assertEquals("ding", parts.right)
    }

    @Test
    fun leadingPunctuationShiftsCenter() {
        val parts = calculator.split("(hello")

        assertEquals("(he", parts.left)
        assertEquals("l", parts.center)
        assertEquals("lo", parts.right)
    }

    @Test
    fun singleCharacterCentersOnFirst() {
        val parts = calculator.split("A")

        assertEquals("", parts.left)
        assertEquals("A", parts.center)
        assertEquals("", parts.right)
    }
}
