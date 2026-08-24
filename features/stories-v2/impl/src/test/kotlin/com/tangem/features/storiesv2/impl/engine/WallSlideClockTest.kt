package com.tangem.features.storiesv2.impl.engine

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class WallSlideClockTest {

    @Test
    fun `GIVEN fresh clock WHEN first frame THEN position stays at zero`() {
        // Arrange
        val clock = WallSlideClock(durationMs = 5_000L)

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 100), isRunning = true)

        // Assert — the first frame only establishes the baseline; there is no previous frame to measure against.
        assertThat(position).isEqualTo(0L)
    }

    @Test
    fun `GIVEN running clock WHEN frames advance THEN position follows wall time`() {
        // Arrange
        val clock = WallSlideClock(durationMs = 5_000L)
        clock.onFrame(frameTimeNanos = nanos(ms = 0), isRunning = true)

        // Act
        clock.onFrame(frameTimeNanos = nanos(ms = 16), isRunning = true)
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 32), isRunning = true)

        // Assert
        assertThat(position).isEqualTo(32L)
    }

    @Test
    fun `GIVEN not running WHEN frames pass THEN position does not move`() {
        // Arrange
        val clock = WallSlideClock(durationMs = 5_000L)
        clock.onFrame(frameTimeNanos = nanos(ms = 0), isRunning = true)
        clock.onFrame(frameTimeNanos = nanos(ms = 100), isRunning = true)

        // Act
        val whilePaused = clock.onFrame(frameTimeNanos = nanos(ms = 5_000), isRunning = false)
        val afterResume = clock.onFrame(frameTimeNanos = nanos(ms = 5_016), isRunning = true)

        // Assert — a pause holds the position and resumes from it instead of catching up on the time away.
        assertThat(whilePaused).isEqualTo(100L)
        assertThat(afterResume).isEqualTo(116L)
    }

    @Test
    fun `GIVEN a frame gap far beyond a dropped frame WHEN running THEN the gap is not counted`() {
        // Arrange
        val clock = WallSlideClock(durationMs = 5_000L)
        clock.onFrame(frameTimeNanos = nanos(ms = 0), isRunning = true)
        clock.onFrame(frameTimeNanos = nanos(ms = 50), isRunning = true)

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 3_000), isRunning = true)

        // Assert — the app was away rather than the slide having run on unobserved.
        assertThat(position).isEqualTo(50L)
    }

    @Test
    fun `GIVEN position past the duration WHEN frames advance THEN position stops at the duration`() {
        // Arrange
        val clock = WallSlideClock(durationMs = 100L)
        clock.onFrame(frameTimeNanos = nanos(ms = 0), isRunning = true)

        // Act
        clock.onFrame(frameTimeNanos = nanos(ms = 80), isRunning = true)
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 160), isRunning = true)

        // Assert
        assertThat(position).isEqualTo(100L)
    }

    @Test
    fun `GIVEN sixty hertz frames WHEN a whole second passes THEN the position does not drift`() {
        // Arrange — a frame is 16.667 ms, so truncating each delta to whole milliseconds would lose 4% a second.
        val clock = WallSlideClock(durationMs = 5_000L)

        // Act
        var position = 0L
        repeat(times = 61) { frame ->
            position = clock.onFrame(frameTimeNanos = frame * NANOS_PER_SECOND / 60L, isRunning = true)
        }

        // Assert
        assertThat(position).isEqualTo(1_000L)
    }

    @Test
    fun `GIVEN a start position WHEN clock runs THEN it continues from there`() {
        // Arrange — a slide that fell back to its poster mid-playback keeps the progress it had.
        val clock = WallSlideClock(durationMs = 5_000L, startPositionMs = 1_200L)
        clock.onFrame(frameTimeNanos = nanos(ms = 0), isRunning = true)

        // Act
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 100), isRunning = true)

        // Assert
        assertThat(position).isEqualTo(1_300L)
    }

    @Test
    fun `GIVEN advanced clock WHEN reset THEN position returns to zero`() {
        // Arrange
        val clock = WallSlideClock(durationMs = 5_000L, startPositionMs = 1_000L)
        clock.onFrame(frameTimeNanos = nanos(ms = 0), isRunning = true)
        clock.onFrame(frameTimeNanos = nanos(ms = 100), isRunning = true)

        // Act
        clock.reset()
        val position = clock.onFrame(frameTimeNanos = nanos(ms = 200), isRunning = true)

        // Assert
        assertThat(position).isEqualTo(0L)
    }
}

private const val NANOS_PER_SECOND = 1_000_000_000L

private fun nanos(ms: Long): Long = ms * NANOS_PER_MS