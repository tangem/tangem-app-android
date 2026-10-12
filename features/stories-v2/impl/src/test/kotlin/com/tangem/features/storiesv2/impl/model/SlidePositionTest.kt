package com.tangem.features.storiesv2.impl.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SlidePositionTest {

    private val position = SlidePosition()

    @Test
    fun `GIVEN an entered slide WHEN its position moves THEN it is read by its play token`() {
        // Arrange
        position.enter(playToken = 1)

        // Act
        position.update(positionMs = 1_500L)

        // Assert
        assertThat(position.of(playToken = 1)).isEqualTo(1_500L)
    }

    @Test
    fun `GIVEN a slide the player left WHEN read by its play token THEN it keeps the position it ended at`() {
        // Arrange
        position.enter(playToken = 1)
        position.update(positionMs = 4_000L)

        // Act — the screen still draws the previous slide for the frame the UI model takes to catch up.
        position.enter(playToken = 2)

        // Assert
        assertThat(position.of(playToken = 1)).isEqualTo(4_000L)
    }

    @Test
    fun `GIVEN a slide the player left WHEN the next one is read by its play token THEN it starts from zero`() {
        // Arrange
        position.enter(playToken = 1)
        position.update(positionMs = 4_000L)

        // Act
        position.enter(playToken = 2)

        // Assert
        assertThat(position.of(playToken = 2)).isEqualTo(0L)
        assertThat(position.currentMs).isEqualTo(0L)
    }
}