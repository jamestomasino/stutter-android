package org.tomasino.stutter.tokenizer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tomasino.stutter.hyphenation.PatternHyphenator
import org.tomasino.stutter.scheduler.MonotonicClock
import org.tomasino.stutter.scheduler.ScheduledToken
import org.tomasino.stutter.scheduler.SchedulerImpl
import org.tomasino.stutter.settings.PlaybackOptions

@OptIn(ExperimentalCoroutinesApi::class)
class TokenPipelineTest {
    @Test
    fun statusMessageTokenizesAndSchedulesLikeInput() = runTest {
        val tokens = buildTokensForText(
            text = "Status message",
            languageTag = "en",
            maxWordLength = 13,
            tokenizer = IcuTokenizer(),
            hyphenator = PatternHyphenator(),
        )

        assertEquals(listOf("Status", "message"), tokens.map { it.text })

        val scheduler = SchedulerImpl(this, TestMonotonicClock(testScheduler))
        scheduler.load(tokens, baseOptions())

        val events = mutableListOf<ScheduledToken>()
        val job = launch { scheduler.events.take(2).toList(events) }

        scheduler.play()
        advanceTimeBy(2000)
        runCurrent()
        job.join()

        assertEquals(listOf(0L, 1000L), events.map { it.targetTimeMs })
    }

    @Test
    fun marksParagraphBoundariesOnLastTokenOfParagraph() {
        val tokens = buildTokensForText(
            text = "First paragraph.\n\nSecond paragraph.",
            languageTag = "en",
            maxWordLength = 13,
            tokenizer = IcuTokenizer(),
            hyphenator = PatternHyphenator(),
        )

        assertEquals(
            listOf("First", "paragraph.", "Second", "paragraph."),
            tokens.map { it.text },
        )
        assertEquals(
            listOf(false, true, false, true),
            tokens.map { it.isParagraphEnd },
        )
    }

    @Test
    fun preservesNumericGroupSeparatorsDuringLongWordSplitting() {
        val tokens = buildTokensForText(
            text = "Upphaed 1.000isk 1.000.000",
            languageTag = "is",
            maxWordLength = 4,
            tokenizer = IcuTokenizer(),
            hyphenator = PatternHyphenator(),
        )

        assertTrue(tokens.any { it.text == "1.000isk" })
        assertTrue(tokens.any { it.text == "1.000.000" })
    }

    private fun baseOptions(): PlaybackOptions {
        return PlaybackOptions.DEFAULT.copy(
            wpm = 60,
            slowStartCount = 1,
            sentenceDelay = 1f,
            otherPuncDelay = 1f,
            shortWordDelay = 1f,
            longWordDelay = 1f,
            numericDelay = 1f,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class TestMonotonicClock(
    private val scheduler: TestCoroutineScheduler,
) : MonotonicClock {
    override fun nowMs(): Long = scheduler.currentTime
}
