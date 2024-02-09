package com.tangem.core.ui.utils

import android.os.SystemClock

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