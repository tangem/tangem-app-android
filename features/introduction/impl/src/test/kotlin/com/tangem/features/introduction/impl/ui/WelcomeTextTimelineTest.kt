package com.tangem.features.introduction.impl.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.features.introduction.impl.ui.WelcomeTextTimeline.Frame
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

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
    fun `GIVEN any loop position WHEN the scenes are framed THEN at most one scene is fully visible`() {
        // Act
        val visibleScenesPerFrame = (0L..12_900L step 10L).map { positionMs ->
            (0 until WelcomeTextTimeline.sceneCount).count { scene ->
                WelcomeTextTimeline.frame(positionMs, scene, line = 0).alpha == 1f
            }
        }

        // Assert
        assertThat(visibleScenesPerFrame.max()).isEqualTo(1)
    }

    private fun provideTestModels() = listOf(
        FrameModel(positionMs = 0, scene = 0, line = 0, expected = HIDDEN_ABOVE),
        FrameModel(positionMs = 1_000, scene = 0, line = 0, expected = WelcomeTextTimeline.steady),
        FrameModel(positionMs = 1_000, scene = 1, line = 0, expected = HIDDEN_ABOVE),
        // Halfway through the exit move, two thirds into the exit fade.
        FrameModel(positionMs = 3_350, scene = 0, line = 0, expected = Frame(alpha = MID_FADE_LATE, offsetDp = 4f)),
        // The second line runs 50 ms behind the first.
        FrameModel(positionMs = 3_400, scene = 0, line = 1, expected = Frame(alpha = MID_FADE_LATE, offsetDp = 4f)),
        // Halfway through the enter move, a third into the enter fade.
        FrameModel(positionMs = 3_650, scene = 1, line = 0, expected = Frame(alpha = MID_FADE_EARLY, offsetDp = -5f)),
        FrameModel(positionMs = 4_000, scene = 0, line = 0, expected = HIDDEN_BELOW),
        FrameModel(positionMs = 5_000, scene = 1, line = 1, expected = WelcomeTextTimeline.steady),
        FrameModel(positionMs = 10_000, scene = 2, line = 0, expected = WelcomeTextTimeline.steady),
        FrameModel(positionMs = 12_900, scene = 2, line = 1, expected = Frame(alpha = 0f, offsetDp = 7.776f)),
    )

    internal data class FrameModel(val positionMs: Long, val scene: Int, val line: Int, val expected: Frame)

    private companion object {
        const val TOLERANCE = 0.001f
        val HIDDEN_ABOVE = Frame(alpha = 0f, offsetDp = -10f)
        val HIDDEN_BELOW = Frame(alpha = 0f, offsetDp = 8f)

        /** Smoothstep of one third. */
        const val MID_FADE_EARLY = 0.2593f

        /** One minus smoothstep of two thirds. */
        const val MID_FADE_LATE = 0.2593f
    }
}