package org.tomasino.stutter.scheduler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.tokenizer.Token

data class ScheduledToken(
    val index: Int,
    val token: Token,
    val targetTimeMs: Long,
)

sealed class SchedulerState {
    data object Idle : SchedulerState()
    data object Playing : SchedulerState()
    data object Paused : SchedulerState()
    data object Finished : SchedulerState()
}

interface Scheduler {
    val state: StateFlow<SchedulerState>
    val events: Flow<ScheduledToken>

    fun load(tokens: List<Token>, options: PlaybackOptions)
    fun play()
    fun pause()
    fun resume()
    fun restart()
    fun skipForward()
    fun skipBack()
}
