package com.tangem.core.ui.ds.topbar.collapsing.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class TangemCollapsingAppBarStateTest {

    @Test
    fun `GIVEN an unmeasured header WHEN height arrives THEN it remains expanded`() {
        // Arrange
        val state = TangemCollapsingAppBarState()

        // Act
        state.updateHeightOffsetLimit(-200f)

        // Assert
        assertThat(state.heightOffset).isEqualTo(0f)
        assertThat(state.heightOffsetLimit).isEqualTo(-200f)
    }

    @Test
    fun `GIVEN a collapsed header WHEN content grows THEN no header gap appears`() {
        // Arrange
        val state = TangemCollapsingAppBarState(initialHeightOffset = -200f, heightOffsetLimit = -200f)

        // Act
        state.updateHeightOffsetLimit(-300f)

        // Assert
        assertThat(state.heightOffset).isEqualTo(-300f)
        assertThat(state.collapsedFraction).isEqualTo(1f)
    }

    @Test
    fun `GIVEN a partial collapse WHEN content grows THEN the scroll offset is preserved`() {
        // Arrange
        val state = TangemCollapsingAppBarState(initialHeightOffset = -80f, heightOffsetLimit = -200f)

        // Act
        state.updateHeightOffsetLimit(-300f)

        // Assert
        assertThat(state.heightOffset).isEqualTo(-80f)
    }

    @Test
    fun `GIVEN a partial collapse WHEN content shrinks past the offset THEN collapse is clamped`() {
        // Arrange
        val state = TangemCollapsingAppBarState(initialHeightOffset = -150f, heightOffsetLimit = -200f)

        // Act
        state.updateHeightOffsetLimit(-100f)

        // Assert
        assertThat(state.heightOffset).isEqualTo(-100f)
        assertThat(state.collapsedFraction).isEqualTo(1f)
    }

    @Test
    fun `GIVEN a measured header WHEN dragging after resize THEN the new collapse limit is used`() {
        // Arrange
        val state = TangemCollapsingAppBarState(isTopOverscrollEnabled = false)
        state.updateHeightOffsetLimit(-200f)

        // Act
        val consumed = state.dispatchRawDelta(-300f)

        // Assert
        assertThat(consumed).isEqualTo(-200f)
        assertThat(state.heightOffset).isEqualTo(-200f)
    }
}