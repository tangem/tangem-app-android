package com.tangem.features.introduction.impl.engine

import android.os.Handler
import android.os.Looper

/**
 * Where the blocking half of a video teardown runs.
 *
 * Letting go of a surface makes media3 wait for the renderer to release it — up to the two seconds its builder
 * defaults to — and Compose disposes the view while the screen it belongs to is still animating away, so the wait
 * lands on the frames of that animation.
 */
internal interface VideoTeardownScheduler {

    fun schedule(action: () -> Unit)

    /** Whatever was scheduled is no longer wanted: the screen came back before it ran. */
    fun cancel()
}

internal class MainThreadVideoTeardownScheduler : VideoTeardownScheduler {

    private val handler = Handler(Looper.getMainLooper())

    override fun schedule(action: () -> Unit) {
        handler.postDelayed(Runnable { action() }, TEARDOWN_DELAY_MS)
    }

    override fun cancel() {
        handler.removeCallbacksAndMessages(null)
    }

    private companion object {
        /** Longer than the stack transition that plays while the screen is being disposed. */
        const val TEARDOWN_DELAY_MS = 500L
    }
}