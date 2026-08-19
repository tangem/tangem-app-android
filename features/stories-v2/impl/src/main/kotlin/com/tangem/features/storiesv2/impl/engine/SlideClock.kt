package com.tangem.features.storiesv2.impl.engine

import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

internal const val NANOS_PER_MS = 1_000_000L

/**
 * The largest gap between two frames that is still a dropped frame or two rather than a break in the frame loop.
 * A longer one belongs to neither the position, nor the watched time, nor the stall budget.
 */
internal const val MAX_FRAME_STEP_NANOS = 250L * NANOS_PER_MS

/**
 * Tells the player how far into the current slide it is: the progress segment, the haptic markers and the moment
 * the slide ends all hang off this one number.
 *
 * Deliberately not a timer — a timer and a decoder drift apart, and that drift is what reads as the progress bar
 * lying about the video.
 */
internal interface SlideClock {

    /**
     * How long the slide has been running without its position moving, in wall time rather than in frames: a
     * starving decoder usually janks the main thread with it, and a budget spent in frames would stretch too.
     */
    val stalledForMs: Long

    /**
     * @param isRunning `false` while held, backgrounded or finished. A clock that is not running holds its
     *                  position rather than catching up.
     */
    fun onFrame(frameTimeNanos: Long, isRunning: Boolean): Long

    fun reset()
}

/**
 * Position for content with no playback position of its own: images, Lottie compositions and slides degraded to
 * their poster. Progress follows the duration from the composition.
 */
internal class WallSlideClock(
    private val durationMs: Long,
    startPositionMs: Long = 0L,
) : SlideClock {

    // Nanoseconds, not milliseconds: truncating each frame's delta loses 0.667 ms of every 16.667 at 60 Hz, which
    // stretches a five second slide past five and a quarter next to a frame-accurate video slide.
    private val durationNanos = durationMs * NANOS_PER_MS
    private var positionNanos = startPositionMs * NANOS_PER_MS
    private var previousFrameNanos = NO_TIME

    override val stalledForMs: Long get() = 0L

    override fun onFrame(frameTimeNanos: Long, isRunning: Boolean): Long {
        val previous = previousFrameNanos
        previousFrameNanos = frameTimeNanos

        if (!isRunning || previous == NO_TIME) return positionNanos / NANOS_PER_MS

        val deltaNanos = frameTimeNanos - previous
        if (deltaNanos in 0..MAX_FRAME_STEP_NANOS) {
            positionNanos = (positionNanos + deltaNanos).coerceAtMost(durationNanos)
        }
        return positionNanos / NANOS_PER_MS
    }

    override fun reset() {
        positionNanos = 0L
        previousFrameNanos = NO_TIME
    }
}

/**
 * Position of a video slide, taken from the frames the decoder actually put on screen.
 *
 * Media3 reports every frame just before releasing it to the surface, with its presentation time and the instant it
 * is due. Driving the bar from that pair means a stalled decoder stops the bar together with the picture, which is
 * the required behaviour rather than a bug to work around. Between two decoded frames the position is interpolated
 * so the bar moves at display refresh rate, capped so it can never run ahead of the picture.
 *
 * @property playerPositionMs arbitrates: a reported frame disagreeing with it by more than a stumble belongs to
 *                            another playlist item, which happens for a frame or two around a transition or a seek.
 */
internal class VideoSlideClock(
    private val durationMs: Long,
    private val frames: RenderedFrameFeed,
    private val playerPositionMs: () -> Long,
) : SlideClock {

    private var positionMs = 0L
    private var lastFrameNanos = NO_TIME
    private var stalledForNanos = 0L

    override val stalledForMs: Long get() = stalledForNanos / NANOS_PER_MS

    override fun onFrame(frameTimeNanos: Long, isRunning: Boolean): Long {
        val frame = frames.latest()
        val playerPosition = playerPositionMs()

        val candidate = when {
            frame == null -> playerPosition
            abs(frame.presentationTimeMs - playerPosition) > MAX_DIVERGENCE_MS -> playerPosition
            isRunning -> frame.presentationTimeMs + extrapolationMs(frame, frameTimeNanos)
            else -> frame.presentationTimeMs
        }

        // Monotonic on purpose: right after a transition the next item's first frames report a position near zero,
        // and a bar dipping back before the slide switches is the visible jump this design exists to avoid.
        val previousPositionMs = positionMs
        positionMs = maxOf(positionMs, candidate.coerceIn(0L, durationMs))

        updateStall(
            frameTimeNanos = frameTimeNanos,
            isRunning = isRunning,
            hasAdvanced = positionMs > previousPositionMs,
        )
        return positionMs
    }

    override fun reset() {
        positionMs = 0L
        lastFrameNanos = NO_TIME
        stalledForNanos = 0L
        frames.reset()
    }

    private fun extrapolationMs(frame: RenderedFrame, frameTimeNanos: Long): Long {
        val sinceReleaseMs = (frameTimeNanos - frame.releaseTimeNanos) / NANOS_PER_MS
        return sinceReleaseMs.coerceIn(0L, MAX_EXTRAPOLATION_MS)
    }

    /**
     * Spent on the position standing still, not on frames failing to arrive: media3 reports only the frames it
     * releases, so a device dropping them reports none while both the picture and the position keep moving.
     */
    private fun updateStall(frameTimeNanos: Long, isRunning: Boolean, hasAdvanced: Boolean) {
        val previousFrameNanos = lastFrameNanos
        lastFrameNanos = frameTimeNanos

        val deltaNanos = frameTimeNanos - previousFrameNanos
        // A frame loop that stopped says nothing about the decoder — the story was away, or the main thread stalled.
        val isFrameLoopBreak = previousFrameNanos == NO_TIME || deltaNanos !in 0..MAX_FRAME_STEP_NANOS

        stalledForNanos = if (!isRunning || hasAdvanced || isFrameLoopBreak) 0L else stalledForNanos + deltaNanos
    }

    private companion object {
        const val MAX_EXTRAPOLATION_MS = 48L
        const val MAX_DIVERGENCE_MS = 400L
    }
}

/** A frame media3 is about to put on the surface. */
internal data class RenderedFrame(
    val presentationTimeMs: Long,
    val releaseTimeNanos: Long,
)

/**
 * Hand-off from the playback thread to the frame loop: the latest frame is published as one immutable value rather
 * than as fields that could be read torn.
 */
internal class RenderedFrameFeed {

    private val latest = AtomicReference<RenderedFrame?>(null)

    /** Called on the playback thread, once per frame. */
    fun onFrameRendered(presentationTimeUs: Long, releaseTimeNanos: Long) {
        latest.set(
            RenderedFrame(
                presentationTimeMs = presentationTimeUs / MICROS_PER_MS,
                releaseTimeNanos = releaseTimeNanos,
            ),
        )
    }

    fun latest(): RenderedFrame? = latest.get()

    fun reset() {
        latest.set(null)
    }

    private companion object {
        const val MICROS_PER_MS = 1_000L
    }
}

private const val NO_TIME = Long.MIN_VALUE