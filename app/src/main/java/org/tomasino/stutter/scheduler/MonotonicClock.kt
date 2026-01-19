package org.tomasino.stutter.scheduler

import android.os.SystemClock

interface MonotonicClock {
    fun nowMs(): Long
}

class AndroidMonotonicClock : MonotonicClock {
    override fun nowMs(): Long = SystemClock.elapsedRealtime()
}
