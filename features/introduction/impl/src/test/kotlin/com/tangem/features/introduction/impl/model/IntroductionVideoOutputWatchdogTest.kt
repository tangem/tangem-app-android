package com.tangem.features.introduction.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IntroductionVideoOutputWatchdogTest {

    private val videoPlayer: IntroductionVideoPlayer = mockk(relaxed = true)

    private var replacements = 0
    private var giveUps = 0

    @BeforeEach
    fun resetMocks() {
        clearMocks(videoPlayer)
        replacements = 0
        giveUps = 0
        every { videoPlayer.isSurfaceAttached } returns true
    }

    @Test
    fun `GIVEN playing with no frames coming out WHEN a second passes THEN a new surface is asked for`() = runTest {
        // Arrange
        every { videoPlayer.outputFrameCount } returns 28L
        val watchdog = createWatchdog()

        // Act
        watchdog.onPlayingChanged(isPlaying = true)
        advanceTimeBy(STALL_DETECTED_AFTER_MS)
        runCurrent()

        // Assert
        assertThat(replacements).isEqualTo(1)
    }

    @Test
    fun `GIVEN a stall cut short by a frame WHEN the output stalls again THEN it is timed from scratch`() =
        runTest {
            // Arrange
            every { videoPlayer.outputFrameCount } returnsMany listOf(10L, 10L, 10L, 10L, 11L)
            val watchdog = createWatchdog()

            // Act
            watchdog.onPlayingChanged(isPlaying = true)
            advanceTimeBy(CHECK_INTERVAL_MS * 8 + 1)
            runCurrent()
            val replacementsBeforeTimeout = replacements
            advanceTimeBy(CHECK_INTERVAL_MS)
            runCurrent()

            // Assert
            assertThat(replacementsBeforeTimeout).isEqualTo(0)
            assertThat(replacements).isEqualTo(1)
        }

    @Test
    fun `GIVEN the replacements spent WHEN the video plays well for a while THEN a later stall is replaced again`() =
        runTest {
            // Arrange
            var frames = 0L
            var isStalled = true
            every { videoPlayer.outputFrameCount } answers {
                if (!isStalled) frames += 6
                frames
            }
            val watchdog = createWatchdog()
            watchdog.onPlayingChanged(isPlaying = true)
            advanceTimeBy(THREE_REPLACEMENTS_AFTER_MS)
            runCurrent()
            val replacementsWhileStuck = replacements

            // Act
            isStalled = false
            advanceTimeBy(RECOVERED_AFTER_MS + CHECK_INTERVAL_MS)
            isStalled = true
            advanceTimeBy(STALL_DETECTED_AFTER_MS)
            runCurrent()

            // Assert
            assertThat(replacementsWhileStuck).isEqualTo(3)
            assertThat(replacements).isEqualTo(4)
            assertThat(giveUps).isEqualTo(0)
        }

    @Test
    fun `GIVEN the replacements spent WHEN the surface is gone for a while THEN they are not given back`() = runTest {
        // Arrange
        every { videoPlayer.outputFrameCount } returns 28L
        val watchdog = createWatchdog()
        watchdog.onPlayingChanged(isPlaying = true)
        advanceTimeBy(THREE_REPLACEMENTS_AFTER_MS)
        runCurrent()

        // Act
        every { videoPlayer.isSurfaceAttached } returns false
        advanceTimeBy(RECOVERED_AFTER_MS + CHECK_INTERVAL_MS)
        every { videoPlayer.isSurfaceAttached } returns true
        advanceTimeBy(STALL_DETECTED_AFTER_MS)
        runCurrent()

        // Assert
        assertThat(replacements).isEqualTo(3)
        assertThat(giveUps).isEqualTo(1)
    }

    @Test
    fun `GIVEN frames coming out WHEN time passes THEN the surface is kept`() = runTest {
        // Arrange
        var frames = 0L
        every { videoPlayer.outputFrameCount } answers { frames += 6; frames }
        val watchdog = createWatchdog()

        // Act
        watchdog.onPlayingChanged(isPlaying = true)
        advanceTimeBy(STALL_DETECTED_AFTER_MS * 4)
        runCurrent()

        // Assert
        assertThat(replacements).isEqualTo(0)
    }

    @Test
    fun `GIVEN the video stops playing WHEN no frames come out THEN the surface is kept`() = runTest {
        // Arrange
        every { videoPlayer.outputFrameCount } returns 28L
        val watchdog = createWatchdog()
        watchdog.onPlayingChanged(isPlaying = true)

        // Act
        watchdog.onPlayingChanged(isPlaying = false)
        advanceTimeBy(STALL_DETECTED_AFTER_MS * 4)
        runCurrent()

        // Assert
        assertThat(replacements).isEqualTo(0)
    }

    @Test
    fun `GIVEN no surface to draw into WHEN no frames come out THEN nothing is replaced`() = runTest {
        // Arrange
        every { videoPlayer.isSurfaceAttached } returns false
        every { videoPlayer.outputFrameCount } returns 28L
        val watchdog = createWatchdog()

        // Act
        watchdog.onPlayingChanged(isPlaying = true)
        advanceTimeBy(STALL_DETECTED_AFTER_MS * 4)
        runCurrent()

        // Assert
        assertThat(replacements).isEqualTo(0)
    }

    @Test
    fun `GIVEN no decoder yet WHEN the count is unknown THEN nothing is replaced`() = runTest {
        // Arrange
        every { videoPlayer.outputFrameCount } returns null
        val watchdog = createWatchdog()

        // Act
        watchdog.onPlayingChanged(isPlaying = true)
        advanceTimeBy(STALL_DETECTED_AFTER_MS * 4)
        runCurrent()

        // Assert
        assertThat(replacements).isEqualTo(0)
    }

    @Test
    fun `GIVEN a stall new surfaces do not cure WHEN it goes on THEN three are tried before the video is given up`() =
        runTest {
            // Arrange
            every { videoPlayer.outputFrameCount } returns 28L
            val watchdog = createWatchdog()

            // Act
            watchdog.onPlayingChanged(isPlaying = true)
            advanceTimeBy(STALL_DETECTED_AFTER_MS * 10)
            watchdog.onPlayingChanged(isPlaying = false)
            watchdog.onPlayingChanged(isPlaying = true)
            advanceTimeBy(STALL_DETECTED_AFTER_MS * 10)
            runCurrent()

            // Assert
            assertThat(replacements).isEqualTo(3)
            assertThat(giveUps).isEqualTo(1)
        }

    private fun TestScope.createWatchdog() = IntroductionVideoOutputWatchdog(
        scope = backgroundScope,
        videoPlayer = videoPlayer,
        onReplaceSurface = { replacements++ },
        onGiveUp = { giveUps++ },
    )

    private companion object {
        const val CHECK_INTERVAL_MS = 250L

        /** The first check only takes the baseline, then four more checks 250 ms apart see the count stand still. */
        const val STALL_DETECTED_AFTER_MS = 1_251L

        const val RECOVERED_AFTER_MS = 10_000L

        /** Stalls are noticed 1.25, 2.25 and 3.25 s in; the next one, at 4.25 s, would be the give-up. */
        const val THREE_REPLACEMENTS_AFTER_MS = 3_500L
    }
}