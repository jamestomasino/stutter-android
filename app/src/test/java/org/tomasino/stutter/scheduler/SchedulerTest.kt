package org.tomasino.stutter.scheduler

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.tokenizer.Token

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerTest {
    @Test
    fun emitsTokensInOrder() = runTest {
        val scheduler = SchedulerImpl(this, TestMonotonicClock(testScheduler))
        val tokens = listOf(token("one"), token("two"), token("three"))
        scheduler.load(tokens, baseOptions())

        val events = mutableListOf<ScheduledToken>()
        val job = launch { scheduler.events.take(3).toList(events) }

        scheduler.play()
        advanceTimeBy(2000)
        runCurrent()
        job.join()

        assertEquals(listOf(0, 1, 2), events.map { it.index })
    }

    @Test
    fun pauseResumePreservesIndex() = runTest {
        val scheduler = SchedulerImpl(this, TestMonotonicClock(testScheduler))
        val tokens = listOf(token("one"), token("two"))
        scheduler.load(tokens, baseOptions())

        val events = mutableListOf<ScheduledToken>()
        val job = launch { scheduler.events.take(2).toList(events) }

        scheduler.play()
        runCurrent()
        assertEquals(1, events.size)

        scheduler.pause()
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(1, events.size)

        scheduler.resume()
        advanceTimeBy(1000)
        runCurrent()
        job.join()

        assertEquals(listOf(0, 1), events.map { it.index })
    }

    @Test
    fun restartResetsToZero() = runTest {
        val scheduler = SchedulerImpl(this, TestMonotonicClock(testScheduler))
        val tokens = listOf(token("one"), token("two"))
        scheduler.load(tokens, baseOptions())

        val events = mutableListOf<ScheduledToken>()
        val job = launch { scheduler.events.take(2).toList(events) }

        scheduler.play()
        runCurrent()
        scheduler.restart()
        runCurrent()
        job.join()

        assertEquals(listOf(0, 0), events.map { it.index })
    }

    @Test
    fun skipForwardBackwardRespectsSkipCount() = runTest {
        val scheduler = SchedulerImpl(this, TestMonotonicClock(testScheduler))
        val tokens = listOf(token("one"), token("two"), token("three"), token("four"))
        scheduler.load(tokens, baseOptions().copy(skipCount = 2))

        val events = mutableListOf<ScheduledToken>()
        val job = launch { scheduler.events.take(2).toList(events) }

        scheduler.skipForward()
        scheduler.play()
        runCurrent()

        scheduler.skipBack()
        runCurrent()
        job.join()

        assertEquals(listOf(2, 0), events.map { it.index })
    }

    @Test
    fun driftCorrectionUsesTargetTimestamps() = runTest {
        val scheduler = SchedulerImpl(this, TestMonotonicClock(testScheduler))
        val tokens = listOf(token("one"), token("two"), token("three"))
        scheduler.load(tokens, baseOptions())

        val events = mutableListOf<ScheduledToken>()
        val job = launch { scheduler.events.take(3).toList(events) }

        scheduler.play()
        advanceTimeBy(5000)
        runCurrent()
        job.join()

        val targets = events.map { it.targetTimeMs }
        assertEquals(listOf(0L, 1000L, 2000L), targets)
        assertTrue(events.last().targetTimeMs <= testScheduler.currentTime)
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

    private fun token(text: String): Token {
        return Token(
            text = text,
            isSentenceEnd = false,
            isOtherPunctuation = false,
            isNumeric = false,
            isShortWord = false,
            isLongWord = false,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class TestMonotonicClock(
    private val scheduler: TestCoroutineScheduler,
) : MonotonicClock {
    override fun nowMs(): Long = scheduler.currentTime
}
