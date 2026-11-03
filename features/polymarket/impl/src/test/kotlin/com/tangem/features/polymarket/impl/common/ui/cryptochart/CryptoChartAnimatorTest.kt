package com.tangem.features.polymarket.impl.common.ui.cryptochart

import com.google.common.truth.Truth.assertThat
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.Point
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.TimeRange
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class CryptoChartAnimatorTest {
    @Test
    fun `GIVEN a new quote WHEN animating THEN the head travels between old and new times`() {
        // Arrange
        val animator = CryptoChartAnimator()
        val old = listOf(Point(0, 100.0), Point(1000, 200.0))
        val updated = old + Point(2000, 300.0)
        val range = TimeRange(0, 2000)
        animator.frame(old, range, null, 0, 1f)
        animator.frame(updated, range, null, 10, 1f)

        // Act
        val midway = animator.frame(updated, range, null, 135, 1f)
        val finished = animator.frame(updated, range, null, 260, 1f)

        // Assert
        assertThat(midway.points.last()).isEqualTo(Point(1500, 250.0))
        assertThat(finished.points).isEqualTo(updated)
        assertThat(midway.pointAt(0.75f)).isEqualTo(midway.points.last())
        assertThat(midway.scale.y(midway.points.last().price)).isAtLeast(0f)
    }

    @Test
    fun `GIVEN a gap before a new quote WHEN animating THEN the gap is never bridged`() {
        // Arrange
        val animator = CryptoChartAnimator()
        val old = listOf(Point(0, 100.0), Point(1000, 200.0))
        val updated = old + Point(2000, 300.0, isSegmentStart = true)
        val range = TimeRange(0, 2000)
        animator.frame(old, range, null, 0, 1f)

        // Act
        val frame = animator.frame(updated, range, null, 10, 1f)

        // Assert
        assertThat(frame.points).isEqualTo(updated)
        assertThat(frame.pointAt(0.75f)).isNull()
    }

    @Test
    fun `GIVEN interrupted motion WHEN another snapshot arrives THEN it starts at the displayed head`() {
        // Arrange
        val animator = CryptoChartAnimator()
        val old = listOf(Point(0, 100.0), Point(1000, 200.0))
        val updated = old + Point(2000, 300.0)
        val range = TimeRange(0, 3000)
        animator.frame(old, range, null, 0, 1f)
        animator.frame(updated, range, null, 10, 1f)
        val midway = animator.frame(updated, range, null, 135, 1f)

        // Act
        val next = animator.frame(updated + Point(3000, 400.0), range, null, 150, 1f)

        // Assert
        assertThat(next.points.last()).isEqualTo(midway.points.last())
    }

    @Test
    fun `GIVEN animations disabled WHEN quotes change THEN the latest frame is applied immediately`() {
        // Arrange
        val animator = CryptoChartAnimator()
        val old = listOf(Point(0, 100.0), Point(1000, 200.0))
        val updated = old + Point(2000, 300.0)
        val range = TimeRange(0, 2000)
        animator.frame(old, range, null, 0, 1f)

        // Act
        val frame = animator.frame(updated, range, null, 10, 0f)

        // Assert
        assertThat(frame).isEqualTo(CryptoChartFrame.from(updated, range, null))
    }

    @Test
    fun `GIVEN empty initial data WHEN first history arrives THEN it does not animate from zero price`() {
        // Arrange
        val animator = CryptoChartAnimator()
        val range = TimeRange(0, 2000)
        animator.frame(emptyList(), range, null, 0, 1f)
        val points = listOf(Point(0, 75000.0), Point(1000, 75001.0))

        // Act
        val frame = animator.frame(points, range, null, 10, 1f)

        // Assert
        assertThat(frame).isEqualTo(CryptoChartFrame.from(points, range, null))
    }

    @Test
    fun `GIVEN a moving window WHEN last quote becomes stale THEN no future tail is fabricated`() {
        // Arrange
        val points = listOf(Point(0, 100.0), Point(1000, 200.0))
        val animator = CryptoChartAnimator()
        animator.frame(points, TimeRange(0, 2000), null, 0, 1f)

        // Act
        val frame = animator.frame(points, TimeRange(1500, 3500), null, 1500, 1f)

        // Assert
        assertThat(frame.points).isEmpty()
    }

    @Test
    fun `GIVEN first point at window start WHEN next quote arrives THEN animation starts from that point`() {
        // Arrange
        val animator = CryptoChartAnimator()
        val range = TimeRange(0, 2000)
        val old = listOf(Point(0, 100.0))
        animator.frame(old, range, null, 0, 1f)

        // Act
        val frame = animator.frame(old + Point(1000, 200.0), range, null, 10, 1f)

        // Assert
        assertThat(frame.points).isEqualTo(old)
    }

    @Test
    fun `GIVEN replaced history without old head WHEN updating THEN frame uses available history`() {
        // Arrange
        val animator = CryptoChartAnimator()
        val range = TimeRange(0, 3000)
        animator.frame(listOf(Point(0, 100.0), Point(1000, 200.0)), range, null, 0, 1f)
        val replacement = listOf(Point(2000, 300.0), Point(3000, 400.0))

        // Act
        val frame = animator.frame(replacement, range, null, 10, 1f)

        // Assert
        assertThat(frame.points).isEqualTo(replacement)
    }

    @ParameterizedTest
    @ValueSource(longs = [6500, 10000, 1000000])
    fun `GIVEN ended event WHEN clock advances THEN render time remains fixed`(now: Long) {
        assertThat(cryptoRenderTime(now, 1500, 5000)).isEqualTo(5000L)
    }

    @Test
    fun `GIVEN live event WHEN applying render delay THEN time and interpolation share the delayed window`() {
        assertThat(cryptoRenderTime(5000, 1500, null)).isEqualTo(3500L)
    }
}