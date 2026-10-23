package com.tangem.core.media

import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * On some Samsung devices the decoder output gets stuck after launch while the clock keeps running, and only a new
 * surface brings frames back ([REDACTED_TASK_KEY]). Asks for one on every stall, up to [MAX_REPLACEMENTS], then gives up.
 */
internal class VideoOutputWatchdog(
    private val scope: CoroutineScope,
    private val videoPlayer: LoopingVideoPlayer,
    private val subject: String,
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
                count == null || !videoPlayer.isSurfaceAttached -> {
                    stalledMs = 0L
                    flowingMs = 0L
                }
                count != previousCount -> {
                    stalledMs = 0L
                    // The first count after none is a baseline, not frames coming out.
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
            onGiveUp(IllegalStateException("$subject output stalled through $MAX_REPLACEMENTS new surfaces"))
            return false
        }
        replacements++
        TangemLogger.e(
            "$subject output stalled after $frameCount frames at ${videoPlayer.positionMs}ms, " +
                "replacing the surface ($replacements of $MAX_REPLACEMENTS)",
        )
        onReplaceSurface()
        return true
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 250L
        const val STALL_TIMEOUT_MS = 1_000L
        const val MAX_REPLACEMENTS = 3
        const val RECOVERED_AFTER_MS = 10_000L
    }
}