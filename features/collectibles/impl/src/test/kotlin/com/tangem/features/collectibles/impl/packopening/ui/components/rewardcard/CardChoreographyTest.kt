package com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

private const val TOLERANCE = 0.001f

private val TIMING = RewardCardTiming.Default

internal class CardChoreographyTest {

    @Test
    fun `GIVEN no progress WHEN growing THEN the card is at its start scale, below its place and unturned`() {
        // Act
        val scale = CardChoreography.growScale(grow = 0f, timing = TIMING)
        val shift = CardChoreography.growShift(grow = 0f, timing = TIMING)
        val degrees = CardChoreography.spinDegrees(spin = 0f)

        // Assert
        assertThat(scale).isWithin(TOLERANCE).of(TIMING.startScale)
        assertThat(shift).isWithin(TOLERANCE).of(TIMING.startShift)
        assertThat(degrees).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `GIVEN full growth WHEN the settle starts THEN scale and shift continue from the peak`() {
        // Act
        val grownScale = CardChoreography.growScale(grow = 1f, timing = TIMING)
        val settlingScale = CardChoreography.settleScale(settle = 0f, timing = TIMING)
        val grownShift = CardChoreography.growShift(grow = 1f, timing = TIMING)
        val settlingShift = CardChoreography.settleShift(settle = 0f, timing = TIMING)

        // Assert — the hand-off between the two beats must not jump.
        assertThat(settlingScale).isWithin(TOLERANCE).of(grownScale)
        assertThat(settlingShift).isWithin(TOLERANCE).of(grownShift)
    }

    @Test
    fun `GIVEN the settle WHEN it ends THEN the card is at its own size and place`() {
        // Act
        val scale = CardChoreography.settleScale(settle = 1f, timing = TIMING)
        val shift = CardChoreography.settleShift(settle = 1f, timing = TIMING)

        // Assert
        assertThat(scale).isWithin(TOLERANCE).of(CardChoreography.SETTLED_SCALE)
        assertThat(shift).isWithin(TOLERANCE).of(0f)
    }

    /** A turn that ends at 350 degrees looks like a card held at an angle, and nothing else would catch it. */
    @Test
    fun `GIVEN the turn completes WHEN spinning THEN the card lands face-on`() {
        // Act
        val degrees = CardChoreography.spinDegrees(spin = 1f)

        // Assert
        assertThat(degrees).isWithin(TOLERANCE).of(CardChoreography.SPIN_DEGREES)
    }

    @Test
    fun `GIVEN progress outside its range WHEN spinning THEN the turn is clamped to one whole turn`() {
        // Act
        val before = CardChoreography.spinDegrees(spin = -1f)
        val after = CardChoreography.spinDegrees(spin = 2f)

        // Assert
        assertThat(before).isWithin(TOLERANCE).of(0f)
        assertThat(after).isWithin(TOLERANCE).of(CardChoreography.SPIN_DEGREES)
    }

    @Test
    fun `GIVEN the settle WHEN it runs THEN the case grows in behind the card over the same window`() {
        // Act
        val start = CardChoreography.frameScale(settle = 0f)
        val end = CardChoreography.frameScale(settle = 1f)

        // Assert
        assertThat(start).isWithin(TOLERANCE).of(CardChoreography.FRAME_START_SCALE)
        assertThat(end).isWithin(TOLERANCE).of(CardChoreography.SETTLED_SCALE)
    }
}