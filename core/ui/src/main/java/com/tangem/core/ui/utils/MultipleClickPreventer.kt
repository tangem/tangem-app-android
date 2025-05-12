package com.tangem.core.ui.utils

import android.os.SystemClock

/**
 * Debounce multiple events (e.i. clicks, etc) in a short period of time
 */
interface MultipleClickPreventer {

    fun processEvent(event: () -> Unit)

    companion object {

        fun get(): MultipleClickPreventer = DefaultMultipleClickPreventer()
    }
}

private class DefaultMultipleClickPreventer : MultipleClickPreventer {
    private val now: Long get() = SystemClock.elapsedRealtime()
    private var lastEventTimeMs: Long = 0

    override fun processEvent(event: () -> Unit) {
        if (now - lastEventTimeMs >= TIME_BETWEEN_CLICK_IN_MS) event()
        lastEventTimeMs = now
    }

    companion object {
        private const val TIME_BETWEEN_CLICK_IN_MS = 500L
    }
}

/**
 * Debounce multiple events (e.i. clicks, etc) in a short period of time
 */
fun singleEvent(event: () -> Unit) = MultipleClickPreventer.get().processEvent(event)