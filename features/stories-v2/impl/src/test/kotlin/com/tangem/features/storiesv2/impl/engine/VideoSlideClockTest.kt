package com.tangem.features.storiesv2.impl.engine

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VideoSlideClockTest {

    private val frames = RenderedFrameFeed()
    private var playerPositionMs = 0L

    private val clock = VideoSlideClock(
        durationMs = DURATION_MS,
        frames = frames,
        playerPositionMs = { playerPositionMs },
    )

    @BeforeEach
    fun resetClock() {
        playerPositionMs = 0L
        clock.reset()
    }

    @Test
    fun `GIVEN no frame yet WHEN frame arrives THEN position comes from the player`() {
        // Arrange
        playerPositionMs = 420L

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 1_000), isRunning = true)

        // Assert — before the first frame is reported there is nothing on screen to follow.
        assertThat(position).isEqualTo(420L)
    }

    @Test
    fun `GIVEN a rendered frame WHEN read at its release time THEN position is that frame`() {
        // Arrange
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 5_000), isRunning = true)

        // Assert — the position is the frame the decoder is about to show, not a parallel timer's guess at it.
        assertThat(position).isEqualTo(1_000L)
    }

    @Test
    fun `GIVEN a rendered frame WHEN read later in the same frame interval THEN position is interpolated`() {
        // Arrange
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 5_030), isRunning = true)

        // Assert — the bar moves at display refresh rate rather than in video frame steps.
        assertThat(position).isEqualTo(1_030L)
    }

    @Test
    fun `GIVEN no new frames WHEN time passes THEN position stops instead of running ahead`() {
        // Arrange
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 6_000), isRunning = true)

        // Assert — a stalled decoder must not let the progress bar overtake the picture.
        assertThat(position).isEqualTo(1_048L)
    }

    @Test
    fun `GIVEN a stalled decoder WHEN the position stops moving THEN the stall is timed in wall time`() {
        // Arrange — one rendered frame and then nothing, while the frame loop keeps running normally.
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act — the position climbs for as long as interpolation is allowed to carry it and then holds.
        val stallStartMs = 5_000L + MAX_EXTRAPOLATION_MS
        runFrames(fromMs = 5_000L, toMs = 6_000L)

        // Assert — measured from the moment the picture stopped, not from a count of frames.
        assertThat(clock.stalledForMs).isEqualTo(6_000L - stallStartMs)
    }

    @Test
    fun `GIVEN a stalled decoder WHEN the frame loop itself breaks THEN the stall time starts over`() {
        // Arrange — a story sent to the background stops the frame loop, and the gap it leaves says nothing about
        // the decoder. Counting it would put a healthy video on its poster on every return.
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))
        runFrames(fromMs = 5_000L, toMs = 6_000L)

        // Act
        clock.onFrame(frameTimeNanos = nanos(ms = 36_000), isRunning = true)

        // Assert
        assertThat(clock.stalledForMs).isEqualTo(0L)
    }

    @Test
    fun `GIVEN dropped frames WHEN the position keeps moving THEN nothing is treated as a stall`() {
        // Arrange — media3 reports only the frames it releases, so a device dropping them reports none while both
        // the picture and the player's own position keep moving.
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act
        for (frameMs in 5_000L..6_000L step FRAME_MS) {
            playerPositionMs = 1_000L + (frameMs - 5_000L)
            clock.onFrame(frameTimeNanos = nanos(ms = frameMs), isRunning = true)
        }

        // Assert
        assertThat(clock.stalledForMs).isEqualTo(0L)
    }

    @Test
    fun `GIVEN a stalled decoder WHEN the frame feed is cleared THEN the stall is not cleared with it`() {
        // Arrange — a seek empties the feed, and an empty feed is the absence of a picture, not a new one.
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))
        runFrames(fromMs = 5_000L, toMs = 5_500L)

        // Act
        frames.reset()
        runFrames(fromMs = 5_500L, toMs = 6_000L)

        // Assert
        assertThat(clock.stalledForMs).isEqualTo(6_000L - (5_000L + MAX_EXTRAPOLATION_MS))
    }

    @Test
    fun `GIVEN no frame ever rendered WHEN the slide keeps running THEN the stall is reported`() {
        // Arrange — a decoder that never starts is the case the watchdog exists for.
        playerPositionMs = 0L

        // Act
        runFrames(fromMs = 1_000L, toMs = 1_500L)

        // Assert
        assertThat(clock.stalledForMs).isEqualTo(500L)
    }

    @Test
    fun `GIVEN a stalled decoder WHEN the slide is not running THEN the stall is not counted`() {
        // Arrange
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act — a held or backgrounded slide produces nothing by design.
        for (frameMs in 5_000L..6_000L step FRAME_MS) {
            clock.onFrame(frameTimeNanos = nanos(ms = frameMs), isRunning = false)
        }

        // Assert
        assertThat(clock.stalledForMs).isEqualTo(0L)
    }

    @Test
    fun `GIVEN a frame from another playlist item WHEN read THEN the player position arbitrates`() {
        // Arrange — around a transition or a seek the newest reported frame can still belong to the old item.
        playerPositionMs = 50L
        frames.onFrameRendered(presentationTimeUs = 6_900_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 5_000), isRunning = true)

        // Assert
        assertThat(position).isEqualTo(50L)
    }

    @Test
    fun `GIVEN an advanced position WHEN a frame reports an earlier one THEN the position does not go back`() {
        // Arrange
        playerPositionMs = 2_000L
        frames.onFrameRendered(presentationTimeUs = 2_000_000L, releaseTimeNanos = nanos(ms = 5_000))
        clock.onFrame(frameTimeNanos = nanos(ms = 5_000), isRunning = true)

        // Act
        playerPositionMs = 1_500L
        frames.onFrameRendered(presentationTimeUs = 1_500_000L, releaseTimeNanos = nanos(ms = 5_016))
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 5_016), isRunning = true)

        // Assert — a segment that dips backwards for a frame is the visible jump this clamp exists to prevent.
        assertThat(position).isEqualTo(2_000L)
    }

    @Test
    fun `GIVEN not running WHEN time passes THEN position holds at the last rendered frame`() {
        // Arrange
        playerPositionMs = 1_000L
        frames.onFrameRendered(presentationTimeUs = 1_000_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 9_000), isRunning = false)

        // Assert
        assertThat(position).isEqualTo(1_000L)
    }

    @Test
    fun `GIVEN a frame past the duration WHEN read THEN position stops at the duration`() {
        // Arrange
        playerPositionMs = DURATION_MS
        frames.onFrameRendered(presentationTimeUs = DURATION_MS * 1_000L, releaseTimeNanos = nanos(ms = 5_000))

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 5_200), isRunning = true)

        // Assert
        assertThat(position).isEqualTo(DURATION_MS)
    }

    @Test
    fun `GIVEN a played slide WHEN reset THEN both position and reported frames are cleared`() {
        // Arrange
        playerPositionMs = 2_000L
        frames.onFrameRendered(presentationTimeUs = 2_000_000L, releaseTimeNanos = nanos(ms = 5_000))
        clock.onFrame(frameTimeNanos = nanos(ms = 5_000), isRunning = true)

        // Act
        clock.reset()
        playerPositionMs = 0L
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 5_100), isRunning = true)

        // Assert
        assertThat(frames.latest()).isNull()
        assertThat(position).isEqualTo(0L)
    }

    private fun runFrames(fromMs: Long, toMs: Long) {
        for (frameMs in fromMs..toMs step FRAME_MS) {
            clock.onFrame(frameTimeNanos = nanos(ms = frameMs), isRunning = true)
        }
    }

    private companion object {
        const val DURATION_MS = 7_000L
        const val FRAME_MS = 4L
        const val MAX_EXTRAPOLATION_MS = 48L
    }
}

private fun nanos(ms: Long): Long = ms * NANOS_PER_MS