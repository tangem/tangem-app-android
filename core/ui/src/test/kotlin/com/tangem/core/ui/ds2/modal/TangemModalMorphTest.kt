package com.tangem.core.ui.ds2.modal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class TangemModalMorphTest {

    @Nested
    @OptIn(ExperimentalFoundationApi::class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OpenProgress {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun `GIVEN no anchors WHEN openProgress THEN reports the settled value`(model: OpenProgressModel) {
            // Arrange
            val state = TangemSheetState(
                skipPartiallyExpanded = model.skipPartiallyExpanded,
                density = Density(density = 1f),
                initialValue = model.initialValue,
            )

            // Act
            val actual = state.openProgress()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN anchors and a dragged offset WHEN openProgress THEN interpolates between them`() {
            // Arrange
            val state = TangemSheetState(skipPartiallyExpanded = true, density = Density(density = 1f))
            state.anchoredDraggableState.updateAnchors(
                newAnchors = DraggableAnchors {
                    TangemSheetValue.Hidden at HIDDEN_POSITION
                    TangemSheetValue.Expanded at EXPANDED_POSITION
                },
                newTarget = TangemSheetValue.Hidden,
            )

            // Act
            val hiddenProgress = state.openProgress()
            state.anchoredDraggableState.dispatchRawDelta(delta = -HALFWAY_DELTA)
            val halfwayProgress = state.openProgress()

            // Assert
            assertThat(hiddenProgress).isEqualTo(0f)
            assertThat(halfwayProgress).isEqualTo(0.5f)
        }

        private fun provideTestModels() = listOf(
            OpenProgressModel(
                initialValue = TangemSheetValue.Hidden,
                skipPartiallyExpanded = true,
                expected = 0f,
            ),
            OpenProgressModel(
                initialValue = TangemSheetValue.Expanded,
                skipPartiallyExpanded = true,
                expected = OPEN_PROGRESS_SETTLED,
            ),
            OpenProgressModel(
                initialValue = TangemSheetValue.PartiallyExpanded,
                skipPartiallyExpanded = false,
                expected = OPEN_PROGRESS_SETTLED,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ModalMorphTarget {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun `GIVEN a content height WHEN modalMorphTarget THEN blends only near the full height`(
            model: MorphTargetModel,
        ) {
            // Act
            val actual = modalMorphTarget(intrinsicHeight = model.intrinsicHeight, maxHeight = model.maxHeight)

            // Assert
            assertThat(actual).isWithin(TOLERANCE).of(model.expected)
        }

        private fun provideTestModels() = listOf(
            MorphTargetModel(intrinsicHeight = 0, maxHeight = 1000, expected = 0f),
            MorphTargetModel(intrinsicHeight = 900, maxHeight = 1000, expected = 0f),
            MorphTargetModel(intrinsicHeight = 950, maxHeight = 1000, expected = 0.5f),
            MorphTargetModel(intrinsicHeight = 1000, maxHeight = 1000, expected = 1f),
            MorphTargetModel(intrinsicHeight = 500, maxHeight = 0, expected = 0f),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CombinedOpenProgress {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun `GIVEN a back gesture WHEN combinedOpenProgress THEN dims by the previewed share`(
            model: CombinedProgressModel,
        ) {
            // Act
            val actual = combinedOpenProgress(openProgress = model.openProgress, backProgress = model.backProgress)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CombinedProgressModel(
                openProgress = OPEN_PROGRESS_SETTLED,
                backProgress = 0f,
                expected = OPEN_PROGRESS_SETTLED,
            ),
            CombinedProgressModel(
                openProgress = OPEN_PROGRESS_SETTLED,
                backProgress = 1f,
                expected = OPEN_PROGRESS_SETTLED - MODAL_BACK_PREVIEW_FRACTION,
            ),
            CombinedProgressModel(
                openProgress = 0f,
                backProgress = 1f,
                expected = 0f,
            ),
        )
    }

    internal data class OpenProgressModel(
        val initialValue: TangemSheetValue,
        val skipPartiallyExpanded: Boolean,
        val expected: Float,
    )

    internal data class CombinedProgressModel(
        val openProgress: Float,
        val backProgress: Float,
        val expected: Float,
    )

    internal data class MorphTargetModel(
        val intrinsicHeight: Int,
        val maxHeight: Int,
        val expected: Float,
    )

    private companion object {
        const val HIDDEN_POSITION = 1000f
        const val EXPANDED_POSITION = 200f
        const val HALFWAY_DELTA = 400f
        const val TOLERANCE = 1e-4f
    }
}