package com.tangem.features.foryou.impl.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class DonutSegmentSweepsTest {

    @Test
    fun `GIVEN empty weights WHEN visualSweepAngles THEN returns empty`() {
        // Act
        val actual = visualSweepAngles(emptyList(), capDeg = 0f)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN all zero weights WHEN visualSweepAngles THEN all zero and size preserved`() {
        // Act
        val actual = visualSweepAngles(listOf(0f, 0f, 0f), capDeg = 0f)

        // Assert
        assertThat(actual).containsExactly(0f, 0f, 0f).inOrder()
    }

    @Test
    fun `GIVEN all segments above floor WHEN visualSweepAngles THEN sweeps stay proportional to weight`() {
        // Arrange — 0.5 / 0.3 / 0.2, none below the floor.
        val weights = listOf(0.5f, 0.3f, 0.2f)

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert — untouched: weight * 360.
        assertThat(actual[0]).isWithin(TOLERANCE).of(180f)
        assertThat(actual[1]).isWithin(TOLERANCE).of(108f)
        assertThat(actual[2]).isWithin(TOLERANCE).of(72f)
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN a segment below floor WHEN visualSweepAngles THEN it is raised to the floor and larger ones shrink`() {
        // Arrange — one below-floor slice; the other two split the rest 4:1 and stay above the floor.
        val rest = 1f - BELOW_FLOOR
        val weights = listOf(rest * 0.8f, rest * 0.2f, BELOW_FLOOR)

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert — the tiny slice is floored, the rest shrink to keep the sum at 360°.
        assertThat(actual[2]).isWithin(TOLERANCE).of(FLOOR_DEG)
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
        // Proportion between the two large slices is preserved.
        assertThat(actual[0] / actual[1]).isWithin(TOLERANCE).of(0.8f / 0.2f)
    }

    @Test
    fun `GIVEN zero-weight slices among real ones WHEN visualSweepAngles THEN zeros stay zero`() {
        // Arrange — a 0f slice sits between real ones; the last is below the floor.
        val weights = listOf(1f - ABOVE_FLOOR - BELOW_FLOOR, 0f, ABOVE_FLOOR, BELOW_FLOOR)

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert
        assertThat(actual[1]).isEqualTo(0f)
        assertThat(actual[3]).isWithin(TOLERANCE).of(FLOOR_DEG)
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN a single tiny segment WHEN visualSweepAngles THEN it grows into the track up to the floor`() {
        // Arrange — a below-floor slice with none larger to borrow from: it grows into the unfilled track.
        val weights = listOf(BELOW_FLOOR)

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert
        assertThat(actual[0]).isWithin(TOLERANCE).of(FLOOR_DEG)
    }

    @Test
    fun `GIVEN filled sum below the full circle and floors fit WHEN visualSweepAngles THEN filled sum preserved`() {
        // Arrange — segments leave a wide grey gap; only the last is below the floor.
        val weights = listOf(ABOVE_FLOOR * 2f, ABOVE_FLOOR, BELOW_FLOOR)
        val filledSum = weights.sum() * 360f

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert — small one floored, total filled sweep (track remainder) unchanged.
        assertThat(actual[2]).isWithin(TOLERANCE).of(FLOOR_DEG)
        assertThat(actual.sum()).isWithin(TOLERANCE).of(filledSum)
    }

    @Test
    fun `GIVEN more segments than the floor allows WHEN visualSweepAngles THEN falls back to an equal split`() {
        // Arrange — 25 equal slices; 25 floors would overflow 360°, so the floor drops to 360/25.
        val weights = List(25) { 0.04f }

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert
        actual.forEach { assertThat(it).isWithin(TOLERANCE).of(360f / 25f) }
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN a grey gap thinner than the floor WHEN visualSweepAngles THEN it is grown to the floor`() {
        // Arrange — segments leave a grey sliver thinner than the floor.
        val weights = listOf(0.6f, 1f - 0.6f - BELOW_FLOOR)

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert — segments shrink to 360° − floor so the grey gap reads at exactly the floor,
        // and the two segments keep their 0.6 : 0.39 proportion.
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f - FLOOR_DEG)
        assertThat(actual[0] / actual[1]).isWithin(TOLERANCE).of(0.6f / (1f - 0.6f - BELOW_FLOOR))
    }

    @Test
    fun `GIVEN segments filling the whole ring WHEN visualSweepAngles THEN no grey gap is reserved`() {
        // Arrange — segments sum to exactly 1.0.
        val weights = listOf(0.6f, 0.4f)

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert — full ring: segments still occupy the whole circle, no grey gap carved out.
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN a grey gap already wider than the floor WHEN visualSweepAngles THEN segments are untouched`() {
        // Arrange — segments sum to 0.6, leaving a 144° grey gap well above the floor.
        val weights = listOf(0.3f, 0.3f)
        val filledSum = 0.6f * 360f

        // Act
        val actual = visualSweepAngles(weights, capDeg = 0f)

        // Assert — filled sweep (and therefore the grey gap) is left exactly as-is.
        assertThat(actual.sum()).isWithin(TOLERANCE).of(filledSum)
        assertThat(actual[0]).isWithin(TOLERANCE).of(108f)
        assertThat(actual[1]).isWithin(TOLERANCE).of(108f)
    }

    @Test
    fun `GIVEN a grey gap and capDeg WHEN visualSweepAngles THEN grey floor is padded by the cap overlap`() {
        // Arrange — a thin grey gap grown to the floor. With a cap width, both neighbouring caps eat into
        // the gap, so the reserved grey sweep is padded by capDeg to keep the *visible* grey at the floor.
        val weights = listOf(0.6f, 1f - 0.6f - BELOW_FLOOR)

        // Act
        val actual = visualSweepAngles(weights, capDeg = CAP_DEG)

        // Assert — segments give up the grey's floor plus CAP_DEG so the visible grey reads as the floor;
        // the last slice itself is not bumped (free end), so the two keep their 0.6 : 0.39 proportion.
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f - (FLOOR_DEG + CAP_DEG))
        assertThat(actual[0] / actual[1]).isWithin(TOLERANCE).of(0.6f / (1f - 0.6f - BELOW_FLOOR))
    }

    @Test
    fun `GIVEN capDeg WHEN visualSweepAngles THEN the last slice sits at the plain floor like any other`() {
        // Arrange — filled sum well below the circle, so the gap far exceeds capDeg.
        val weights = listOf(ABOVE_FLOOR * 2f, BELOW_FLOOR)

        // Act
        val actual = visualSweepAngles(weights, capDeg = CAP_DEG)

        // Assert — no slice is ever widened to pay for a cap; a cap only ever extends forward.
        assertThat(actual[1]).isWithin(TOLERANCE).of(FLOOR_DEG)
    }

    @Test
    fun `GIVEN more slices than the floor allows WHEN visualSweepAngles THEN falls back to an equal split`() {
        // Arrange — 72 equal slices, so an equal share is narrower than the minimum share: the floor has
        // to be clamped back to 360 / 72 and the split must still come out even.
        val count = 72
        val weights = List(count) { 1f / count }

        // Act
        val actual = visualSweepAngles(weights, capDeg = CAP_DEG)

        // Assert
        actual.forEach { assertThat(it).isWithin(TOLERANCE).of(360f / count) }
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN an unfilled remainder WHEN slicePaintOrder THEN slices are painted back to front`() {
        // Act
        val actual = slicePaintOrder(listOf(180f, 108f))

        // Assert
        assertThat(actual).containsExactly(1, 0).inOrder()
    }

    @Test
    fun `GIVEN a closed ring WHEN slicePaintOrder THEN the order is unchanged`() {
        // Arrange — closing the ring must not reorder anything; the wrap is closed by a cap instead.
        val sweeps = listOf(180f, 108f, 72f)

        // Act
        val actual = slicePaintOrder(sweeps)

        // Assert
        assertThat(actual).containsExactly(2, 1, 0).inOrder()
    }

    @Test
    fun `GIVEN zero-weight slices WHEN slicePaintOrder THEN they are not painted`() {
        // Act
        val actual = slicePaintOrder(listOf(200f, 0f, 160f, 0f))

        // Assert
        assertThat(actual).containsExactly(2, 0).inOrder()
    }

    @Test
    fun `GIVEN nothing drawn WHEN slicePaintOrder THEN nothing is painted`() {
        // Act
        val actual = slicePaintOrder(listOf(0f, 0f))

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN an unfilled remainder WHEN freeStartSliceIndex THEN the first drawn slice rounds the tail`() {
        // Act
        val actual = freeStartSliceIndex(listOf(180f, 108f))

        // Assert
        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun `GIVEN leading zero-weight slices WHEN freeStartSliceIndex THEN the first drawn slice is returned`() {
        // Act
        val actual = freeStartSliceIndex(listOf(0f, 0f, 180f, 108f))

        // Assert
        assertThat(actual).isEqualTo(2)
    }

    @Test
    fun `GIVEN a closed ring WHEN freeStartSliceIndex THEN there is no free end to round`() {
        // Arrange — the last slice's forward cap covers that point instead.
        val sweeps = listOf(180f, 108f, 72f)

        // Act & Assert
        assertThat(freeStartSliceIndex(sweeps)).isNull()
    }

    @Test
    fun `GIVEN nothing drawn WHEN freeStartSliceIndex THEN there is no free end to round`() {
        // Act & Assert
        assertThat(freeStartSliceIndex(listOf(0f, 0f))).isNull()
    }

    @Test
    fun `GIVEN sweeps a hair short of the circle WHEN isRingClosed THEN the ring still counts as closed`() {
        // Arrange — the running fold of start angles accumulates float error; a shortfall this small is
        // noise, not a remainder.
        val sweeps = listOf(180f, 179.999f)

        // Act & Assert
        assertThat(isRingClosed(sweeps)).isTrue()
    }

    @Test
    fun `GIVEN a remainder wider than float noise WHEN isRingClosed THEN the ring counts as open`() {
        // Arrange
        val sweeps = listOf(180f, 179f)

        // Act & Assert
        assertThat(isRingClosed(sweeps)).isFalse()
    }

    private companion object {
        const val TOLERANCE = 0.01f
        const val CAP_DEG = 12f

        // Derived from the production constant so these tests track it instead of hardcoding the angle.
        const val FLOOR_DEG = MIN_VISUAL_SWEEP_FRACTION * 360f

        /** A share safely below the floor, so the floor branch is exercised whatever the constant is. */
        const val BELOW_FLOOR = MIN_VISUAL_SWEEP_FRACTION / 2f

        /** A share safely above the floor, so it is never floored — nor squeezed below it by a neighbour. */
        const val ABOVE_FLOOR = MIN_VISUAL_SWEEP_FRACTION * 4f

    }
}