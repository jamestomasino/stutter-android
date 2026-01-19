package org.tomasino.stutter.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.tokenizer.Token
import kotlin.math.max

class SchedulerImpl(
    private val scope: CoroutineScope,
    private val clock: MonotonicClock = AndroidMonotonicClock(),
) : Scheduler {
    private val mutableState = MutableStateFlow<SchedulerState>(SchedulerState.Idle)
    private val mutableEvents = MutableSharedFlow<ScheduledToken>(extraBufferCapacity = 64)

    override val state: StateFlow<SchedulerState> = mutableState.asStateFlow()
    override val events: Flow<ScheduledToken> = mutableEvents.asSharedFlow()

    private var tokens: List<Token> = emptyList()
    private var options: PlaybackOptions = PlaybackOptions.DEFAULT
    private var offsetsMs: List<Long> = emptyList()
    private var currentIndex = 0
    private var elapsedOffsetMs = 0L
    private var startTimeMs = 0L
    private var lastEmittedIndex = -1
    private var job: Job? = null

    override fun load(tokens: List<Token>, options: PlaybackOptions) {
        stopJob()
        this.tokens = tokens
        this.options = options
        this.offsetsMs = computeOffsets(tokens, options)
        this.currentIndex = 0
        this.elapsedOffsetMs = 0L
        this.lastEmittedIndex = -1
        mutableState.value = SchedulerState.Idle
    }

    override fun updateOptions(options: PlaybackOptions) {
        this.options = options
        if (tokens.isEmpty()) return
        this.offsetsMs = computeOffsets(tokens, options)

        val state = mutableState.value
        val baseIndex = when (state) {
            SchedulerState.Playing -> lastEmittedIndex.coerceAtLeast(0).coerceAtMost(tokens.lastIndex)
            SchedulerState.Paused -> currentIndex.coerceAtMost(tokens.lastIndex)
            SchedulerState.Finished -> tokens.lastIndex
            SchedulerState.Idle -> currentIndex.coerceAtMost(tokens.lastIndex)
        }
        currentIndex = baseIndex
        elapsedOffsetMs = offsetsMs.getOrElse(currentIndex) { 0L }
        lastEmittedIndex = -1

        if (state == SchedulerState.Playing) {
            startJob()
        }
    }

    override fun play() {
        if (tokens.isEmpty()) return
        when (mutableState.value) {
            SchedulerState.Playing -> return
            SchedulerState.Finished -> {
                currentIndex = 0
                elapsedOffsetMs = 0L
            }
            else -> Unit
        }
        startJob()
    }

    override fun pause() {
        if (mutableState.value != SchedulerState.Playing) return
        elapsedOffsetMs = max(0L, clock.nowMs() - startTimeMs)
        stopJob()
        mutableState.value = SchedulerState.Paused
    }

    override fun resume() {
        if (mutableState.value != SchedulerState.Paused) return
        startJob()
    }

    override fun restart() {
        if (tokens.isEmpty()) return
        currentIndex = 0
        elapsedOffsetMs = 0L
        lastEmittedIndex = -1
        startJob()
    }

    override fun skipForward() {
        skipBy(options.skipCount)
    }

    override fun skipBack() {
        skipBy(-options.skipCount)
    }

    private fun skipBy(delta: Int) {
        if (tokens.isEmpty()) return
        val baseIndex = if (mutableState.value == SchedulerState.Playing && lastEmittedIndex >= 0) {
            lastEmittedIndex
        } else {
            currentIndex
        }
        currentIndex = (baseIndex + delta).coerceIn(0, tokens.lastIndex)
        elapsedOffsetMs = offsetsMs.getOrElse(currentIndex) { 0L }
        lastEmittedIndex = -1
        if (mutableState.value == SchedulerState.Playing) {
            startJob()
        }
    }

    private fun startJob() {
        stopJob()
        val startTime = clock.nowMs() - elapsedOffsetMs
        startTimeMs = startTime
        job = scope.launch {
            mutableState.value = SchedulerState.Playing
            while (currentIndex < tokens.size) {
                val targetTime = startTime + offsetsMs[currentIndex]
                val delayMs = targetTime - clock.nowMs()
                if (delayMs > 0) {
                    delay(delayMs)
                }
                if (!isActive) return@launch
                mutableEvents.emit(
                    ScheduledToken(
                        index = currentIndex,
                        token = tokens[currentIndex],
                        targetTimeMs = targetTime,
                    )
                )
                lastEmittedIndex = currentIndex
                currentIndex++
            }
            mutableState.value = SchedulerState.Finished
        }
    }

    private fun stopJob() {
        job?.cancel()
        job = null
    }

    private fun computeOffsets(tokens: List<Token>, options: PlaybackOptions): List<Long> {
        if (tokens.isEmpty()) return emptyList()
        val delays = tokens.mapIndexed { index, token -> computeDelayMs(token, options, index) }
        val offsets = ArrayList<Long>(tokens.size)
        var running = 0L
        offsets.add(0L)
        for (i in 1 until tokens.size) {
            running += delays[i - 1]
            offsets.add(running)
        }
        return offsets
    }

    private fun computeDelayMs(token: Token, options: PlaybackOptions, index: Int): Long {
        var delayMs = 60000f / options.wpm.toFloat()
        if (token.isSentenceEnd) delayMs *= options.sentenceDelay
        if (token.isOtherPunctuation) delayMs *= options.otherPuncDelay
        if (token.isShortWord) delayMs *= options.shortWordDelay
        if (token.isLongWord) delayMs *= options.longWordDelay
        if (token.isNumeric) delayMs *= options.numericDelay

        if (options.slowStartCount > 1) {
            val slowStartRemaining = max(1, options.slowStartCount - index)
            delayMs *= slowStartRemaining
        }

        return delayMs.toLong()
    }
}
