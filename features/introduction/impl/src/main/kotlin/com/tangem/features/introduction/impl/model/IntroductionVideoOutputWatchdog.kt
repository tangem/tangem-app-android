package com.tangem.features.introduction.impl.model

import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * On some Samsung devices the decoder's output gets stuck right after launch: the player stays ready and its clock
 * runs, so the welcome text keeps moving, but no frame reaches the screen until the video gets a new surface
 * ([REDACTED_TASK_KEY]). Watches the output while the video plays, asks for that surface, and gives the video up when new
 * surfaces do not help.
 */
internal class IntroductionVideoOutputWatchdog(
    private val scope: CoroutineScope,
    private val videoPlayer: IntroductionVideoPlayer,
    private val onReplaceSurface: () -> Unit,
    private val onGiveUp: (Throwable) -> Unit,
) {

    private var watchJob: Job? = null
    private var replacements = 0
    private var hasGivenUp = false

    fun onPlayingChanged(isPlaying: Boolean) {
        watchJob?.cancel()
        watchJob = if (isPlaying && !hasGivenUp) scope.launch { watch() } else null
    }

    private suspend fun watch() {
        var lastCount: Long? = null
        var stalledMs = 0L
        var flowingMs = 0L
        while (true) {
            delay(CHECK_INTERVAL_MS)
            val count = videoPlayer.outputFrameCount
            val previousCount = lastCount
            lastCount = count
            when {
                // Nothing to judge: there is no decoder yet, or no surface for it to fill.
                count == null || !videoPlayer.isSurfaceAttached -> {
                    stalledMs = 0L
                    flowingMs = 0L
                }
                count != previousCount -> {
                    stalledMs = 0L
                    // Only frames that really came out earn the replacements back.
                    if (previousCount != null) flowingMs += CHECK_INTERVAL_MS
                    if (flowingMs >= RECOVERED_AFTER_MS) replacements = 0
                }
                else -> {
                    flowingMs = 0L
                    stalledMs += CHECK_INTERVAL_MS
                    if (stalledMs < STALL_TIMEOUT_MS) continue
                    stalledMs = 0L
                    if (!onStall(frameCount = count)) return
                }
            }
        }
    }

    /** Returns whether the video is still worth watching. */
    private fun onStall(frameCount: Long): Boolean {
        if (replacements == MAX_REPLACEMENTS) {
            hasGivenUp = true
            onGiveUp(IllegalStateException("Welcome video output stalled through $MAX_REPLACEMENTS new surfaces"))
            return false
        }
        replacements++
        TangemLogger.e(
            "Welcome video output stalled after $frameCount frames at ${videoPlayer.positionMs}ms, " +
                "replacing the surface ($replacements of $MAX_REPLACEMENTS)",
        )
        onReplaceSurface()
        return true
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 250L

        /** Two dozen frame intervals of a 24 fps loop with nothing coming out. */
        const val STALL_TIMEOUT_MS = 1_000L

        /** A stall that this many new surfaces in a row do not cure is not this one, so the video is given up. */
        const val MAX_REPLACEMENTS = 3

        /** Playing this long without a stall gives the replacements back: whatever stalls next is a new stall. */
        const val RECOVERED_AFTER_MS = 10_000L
    }
}