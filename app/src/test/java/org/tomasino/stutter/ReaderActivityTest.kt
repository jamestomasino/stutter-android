package org.tomasino.stutter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tomasino.stutter.scheduler.ScheduledToken
import org.tomasino.stutter.scheduler.Scheduler
import org.tomasino.stutter.scheduler.SchedulerState
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.tokenizer.Token

class ReaderActivityTest {
    @Test
    fun reloadWhilePlayingStopsAtFirstToken() {
        val scheduler = FakeScheduler(SchedulerState.Playing)

        loadTokensIntoScheduler(
            scheduler = scheduler,
            tokens = listOf(token("replacement")),
            options = PlaybackOptions.DEFAULT,
        )

        assertEquals(
            listOf("load", "restart"),
            scheduler.calls,
        )
    }

    @Test
    fun reloadWhileNotPlayingKeepsPausedPreviewBehavior() {
        val scheduler = FakeScheduler(SchedulerState.Paused)

        loadTokensIntoScheduler(
            scheduler = scheduler,
            tokens = listOf(token("replacement")),
            options = PlaybackOptions.DEFAULT,
        )

        assertEquals(
            listOf("load", "restart"),
            scheduler.calls,
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
            isParagraphEnd = false,
        )
    }
}

private class FakeScheduler(initialState: SchedulerState) : Scheduler {
    override val state = MutableStateFlow(initialState)
    override val events: Flow<ScheduledToken>
        get() = throw UnsupportedOperationException("Not used in these tests")

    val calls = mutableListOf<String>()

    override fun load(tokens: List<Token>, options: PlaybackOptions) {
        calls += "load"
        state.value = SchedulerState.Idle
    }

    override fun updateOptions(options: PlaybackOptions) = Unit

    override fun play() {
        calls += "play"
        state.value = SchedulerState.Playing
    }

    override fun pause() = Unit

    override fun resume() = Unit

    override fun restart() {
        calls += "restart"
        state.value = SchedulerState.Paused
    }

    override fun seekTo(index: Int) = Unit

    override fun skipForward() = Unit

    override fun skipBack() = Unit
}
