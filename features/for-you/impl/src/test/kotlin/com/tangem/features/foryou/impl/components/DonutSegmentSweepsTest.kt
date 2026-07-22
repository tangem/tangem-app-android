package com.tangem.features.foryou.impl.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class DonutSegmentSweepsTest {

    @Test
    fun `GIVEN empty weights WHEN visualSweepAngles THEN returns empty`() {
        // Act
        val actual = visualSweepAngles(emptyList())

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN all zero weights WHEN visualSweepAngles THEN all zero and size preserved`() {
        // Act
        val actual = visualSweepAngles(listOf(0f, 0f, 0f))

        // Assert
        assertThat(actual).containsExactly(0f, 0f, 0f).inOrder()
    }

    @Test
    fun `GIVEN all segments above floor WHEN visualSweepAngles THEN sweeps stay proportional to weight`() {
        // Arrange — 0.5 / 0.3 / 0.2, none below 5%.
        val weights = listOf(0.5f, 0.3f, 0.2f)

        // Act
        val actual = visualSweepAngles(weights)

        // Assert — untouched: weight * 360.
        assertThat(actual[0]).isWithin(TOLERANCE).of(180f)
        assertThat(actual[1]).isWithin(TOLERANCE).of(108f)
        assertThat(actual[2]).isWithin(TOLERANCE).of(72f)
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN a segment below floor WHEN visualSweepAngles THEN it is raised to the floor and larger ones shrink`() {
        // Arrange — only 0.05 is below the floor; filled sum is the whole circle.
        val weights = listOf(0.8f, 0.15f, 0.05f)

        // Act
        val actual = visualSweepAngles(weights)

        // Assert — the tiny slice is floored, the rest shrink to keep the sum at 360°.
        assertThat(actual[2]).isWithin(TOLERANCE).of(FLOOR_DEG)
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
        // Proportion between the two large slices is preserved (288 / 54 == actual[0] / actual[1]).
        assertThat(actual[0] / actual[1]).isWithin(TOLERANCE).of(288f / 54f)
    }

    @Test
    fun `GIVEN zero-weight slices among real ones WHEN visualSweepAngles THEN zeros stay zero`() {
        // Arrange — a 0f slice sits between real ones.
        val weights = listOf(0.9f, 0f, 0.08f, 0.02f)

        // Act
        val actual = visualSweepAngles(weights)

        // Assert
        assertThat(actual[1]).isEqualTo(0f)
        assertThat(actual[3]).isWithin(TOLERANCE).of(FLOOR_DEG)
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN a single tiny segment WHEN visualSweepAngles THEN it grows into the track up to the floor`() {
        // Arrange — 2% with no larger slice to borrow from; it must grow into the unfilled track.
        val weights = listOf(0.02f)

        // Act
        val actual = visualSweepAngles(weights)

        // Assert
        assertThat(actual[0]).isWithin(TOLERANCE).of(FLOOR_DEG)
    }

    @Test
    fun `GIVEN filled sum below the full circle and floors fit WHEN visualSweepAngles THEN filled sum preserved`() {
        // Arrange — segments sum to 0.5 of the circle; the 0.03 slice is below the floor.
        val weights = listOf(0.4f, 0.07f, 0.03f)
        val filledSum = (0.4f + 0.07f + 0.03f) * 360f

        // Act
        val actual = visualSweepAngles(weights)

        // Assert — small one floored, total filled sweep (track remainder) unchanged.
        assertThat(actual[2]).isWithin(TOLERANCE).of(FLOOR_DEG)
        assertThat(actual.sum()).isWithin(TOLERANCE).of(filledSum)
    }

    @Test
    fun `GIVEN more segments than the floor allows WHEN visualSweepAngles THEN falls back to an equal split`() {
        // Arrange — 25 equal slices; 25 floors would overflow 360°, so the floor drops to 360/25.
        val weights = List(25) { 0.04f }

        // Act
        val actual = visualSweepAngles(weights)

        // Assert
        actual.forEach { assertThat(it).isWithin(TOLERANCE).of(360f / 25f) }
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN a full ring and capDeg WHEN visualSweepAngles THEN only the last floored slice is bumped`() {
        // Arrange — two tiny slices below the floor on a full ring; index 2 is the last active.
        val weights = listOf(0.9f, 0.05f, 0.05f)

        // Act
        val actual = visualSweepAngles(weights, capDeg = CAP_DEG)

        // Assert — the non-last floored slice sits at the plain floor, the last one is bumped above it.
        assertThat(actual[1]).isWithin(TOLERANCE).of(FLOOR_DEG)
        assertThat(actual[2]).isGreaterThan(actual[1])
        assertThat(actual.sum()).isWithin(TOLERANCE).of(360f)
    }

    @Test
    fun `GIVEN a gap wider than capDeg WHEN visualSweepAngles THEN the last slice is not bumped`() {
        // Arrange — filled sum well below the circle, so the gap far exceeds capDeg.
        val weights = listOf(0.4f, 0.05f)

        // Act
        val actual = visualSweepAngles(weights, capDeg = CAP_DEG)

        // Assert — no compensation: the last floored slice stays at the plain floor.
        assertThat(actual[1]).isWithin(TOLERANCE).of(FLOOR_DEG)
    }

    private companion object {
        const val TOLERANCE = 0.01f
        const val CAP_DEG = 12f

        // Derived from the production constant so these tests track it instead of hardcoding the angle.
        const val FLOOR_DEG = MIN_VISUAL_SWEEP_FRACTION * 360f
    }
}