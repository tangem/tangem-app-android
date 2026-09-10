package com.tangem.core.ui.ds2.modal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalFoundationApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemModalMorphTest {

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

    internal data class OpenProgressModel(
        val initialValue: TangemSheetValue,
        val skipPartiallyExpanded: Boolean,
        val expected: Float,
    )

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

    private companion object {
        const val HIDDEN_POSITION = 1000f
        const val EXPANDED_POSITION = 200f
        const val HALFWAY_DELTA = 400f
    }
}