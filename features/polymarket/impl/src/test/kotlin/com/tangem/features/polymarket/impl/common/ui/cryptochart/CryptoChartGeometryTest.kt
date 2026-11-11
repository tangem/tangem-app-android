package com.tangem.features.polymarket.impl.common.ui.cryptochart

import com.google.common.truth.Truth.assertThat
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.Point
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.TimeRange
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.math.RoundingMode

internal class CryptoChartGeometryTest {
    @Test
    fun `GIVEN irregular observations WHEN clipping THEN boundaries interpolate by time`() {
        // Arrange
        val points = listOf(Point(0, 100.0), Point(10, 200.0), Point(100, 110.0))

        // Act
        val visible = points.visiblePoints(TimeRange(5, 55))

        // Assert
        assertThat(visible).containsExactly(Point(5, 150.0), Point(10, 200.0), Point(55, 155.0)).inOrder()
    }

    @Test
    fun `GIVEN a known gap WHEN clipping across it THEN neither boundary is fabricated`() {
        // Arrange
        val points = listOf(Point(0, 100.0), Point(20, 200.0, isSegmentStart = true), Point(30, 300.0))

        // Act
        val visible = points.visiblePoints(TimeRange(10, 25))

        // Assert
        assertThat(visible).containsExactly(Point(20, 200.0, isSegmentStart = true), Point(25, 250.0)).inOrder()
        assertThat(points.priceAt(10)).isNull()
    }

    @Test
    fun `GIVEN only old observations WHEN displaying new window THEN no price is extrapolated`() {
        // Arrange
        val points = listOf(Point(0, 100.0), Point(10, 200.0))

        // Act
        val visible = points.visiblePoints(TimeRange(20, 30))

        // Assert
        assertThat(visible).isEmpty()
        assertThat(points.priceAt(20)).isNull()
    }

    @Test
    fun `GIVEN a future observation WHEN displaying current window THEN it does not become the latest price`() {
        // Arrange
        val points = listOf(Point(0, 100.0), Point(100, 200.0))

        // Act
        val visible = points.visiblePoints(TimeRange(0, 50))

        // Assert
        assertThat(visible).containsExactly(Point(0, 100.0), Point(50, 150.0)).inOrder()
    }

    @ParameterizedTest
    @ValueSource(longs = [0, 50, 100])
    fun `GIVEN one observation WHEN displaying its interval THEN exactly one point is shown`(timestamp: Long) {
        // Arrange
        val point = Point(timestamp, 100.0)

        // Act
        val visible = listOf(point).visiblePoints(TimeRange(0, 100))

        // Assert
        assertThat(visible).containsExactly(point)
    }

    @ParameterizedTest
    @ValueSource(doubles = [0.0, 0.00000001, 0.5, 2500.0, 75000.0])
    fun `GIVEN a flat series WHEN scaling THEN its price is visible and all labels are distinct`(price: Double) {
        // Arrange
        val points = listOf(Point(0, price), Point(10, price))

        // Act
        val scale = CryptoChartScale.from(points, null)
        val labels = scale.ticks(11).map {
            BigDecimal.valueOf(it).setScale(scale.fractionDigits(11), RoundingMode.HALF_UP).toPlainString()
        }

        // Assert
        assertThat(scale.y(price)).isAtLeast(0f)
        assertThat(scale.y(price)).isAtMost(1f)
        assertThat(scale.min).isAtLeast(0.0)
        assertThat(scale.max).isGreaterThan(scale.min)
        assertThat(labels.distinct()).hasSize(11)
    }

    @Test
    fun `GIVEN a distant target WHEN scaling actual prices THEN the price movement stays legible`() {
        // Arrange
        val points = listOf(Point(0, 75_000.0), Point(10, 75_010.0))

        // Act
        val scale = CryptoChartScale.from(points, 10_000.0)

        // Assert
        assertThat(scale).isEqualTo(CryptoChartScale.from(points, null))
        assertThat(scale.y(10_000.0)).isGreaterThan(1f)
    }

    @Test
    fun `GIVEN whole day interval WHEN projecting minute offset THEN position uses the whole day`() {
        // Arrange
        val range = TimeRange(0, 86_400_000)

        // Act
        val x = range.x(60_000)

        // Assert
        assertThat(x).isWithin(0.000001f).of(1f / 1440)
    }
}