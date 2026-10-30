package com.tangem.core.media

import android.os.Handler
import android.os.Looper

/**
 * Defers the blocking part of a teardown: media3 waits up to 2 s for the renderer to let go of a surface, and Compose
 * disposes the view while the screen is still animating away.
 */
internal interface VideoTeardownScheduler {

    fun schedule(action: () -> Unit)

    fun cancel()
}

/** Drops everything it holds on [cancel], so each player needs an instance of its own. */
internal class MainThreadVideoTeardownScheduler : VideoTeardownScheduler {

    private val handler = Handler(Looper.getMainLooper())

    override fun schedule(action: () -> Unit) {
        handler.postDelayed(Runnable { action() }, TEARDOWN_DELAY_MS)
    }

    override fun cancel() {
        handler.removeCallbacksAndMessages(null)
    }

    private companion object {
        /** Longer than the stack exit transition. */
        const val TEARDOWN_DELAY_MS = 500L
    }
}