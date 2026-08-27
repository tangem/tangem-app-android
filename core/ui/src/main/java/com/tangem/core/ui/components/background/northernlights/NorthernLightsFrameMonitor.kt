package com.tangem.core.ui.components.background.northernlights

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tangem.core.ui.components.background.SHADER_FRAME_INTERVAL_MILLIS

/**
 * Process-wide guard that downgrades [NorthernLightsBackground] to the simple variant on devices
 * that can't sustain the shader. The background is pure decoration, so sustained slow rendering
 * while it is visible — whatever the cause — is a reason to drop the expensive variant.
 *
 * Once tripped, [isDegraded] stays `true` for the rest of the process: the shader is re-evaluated
 * on the next app launch, not within the session.
 */
internal object NorthernLightsFrameMonitor {

    /** Frames skipped after (re)entering composition — covers shader compilation and screen entry animations. */
    private const val WARMUP_FRAMES = 30

    private const val WINDOW_FRAMES = 90

    /**
     * 1.5x the shader's own redraw interval (~50ms): "slow" means failing the shader's 30fps content
     * rate, not the panel's vsync rate. Adaptive (LTPO) panels may legitimately lower their refresh
     * rate to ~30Hz to match the throttled shader, producing healthy ~33ms frame callbacks — a
     * threshold tied to the panel refresh rate would misread that as 100% dropped frames.
     */
    private const val SLOW_FRAME_THRESHOLD_NANOS = SHADER_FRAME_INTERVAL_MILLIS * 1_500_000L

    private const val SLOW_FRAMES_TO_DEGRADE = WINDOW_FRAMES / 2

    /** A single bad window can still be cold-start jank (balance loading, list building) — require two in a row. */
    private const val SLOW_WINDOWS_TO_DEGRADE = 2

    var isDegraded by mutableStateOf(false)
        private set

    /**
     * Observes frame intervals while the shader background is composed and trips [isDegraded] when
     * a majority of [SLOW_WINDOWS_TO_DEGRADE] consecutive [WINDOW_FRAMES] windows misses the frame
     * budget. Windows are evaluated independently and a good window resets the streak, so a short
     * jank spike (cold start, heavy list load, navigation) can't accumulate into a false positive.
     * Suspends until cancelled or degraded.
     */
    suspend fun watch() {
        var previousFrameNanos = -1L
        var warmupLeft = WARMUP_FRAMES
        var evaluated = 0
        var slow = 0
        var slowWindows = 0
        while (!isDegraded) {
            withInfiniteAnimationFrameNanos { frameNanos ->
                val delta = if (previousFrameNanos > 0) frameNanos - previousFrameNanos else -1L
                previousFrameNanos = frameNanos
                when {
                    delta < 0 -> Unit
                    warmupLeft > 0 -> warmupLeft--
                    else -> {
                        evaluated++
                        if (delta > SLOW_FRAME_THRESHOLD_NANOS) slow++
                    }
                }
            }
            if (evaluated >= WINDOW_FRAMES) {
                slowWindows = if (slow >= SLOW_FRAMES_TO_DEGRADE) slowWindows + 1 else 0
                if (slowWindows >= SLOW_WINDOWS_TO_DEGRADE) {
                    isDegraded = true
                }
                evaluated = 0
                slow = 0
            }
        }
    }
}