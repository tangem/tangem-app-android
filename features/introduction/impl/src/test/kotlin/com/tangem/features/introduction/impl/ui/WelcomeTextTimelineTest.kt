package com.tangem.features.introduction.impl.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.features.introduction.impl.ui.WelcomeTextTimeline.Frame
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WelcomeTextTimelineTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a loop position WHEN a line is framed THEN it matches the handoff`(model: FrameModel) {
        // Act
        val frame = WelcomeTextTimeline.frame(positionMs = model.positionMs, scene = model.scene, line = model.line)

        // Assert
        assertThat(frame.alpha).isWithin(TOLERANCE).of(model.expected.alpha)
        assertThat(frame.offsetDp).isWithin(TOLERANCE).of(model.expected.offsetDp)
    }

    @Test
    fun `GIVEN any loop position WHEN the scenes are framed THEN no two scenes are visible at once`() {
        // Act
        val visibleScenesPerFrame = (0L..WelcomeTextTimeline.LOOP_LENGTH_MS step 10L).map { positionMs ->
            (0 until WelcomeTextTimeline.sceneCount).count { scene ->
                (0..1).any { line -> WelcomeTextTimeline.frame(positionMs, scene, line).alpha > 0f }
            }
        }

        // Assert
        assertThat(visibleScenesPerFrame.max()).isAtMost(1)
    }

    @Test
    fun `GIVEN a scene the timeline does not know WHEN framed THEN it stays hidden`() {
        // Act
        val frame = WelcomeTextTimeline.frame(positionMs = 5_000, scene = WelcomeTextTimeline.sceneCount, line = 0)

        // Assert
        assertThat(frame).isEqualTo(WelcomeTextTimeline.hidden)
    }

    @ParameterizedTest
    @MethodSource("provideHoldModels")
    fun `GIVEN a stopped video WHEN the position is held THEN the last visible scene is kept`(model: HoldModel) {
        // Act
        val heldMs = WelcomeTextTimeline.holdPosition(model.positionMs)

        // Assert
        assertThat(heldMs).isEqualTo(model.expectedMs)
    }

    @Test
    fun `GIVEN any stopping position WHEN it is held THEN exactly one scene stays on screen`() {
        // Act
        val visibleScenesPerHold = (0L..WelcomeTextTimeline.LOOP_LENGTH_MS step 10L).map { positionMs ->
            val heldMs = WelcomeTextTimeline.holdPosition(positionMs)
            (0 until WelcomeTextTimeline.sceneCount).count { scene ->
                (0..1).any { line -> WelcomeTextTimeline.frame(heldMs, scene, line).alpha > 0f }
            }
        }

        // Assert
        assertThat(visibleScenesPerHold.toSet()).containsExactly(1)
    }

    @ParameterizedTest
    @MethodSource("provideActiveSceneModels")
    fun `GIVEN a loop position WHEN the active scene is asked THEN it is the last one that started`(
        model: ActiveSceneModel,
    ) {
        // Act
        val scene = WelcomeTextTimeline.activeScene(model.positionMs)

        // Assert
        assertThat(scene).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FrameModel(positionMs = 0, scene = 0, line = 0, expected = WelcomeTextTimeline.hidden),
        FrameModel(positionMs = 1_000, scene = 0, line = 0, expected = WelcomeTextTimeline.steady),
        FrameModel(positionMs = 1_000, scene = 1, line = 0, expected = WelcomeTextTimeline.hidden),
        // Halfway through the exit move, two thirds into the exit fade.
        FrameModel(positionMs = 3_350, scene = 0, line = 0, expected = Frame(alpha = MID_FADE, offsetDp = 4f)),
        // The second line runs 50 ms behind the first.
        FrameModel(positionMs = 3_400, scene = 0, line = 1, expected = Frame(alpha = MID_FADE, offsetDp = 4f)),
        // Halfway through the enter move, a third into the enter fade.
        FrameModel(positionMs = 3_650, scene = 1, line = 0, expected = Frame(alpha = MID_FADE, offsetDp = -5f)),
        FrameModel(positionMs = 4_000, scene = 0, line = 0, expected = Frame(alpha = 0f, offsetDp = 8f)),
        FrameModel(positionMs = 5_000, scene = 1, line = 1, expected = WelcomeTextTimeline.steady),
        FrameModel(positionMs = 10_000, scene = 2, line = 0, expected = WelcomeTextTimeline.steady),
        FrameModel(positionMs = 12_900, scene = 2, line = 1, expected = Frame(alpha = 0f, offsetDp = 7.776f)),
    )

    private fun provideHoldModels() = listOf(
        // Text on screen, even barely: the frame is kept exactly as it looked.
        HoldModel(positionMs = 1_000, expectedMs = 1_000),
        HoldModel(positionMs = 3_450, expectedMs = 3_450),
        // The gaps between the scenes, where every line is transparent and the timeline has to rewind.
        HoldModel(positionMs = 0, expectedMs = 500),
        HoldModel(positionMs = 149, expectedMs = 500),
        HoldModel(positionMs = 3_520, expectedMs = 3_100),
        HoldModel(positionMs = 7_240, expectedMs = 6_800),
        HoldModel(positionMs = WelcomeTextTimeline.LOOP_LENGTH_MS, expectedMs = 12_400),
    )

    private fun provideActiveSceneModels() = listOf(
        ActiveSceneModel(positionMs = 0, expected = 0),
        ActiveSceneModel(positionMs = 3_399, expected = 0),
        ActiveSceneModel(positionMs = 3_400, expected = 1),
        ActiveSceneModel(positionMs = 7_100, expected = 2),
        ActiveSceneModel(positionMs = WelcomeTextTimeline.LOOP_LENGTH_MS, expected = 2),
    )

    internal data class FrameModel(val positionMs: Long, val scene: Int, val line: Int, val expected: Frame)

    internal data class ActiveSceneModel(val positionMs: Long, val expected: Int)

    internal data class HoldModel(val positionMs: Long, val expectedMs: Long)

    private companion object {
        const val TOLERANCE = 0.001f

        /** Smoothstep of one third, which is also one minus smoothstep of two thirds. */
        const val MID_FADE = 0.2593f
    }
}