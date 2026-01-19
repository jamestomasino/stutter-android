package org.tomasino.stutter.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackOptionsTest {
    @Test
    fun defaultsMatchSpec() {
        val defaults = PlaybackOptions.DEFAULT

        assertEquals(400, defaults.wpm)
        assertEquals(5, defaults.slowStartCount)
        assertEquals(2.5f, defaults.sentenceDelay, 0f)
        assertEquals(1.5f, defaults.otherPuncDelay, 0f)
        assertEquals(1.3f, defaults.shortWordDelay, 0f)
        assertEquals(1.4f, defaults.longWordDelay, 0f)
        assertEquals(1.8f, defaults.numericDelay, 0f)
        assertEquals(10, defaults.skipCount)
    }

    @Test
    fun clampsBelowMinimum() {
        val options = PlaybackOptions(
            wpm = 1,
            slowStartCount = 0,
            sentenceDelay = 0.1f,
            otherPuncDelay = 0.2f,
            shortWordDelay = 0.3f,
            longWordDelay = 0.4f,
            numericDelay = 0.5f,
            skipCount = -5,
        ).clamped()

        assertEquals(PlaybackOptions.MIN_WPM, options.wpm)
        assertEquals(PlaybackOptions.MIN_SLOW_START, options.slowStartCount)
        assertEquals(PlaybackOptions.MIN_SENTENCE_DELAY, options.sentenceDelay, 0f)
        assertEquals(PlaybackOptions.MIN_OTHER_PUNC_DELAY, options.otherPuncDelay, 0f)
        assertEquals(PlaybackOptions.MIN_SHORT_WORD_DELAY, options.shortWordDelay, 0f)
        assertEquals(PlaybackOptions.MIN_LONG_WORD_DELAY, options.longWordDelay, 0f)
        assertEquals(PlaybackOptions.MIN_NUMERIC_DELAY, options.numericDelay, 0f)
        assertEquals(PlaybackOptions.MIN_SKIP_COUNT, options.skipCount)
    }

    @Test
    fun clampsAboveMaximum() {
        val options = PlaybackOptions(
            wpm = 5000,
            slowStartCount = 50,
            sentenceDelay = 20f,
            otherPuncDelay = 20f,
            shortWordDelay = 20f,
            longWordDelay = 20f,
            numericDelay = 20f,
            skipCount = 500,
        ).clamped()

        assertEquals(PlaybackOptions.MAX_WPM, options.wpm)
        assertEquals(PlaybackOptions.MAX_SLOW_START, options.slowStartCount)
        assertEquals(PlaybackOptions.MAX_SENTENCE_DELAY, options.sentenceDelay, 0f)
        assertEquals(PlaybackOptions.MAX_OTHER_PUNC_DELAY, options.otherPuncDelay, 0f)
        assertEquals(PlaybackOptions.MAX_SHORT_WORD_DELAY, options.shortWordDelay, 0f)
        assertEquals(PlaybackOptions.MAX_LONG_WORD_DELAY, options.longWordDelay, 0f)
        assertEquals(PlaybackOptions.MAX_NUMERIC_DELAY, options.numericDelay, 0f)
        assertEquals(PlaybackOptions.MAX_SKIP_COUNT, options.skipCount)
    }
}
