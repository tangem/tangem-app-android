package com.tangem.core.media

import android.view.SurfaceView
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
internal class LoopingVideoControllerTest {

    private val player: LoopingVideoPlayer = mockk(relaxed = true)
    private val surfaceView: SurfaceView = mockk(relaxed = true)
    private val otherSurfaceView: SurfaceView = mockk(relaxed = true)

    private var heardPlayerListener: LoopingVideoPlayer.Listener? = null
    private val playerListener: LoopingVideoPlayer.Listener
        get() = checkNotNull(heardPlayerListener) { "no controller was created in this test" }

    private val failures = mutableListOf<ReportedFailure>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(player)
        heardPlayerListener = null
        failures.clear()
        every { player.isMotionEnabled } returns true
        surfaceAttached(isAttached = false)
    }

    @Test
    fun `GIVEN nothing decoded yet WHEN created THEN the surface already has the declared size and stays covered`() =
        runTest {
            // Act
            val state = createController().state.value

            // Assert
            assertThat(state).isEqualTo(
                LoopingVideoState(
                    surface = VideoSurfaceState(
                        frameSize = FRAME_SIZE,
                        key = 0,
                        isFirstFrameShown = false,
                        onAvailable = state.surface.onAvailable,
                        onRelease = state.surface.onRelease,
                    ),
                    isRunning = false,
                    isMotionEnabled = true,
                    isFailed = false,
                ),
            )
        }

    @Test
    fun `GIVEN animations off system-wide WHEN created THEN motion is reported disabled`() = runTest {
        // Arrange
        every { player.isMotionEnabled } returns false

        // Act
        val controller = createController()

        // Assert
        assertThat(controller.state.value.isMotionEnabled).isFalse()
    }

    @Test
    fun `GIVEN playback under way WHEN the position is read THEN the player position is returned`() = runTest {
        // Arrange
        every { player.positionMs } returns 4_200L
        val controller = createController()

        // Act
        val positionMs = controller.positionMs

        // Assert
        assertThat(positionMs).isEqualTo(4_200L)
    }

    @Test
    fun `GIVEN a controller WHEN restarted THEN the player rewinds`() = runTest {
        // Arrange
        val controller = createController()

        // Act
        controller.restart()

        // Assert
        verify(exactly = 1) { player.restart() }
    }

    @Nested
    inner class Surface {

        @Test
        fun `GIVEN a new surface WHEN it becomes available THEN the player renders into it`() = runTest {
            // Arrange
            val controller = createController()

            // Act
            controller.attach(surfaceView)

            // Assert
            verify(exactly = 1) { player.attachSurface(surfaceView) }
        }

        @Test
        fun `GIVEN surface returns after leaving the screen WHEN each frame arrives THEN the poster follows it`() =
            runTest {
                // Arrange
                val controller = createControllerWithFrameShown()
                val shownStates = mutableListOf(controller.isFirstFrameShown())

                // Act
                surfaceAttached(isAttached = false)
                controller.detach(surfaceView)
                shownStates += controller.isFirstFrameShown()
                surfaceAttached(isAttached = true)
                controller.attach(otherSurfaceView)
                shownStates += controller.isFirstFrameShown()
                playerListener.onFirstFrameRendered()
                shownStates += controller.isFirstFrameShown()

                // Assert
                assertThat(shownStates).containsExactly(true, false, false, true).inOrder()
            }

        @Test
        fun `GIVEN a replacement surface WHEN the previous one is released THEN the picture stays uncovered`() =
            runTest {
                // Arrange
                val controller = createControllerWithFrameShown()
                controller.attach(otherSurfaceView)

                // Act
                controller.detach(surfaceView)

                // Assert
                assertThat(controller.isFirstFrameShown()).isTrue()
                verify(exactly = 1) { player.detachSurface(surfaceView) }
            }

        @Test
        fun `GIVEN no surface attached WHEN a pass of the loop reports its first frame THEN the poster stays`() =
            runTest {
                // Arrange
                val controller = createController()

                // Act
                playerListener.onFirstFrameRendered()

                // Assert
                assertThat(controller.isFirstFrameShown()).isFalse()
            }

        @Test
        fun `GIVEN a file of another size WHEN the decoder reports it THEN the surface keeps the declared size`() =
            runTest {
                // Arrange
                val controller = createControllerWithFrameShown()

                // Act
                playerListener.onVideoSizeChanged(width = 720, height = 1280)

                // Assert
                val surface = controller.state.value.surface
                assertThat(surface.frameSize).isEqualTo(FRAME_SIZE)
                assertThat(surface.key).isEqualTo(0)
                assertThat(surface.isFirstFrameShown).isTrue()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SetRunning {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a working video WHEN setRunning THEN the player and the state follow`(model: RunningModel) =
            runTest {
                // Arrange
                val controller = createController()

                // Act
                controller.setRunning(isRunning = model.isRunning)

                // Assert
                verify(exactly = 1) { player.setRunning(isRunning = model.isRunning) }
                assertThat(controller.state.value.isRunning).isEqualTo(model.isRunning)
            }

        @Test
        fun `GIVEN the video failed WHEN the screen asks it to run THEN playback stays stopped`() = runTest {
            // Arrange
            val controller = createControllerWithFrameShown()
            playerListener.onError(PLAYBACK_ERROR)

            // Act
            controller.setRunning(isRunning = true)

            // Assert
            verify(exactly = 0) { player.setRunning(isRunning = true) }
            assertThat(controller.state.value.isRunning).isFalse()
        }

        private fun provideTestModels() = listOf(
            RunningModel(isRunning = true),
            RunningModel(isRunning = false),
        )
    }

    @Nested
    inner class Failure {

        @Test
        fun `GIVEN nothing rendered yet WHEN playback fails THEN the poster stays and the failure is a load one`() =
            runTest {
                // Arrange
                val controller = createController()
                surfaceAttached(isAttached = true)
                controller.attach(surfaceView)

                // Act
                playerListener.onError(PLAYBACK_ERROR)

                // Assert
                assertThat(controller.isFirstFrameShown()).isFalse()
                assertThat(controller.state.value.isFailed).isTrue()
                assertThat(failures).containsExactly(ReportedFailure(PLAYBACK_ERROR, hasRenderedFrame = false))
            }

        @Test
        fun `GIVEN a frame already rendered WHEN playback fails THEN the still frame stays uncovered`() = runTest {
            // Arrange
            val controller = createControllerWithFrameShown()

            // Act
            playerListener.onError(PLAYBACK_ERROR)

            // Assert
            assertThat(controller.isFirstFrameShown()).isTrue()
            assertThat(controller.state.value.isFailed).isTrue()
            assertThat(failures).containsExactly(ReportedFailure(PLAYBACK_ERROR, hasRenderedFrame = true))
        }

        @Test
        fun `GIVEN a fresh surface after a rendered frame WHEN playback fails THEN the poster stays`() = runTest {
            // Arrange
            val controller = createControllerWithFrameShown()
            surfaceAttached(isAttached = false)
            controller.detach(surfaceView)
            surfaceAttached(isAttached = true)
            controller.attach(otherSurfaceView)

            // Act
            playerListener.onError(PLAYBACK_ERROR)

            // Assert
            assertThat(controller.isFirstFrameShown()).isFalse()
            assertThat(controller.state.value.isFailed).isTrue()
        }

        @Test
        fun `GIVEN the surface already gone after a frame WHEN playback fails THEN it fails after its first frame`() =
            runTest {
                // Arrange
                val controller = createControllerWithFrameShown()
                surfaceAttached(isAttached = false)
                controller.detach(surfaceView)

                // Act
                playerListener.onError(PLAYBACK_ERROR)

                // Assert
                assertThat(controller.isFirstFrameShown()).isFalse()
                assertThat(failures).containsExactly(ReportedFailure(PLAYBACK_ERROR, hasRenderedFrame = true))
            }
    }

    @Nested
    inner class OutputStall {

        @Test
        fun `GIVEN no frames coming out while playing WHEN the stall is noticed THEN a covered surface replaces it`() =
            runTest {
                // Arrange
                val controller = createControllerWithFrameShown()
                every { player.outputFrameCount } returns 28L

                // Act
                playerListener.onPlayingChanged(isPlaying = true)
                advanceTimeBy(STALL_DETECTED_AFTER_MS)
                runCurrent()

                // Assert
                assertThat(controller.state.value.surface.key).isEqualTo(1)
                assertThat(controller.isFirstFrameShown()).isFalse()
            }

        @Test
        fun `GIVEN a stall new surfaces do not cure WHEN the watchdog gives up THEN the video is treated as failed`() =
            runTest {
                // Arrange
                val controller = createControllerWithFrameShown()
                controller.setRunning(isRunning = true)
                every { player.outputFrameCount } returns 28L

                // Act
                playerListener.onPlayingChanged(isPlaying = true)
                advanceTimeBy(STALL_DETECTED_AFTER_MS * 4)
                runCurrent()

                // Assert
                val state = controller.state.value
                assertThat(state.surface.key).isEqualTo(3)
                assertThat(state.surface.isFirstFrameShown).isFalse()
                assertThat(state.isFailed).isTrue()
                assertThat(state.isRunning).isFalse()
                verify { player.setRunning(isRunning = false) }
                assertThat(failures.map { it.hasRenderedFrame }).containsExactly(true)
            }
    }

    @Nested
    inner class Release {

        @Test
        fun `GIVEN a playing video WHEN released THEN the player is released and its output no longer watched`() =
            runTest {
                // Arrange
                val controller = createControllerWithFrameShown()
                every { player.outputFrameCount } returns 28L
                playerListener.onPlayingChanged(isPlaying = true)

                // Act
                controller.release()
                advanceTimeBy(STALL_DETECTED_AFTER_MS * 4)
                runCurrent()

                // Assert
                verify(exactly = 1) { player.release() }
                assertThat(controller.state.value.surface.key).isEqualTo(0)
                assertThat(failures).isEmpty()
            }
    }

    private fun surfaceAttached(isAttached: Boolean) {
        every { player.isSurfaceAttached } returns isAttached
    }

    private fun TestScope.createController() = LoopingVideoController(
        scope = backgroundScope,
        frameSize = FRAME_SIZE,
        diagnosticsSubject = SUBJECT,
        onFailed = { error, hasRenderedFrame -> failures += ReportedFailure(error, hasRenderedFrame) },
        createPlayer = { listener ->
            heardPlayerListener = listener
            player
        },
    )

    private fun TestScope.createControllerWithFrameShown(): LoopingVideoController {
        val controller = createController()
        surfaceAttached(isAttached = true)
        controller.attach(surfaceView)
        playerListener.onFirstFrameRendered()
        return controller
    }

    private fun LoopingVideoController.attach(surfaceView: SurfaceView) = state.value.surface.onAvailable(surfaceView)

    private fun LoopingVideoController.detach(surfaceView: SurfaceView) = state.value.surface.onRelease(surfaceView)

    private fun LoopingVideoController.isFirstFrameShown() = state.value.surface.isFirstFrameShown

    private data class ReportedFailure(val error: Throwable, val hasRenderedFrame: Boolean)

    internal data class RunningModel(val isRunning: Boolean)

    private companion object {
        const val SUBJECT = "Test video"
        val FRAME_SIZE = VideoFrameSize(width = 1080, height = 1920)
        val PLAYBACK_ERROR = IllegalStateException("decoder failed")

        /** The first check only takes the baseline, then four more checks 250 ms apart see the count stand still. */
        const val STALL_DETECTED_AFTER_MS = 1_251L
    }
}