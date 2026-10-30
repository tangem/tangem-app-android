package com.tangem.core.media

import com.google.common.truth.Truth.assertThat
import com.tangem.core.media.OneShotVideoController.CueReason
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class OneShotVideoControllerTest {

    private val player: OneShotVideoPlayer = mockk(relaxed = true)
    private val cache: RemoteClipCache = mockk(relaxed = true)

    private var heardPlayerListener: OneShotVideoPlayer.Listener? = null
    private val playerListener: OneShotVideoPlayer.Listener
        get() = checkNotNull(heardPlayerListener) { "no controller was created in this test" }

    private val cues = mutableListOf<CueReason>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(player, cache)
        heardPlayerListener = null
        cues.clear()
        every { cache.isFullyCached(any()) } returns false
    }

    @Nested
    inner class Prepare {

        @Test
        fun `GIVEN no clip WHEN prepared THEN it failed without a download`() = runTest {
            // Arrange
            val controller = createController()

            // Act
            controller.prepare(url = null, cueMs = CUE_MS)

            // Assert
            assertThat(controller.loadState.value).isEqualTo(VideoLoadState.Failed)
            coVerify(exactly = 0) { cache.download(any()) }
        }

        @Test
        fun `GIVEN animations off system-wide WHEN prepared THEN it failed without a download`() = runTest {
            // Arrange
            val controller = createController(isMotionEnabled = false)

            // Act
            controller.prepare(url = URL, cueMs = CUE_MS)

            // Assert
            assertThat(controller.loadState.value).isEqualTo(VideoLoadState.Failed)
            coVerify(exactly = 0) { cache.download(any()) }
        }

        @Test
        fun `GIVEN a clip not cached yet WHEN prepared THEN it is downloaded before the player prepares it`() =
            runTest {
                // Arrange
                val controller = createController()

                // Act
                controller.prepare(url = URL, cueMs = CUE_MS)

                // Assert
                coVerifyOrder {
                    player.stop()
                    cache.download(URL)
                    player.prepare(url = URL, cueMs = CUE_MS)
                }
                assertThat(controller.loadState.value).isEqualTo(VideoLoadState.Loading)
            }

        @Test
        fun `GIVEN a cached clip WHEN prepared THEN it is prepared without a download`() = runTest {
            // Arrange
            every { cache.isFullyCached(URL) } returns true
            val controller = createController()

            // Act
            controller.prepare(url = URL, cueMs = CUE_MS)

            // Assert
            coVerify(exactly = 0) { cache.download(any()) }
            verify(exactly = 1) { player.prepare(url = URL, cueMs = CUE_MS) }
        }

        @Test
        fun `GIVEN the download fails WHEN prepared THEN it failed and nothing is prepared`() = runTest {
            // Arrange
            coEvery { cache.download(URL) } throws IllegalStateException("offline")
            val controller = createController()

            // Act
            controller.prepare(url = URL, cueMs = CUE_MS)

            // Assert
            assertThat(controller.loadState.value).isEqualTo(VideoLoadState.Failed)
            verify(exactly = 0) { player.prepare(any(), any()) }
        }

        @Test
        fun `GIVEN a download in flight WHEN prepare is cancelled THEN it is still loading and nothing is prepared`() =
            runTest {
                // Arrange
                coEvery { cache.download(URL) } coAnswers { CompletableDeferred<Unit>().await() }
                val controller = createController()
                val preparing = launch { controller.prepare(url = URL, cueMs = CUE_MS) }
                runCurrent()

                // Act
                preparing.cancel()
                runCurrent()

                // Assert
                assertThat(controller.loadState.value).isEqualTo(VideoLoadState.Loading)
                verify(exactly = 0) { player.prepare(any(), any()) }
            }

        @Test
        fun `GIVEN started during the download WHEN the download completes THEN the clip is not prepared`() = runTest {
            // Arrange
            val downloaded = CompletableDeferred<Unit>()
            coEvery { cache.download(URL) } coAnswers { downloaded.await() }
            val controller = createController()
            launch { controller.prepare(url = URL, cueMs = CUE_MS) }
            runCurrent()
            controller.start()

            // Act
            downloaded.complete(Unit)
            runCurrent()

            // Assert
            verify(exactly = 0) { player.prepare(any(), any()) }
            assertThat(cues).containsExactly(CueReason.NotReady)
        }

        @Test
        fun `GIVEN a ready clip WHEN another one is prepared THEN it is loading until that one reports ready`() =
            runTest {
                // Arrange
                val controller = createReadyController()

                // Act
                controller.prepare(url = OTHER_URL, cueMs = CUE_MS)
                val stateBeforeReady = controller.loadState.value
                playerListener.onReady()

                // Assert
                assertThat(stateBeforeReady).isEqualTo(VideoLoadState.Loading)
                assertThat(controller.loadState.value).isEqualTo(VideoLoadState.Ready)
            }

        @Test
        fun `GIVEN started WHEN another clip is prepared THEN the playing one is kept`() = runTest {
            // Arrange
            val controller = createReadyController()
            controller.start()
            clearMocks(player, answers = false)

            // Act
            controller.prepare(url = OTHER_URL, cueMs = CUE_MS)

            // Assert
            verify(exactly = 0) { player.stop() }
            verify(exactly = 0) { player.prepare(any(), any()) }
        }
    }

    @Nested
    inner class Start {

        @Test
        fun `GIVEN a ready clip WHEN started THEN it plays and the cue waits for its frame`() = runTest {
            // Arrange
            val controller = createReadyController()

            // Act
            controller.start()

            // Assert
            verify(exactly = 1) { player.play() }
            assertThat(cues).isEmpty()
        }

        @Test
        fun `GIVEN no ready clip WHEN started THEN the cue is reported at once and nothing plays`() = runTest {
            // Arrange
            val controller = createController()

            // Act
            controller.start()

            // Assert
            assertThat(cues).containsExactly(CueReason.NotReady)
            verify(exactly = 0) { player.play() }
        }

        @Test
        fun `GIVEN started WHEN started again THEN it plays once`() = runTest {
            // Arrange
            val controller = createReadyController()
            controller.start()

            // Act
            controller.start()

            // Assert
            verify(exactly = 1) { player.play() }
        }

        @Test
        fun `GIVEN the screen in the background WHEN started THEN nothing plays`() = runTest {
            // Arrange
            val controller = createReadyController()
            controller.setRunning(isRunning = false)

            // Act
            controller.start()

            // Assert
            verify(exactly = 0) { player.play() }
        }

        @Test
        fun `GIVEN started in the background WHEN the screen comes back THEN the clip plays and stalls are watched`() =
            runTest {
                // Arrange
                every { player.presentedFrameCount } returns FRAMES
                val controller = createReadyController()
                controller.setRunning(isRunning = false)
                controller.start()

                // Act
                controller.setRunning(isRunning = true)
                advanceTimeBy(STALL_TIMEOUT_MS)
                runCurrent()

                // Assert
                verify(exactly = 1) { player.play() }
                assertThat(cues).containsExactly(CueReason.Stalled)
            }

        @Test
        fun `GIVEN started in the background WHEN no frames come out THEN it is not a stall`() = runTest {
            // Arrange
            every { player.presentedFrameCount } returns FRAMES
            val controller = createReadyController()
            controller.setRunning(isRunning = false)

            // Act
            controller.start()
            advanceTimeBy(STALL_TIMEOUT_MS * 3)
            runCurrent()

            // Assert
            assertThat(cues).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Cue {

        @Test
        fun `GIVEN started WHEN the cue frame is shown THEN it is reported once and the clip keeps playing`() =
            runTest {
                // Arrange
                createStartedController()

                // Act
                playerListener.onCueFramePresented()
                playerListener.onCueFramePresented()

                // Assert
                assertThat(cues).containsExactly(CueReason.Shown)
                verify(exactly = 0) { player.pause() }
            }

        @Test
        fun `GIVEN not started WHEN the cue frame is shown THEN nothing is reported`() = runTest {
            // Arrange
            createReadyController()

            // Act
            playerListener.onCueFramePresented()

            // Assert
            assertThat(cues).isEmpty()
        }

        @Test
        fun `GIVEN not started WHEN playback fails THEN the clip failed and no cue is reported`() = runTest {
            // Arrange
            val controller = createReadyController()

            // Act
            playerListener.onError(PLAYBACK_ERROR)

            // Assert
            assertThat(controller.loadState.value).isEqualTo(VideoLoadState.Failed)
            assertThat(cues).isEmpty()
        }

        @Test
        fun `GIVEN started and no cue frame shown WHEN the clip ends THEN the cue is reported anyway`() = runTest {
            // Arrange
            createStartedController()

            // Act
            playerListener.onEnded()

            // Assert
            assertThat(cues).containsExactly(CueReason.Ended)
        }

        @Test
        fun `GIVEN started WHEN playback fails THEN the cue is reported and the clip failed`() = runTest {
            // Arrange
            val controller = createStartedController()

            // Act
            playerListener.onError(PLAYBACK_ERROR)

            // Assert
            assertThat(cues).containsExactly(CueReason.Failed)
            assertThat(controller.loadState.value).isEqualTo(VideoLoadState.Failed)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the cue reported WHEN the clip stops THEN no second cue and coming back does not resume it`(
            model: StopModel,
        ) = runTest {
            // Arrange
            val controller = createStartedController()
            playerListener.onCueFramePresented()
            controller.setRunning(isRunning = false)
            clearMocks(player, answers = false)

            // Act
            model.stop(playerListener)
            controller.setRunning(isRunning = true)

            // Assert
            assertThat(cues).containsExactly(CueReason.Shown)
            verify(exactly = 0) { player.play() }
        }

        private fun provideTestModels() = listOf(
            StopModel(name = "ends", stop = { listener -> listener.onEnded() }),
            StopModel(name = "fails", stop = { listener -> listener.onError(PLAYBACK_ERROR) }),
        )
    }

    @Nested
    inner class Stall {

        @Test
        fun `GIVEN playing WHEN no new frame comes out for three seconds THEN it gives up on the clip`() = runTest {
            // Arrange
            every { player.presentedFrameCount } returns FRAMES
            createStartedController()

            // Act
            advanceTimeBy(STALL_TIMEOUT_MS)
            runCurrent()

            // Assert
            assertThat(cues).containsExactly(CueReason.Stalled)
            verify(exactly = 1) { player.pause() }
        }

        @Test
        fun `GIVEN frames coming out WHEN time passes THEN the clip plays on`() = runTest {
            // Arrange
            var frames = 0L
            every { player.presentedFrameCount } answers { ++frames }
            createStartedController()

            // Act
            advanceTimeBy(STALL_TIMEOUT_MS * 3)
            runCurrent()

            // Assert
            assertThat(cues).isEmpty()
        }

        @Test
        fun `GIVEN paused in the background WHEN no frames come out THEN it is not a stall`() = runTest {
            // Arrange
            every { player.presentedFrameCount } returns FRAMES
            val controller = createStartedController()

            // Act
            controller.setRunning(isRunning = false)
            advanceTimeBy(STALL_TIMEOUT_MS * 3)
            runCurrent()

            // Assert
            assertThat(cues).isEmpty()
        }

        @Test
        fun `GIVEN a pause in the middle of a stall WHEN the screen comes back THEN the stall is timed from scratch`() =
            runTest {
                // Arrange
                every { player.presentedFrameCount } returns FRAMES
                val controller = createStartedController()
                advanceTimeBy(STALL_TIMEOUT_MS - 1_000L)
                controller.setRunning(isRunning = false)

                // Act
                controller.setRunning(isRunning = true)
                advanceTimeBy(STALL_TIMEOUT_MS - 1_000L)
                runCurrent()
                val cuesBeforeFullStall = cues.toList()
                advanceTimeBy(1_000L)
                runCurrent()

                // Assert
                assertThat(cuesBeforeFullStall).isEmpty()
                assertThat(cues).containsExactly(CueReason.Stalled)
            }

        @Test
        fun `GIVEN playing WHEN the cue frame is shown THEN stalls are no longer watched`() = runTest {
            // Arrange
            every { player.presentedFrameCount } returns FRAMES
            createStartedController()

            // Act
            playerListener.onCueFramePresented()
            runCurrent()

            // Assert
            assertThat(isStallWatched()).isFalse()
        }
    }

    @Nested
    inner class SkipTo {

        @Test
        fun `GIVEN playing before the position WHEN skipped THEN it jumps there muted`() = runTest {
            // Arrange
            every { player.positionMs } returns 2_000L
            val controller = createStartedController()

            // Act
            val isSkipped = controller.skipTo(positionMs = SKIP_TO_MS)

            // Assert
            assertThat(isSkipped).isTrue()
            verifyOrder {
                player.seekTo(SKIP_TO_MS)
                player.mute()
            }
        }

        @Test
        fun `GIVEN playing past the position WHEN skipped THEN nothing is rewound`() = runTest {
            // Arrange
            every { player.positionMs } returns 6_500L
            val controller = createStartedController()

            // Act
            val isSkipped = controller.skipTo(positionMs = SKIP_TO_MS)

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { player.seekTo(any()) }
        }

        @Test
        fun `GIVEN the cue already reported WHEN skipped THEN nothing is seeked`() = runTest {
            // Arrange
            every { player.positionMs } returns 2_000L
            val controller = createStartedController()
            playerListener.onCueFramePresented()

            // Act
            val isSkipped = controller.skipTo(positionMs = SKIP_TO_MS)

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { player.seekTo(any()) }
        }

        @Test
        fun `GIVEN a clip given up on WHEN skipped THEN nothing is seeked`() = runTest {
            // Arrange
            every { player.positionMs } returns 2_000L
            val controller = createStartedController()
            playerListener.onError(PLAYBACK_ERROR)

            // Act
            val isSkipped = controller.skipTo(positionMs = SKIP_TO_MS)

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { player.seekTo(any()) }
        }

        @Test
        fun `GIVEN not started WHEN skipped THEN nothing is seeked`() = runTest {
            // Arrange
            every { player.positionMs } returns 2_000L
            val controller = createReadyController()

            // Act
            val isSkipped = controller.skipTo(positionMs = SKIP_TO_MS)

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { player.seekTo(any()) }
        }
    }

    @Nested
    inner class SetRunning {

        @Test
        fun `GIVEN playing past the cue WHEN the screen leaves and comes back THEN the clip pauses and goes on`() =
            runTest {
                // Arrange
                val controller = createStartedController()
                playerListener.onCueFramePresented()

                // Act
                controller.setRunning(isRunning = false)
                controller.setRunning(isRunning = true)

                // Assert
                verifyOrder {
                    player.play()
                    player.pause()
                    player.play()
                }
            }

        @Test
        fun `GIVEN the screen in front WHEN it reports being in front again THEN the stall clock keeps counting`() =
            runTest {
                // Arrange
                every { player.presentedFrameCount } returns FRAMES
                val controller = createStartedController()
                advanceTimeBy(STALL_TIMEOUT_MS - 1_000L)

                // Act
                controller.setRunning(isRunning = true)
                advanceTimeBy(1_000L)
                runCurrent()

                // Assert
                assertThat(cues).containsExactly(CueReason.Stalled)
                verify(exactly = 1) { player.play() }
            }

        @Test
        fun `GIVEN not started WHEN the screen comes back THEN nothing plays`() = runTest {
            // Arrange
            val controller = createReadyController()
            controller.setRunning(isRunning = false)

            // Act
            controller.setRunning(isRunning = true)

            // Assert
            verify(exactly = 0) { player.play() }
        }

        @Test
        fun `GIVEN a clip given up on WHEN the screen comes back THEN it stays stopped`() = runTest {
            // Arrange
            val controller = createStartedController()
            playerListener.onError(PLAYBACK_ERROR)
            controller.setRunning(isRunning = false)
            clearMocks(player, answers = false)

            // Act
            controller.setRunning(isRunning = true)

            // Assert
            verify(exactly = 0) { player.play() }
        }
    }

    @Nested
    inner class Release {

        @Test
        fun `GIVEN a playing clip WHEN released THEN the player is released and stalls are no longer watched`() =
            runTest {
                // Arrange
                every { player.presentedFrameCount } returns FRAMES
                val controller = createStartedController()

                // Act
                controller.release()
                runCurrent()

                // Assert
                verify(exactly = 1) { player.release() }
                assertThat(isStallWatched()).isFalse()
                assertThat(cues).isEmpty()
            }
    }

    private fun TestScope.createController(isMotionEnabled: Boolean = true) = OneShotVideoController(
        scope = backgroundScope,
        cache = cache,
        diagnosticsSubject = SUBJECT,
        stallTimeoutMs = STALL_TIMEOUT_MS,
        isMotionEnabled = isMotionEnabled,
        onCueReached = { reason -> cues += reason },
        createPlayer = { listener ->
            heardPlayerListener = listener
            player
        },
    )

    private suspend fun TestScope.createReadyController(): OneShotVideoController {
        val controller = createController()
        controller.prepare(url = URL, cueMs = CUE_MS)
        playerListener.onReady()
        return controller
    }

    private suspend fun TestScope.createStartedController(): OneShotVideoController {
        val controller = createReadyController()
        controller.start()
        return controller
    }

    private fun TestScope.isStallWatched(): Boolean = backgroundScope.coroutineContext.job.children.any { it.isActive }

    internal data class StopModel(val name: String, val stop: (OneShotVideoPlayer.Listener) -> Unit) {
        override fun toString(): String = name
    }

    private companion object {
        const val SUBJECT = "Test clip"
        const val URL = "https://cdn.test/reveal.mp4"
        const val OTHER_URL = "https://cdn.test/reveal-dark.mp4"
        const val CUE_MS = 7_000L
        const val SKIP_TO_MS = 6_100L
        const val FRAMES = 42L
        const val STALL_TIMEOUT_MS = 3_000L
        val PLAYBACK_ERROR = IllegalStateException("decoder failed")
    }
}