package com.tangem.features.polymarket.impl.common.ui.chart

import com.google.common.truth.Truth.assertThat
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Point
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Series
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.SeriesColor
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class ProbabilityChartScaleTest {

    @ParameterizedTest
    @ValueSource(doubles = [0.0, 0.0001, 0.5, 0.9999, 1.0])
    fun `GIVEN flat probability WHEN scaling THEN visible range is nonzero and stays within probability bounds`(
        probability: Double,
    ) {
        // Arrange
        val series = createSeries(values = listOf(1000L to probability, 2000L to probability))

        // Act
        val scale = requireNotNull(ProbabilityChartScale.from(listOf(series)))

        // Assert
        assertThat(scale.min).isAtLeast(0.0)
        assertThat(scale.max).isAtMost(1.0)
        assertThat(scale.min).isAtMost(probability)
        assertThat(scale.max).isAtLeast(probability)
        assertThat(scale.max - scale.min).isAtLeast(0.03 - 1e-9)
        assertThat(scale.ticks.size).isAtLeast(2)
        assertThat(scale.ticks.map { it.probabilityText() }.distinct()).hasSize(scale.ticks.size)
    }

    @Test
    fun `GIVEN histories with different extents WHEN scaling THEN all series share the full time and value range`() {
        // Arrange
        val first = createSeries(values = listOf(1000L to 0.2, 2000L to 0.7))
        val second = createSeries(id = "second", values = listOf(500L to 0.1, 3000L to 0.8))

        // Act
        val scale = requireNotNull(ProbabilityChartScale.from(listOf(first, second)))

        // Assert
        assertThat(scale.start).isEqualTo(500L)
        assertThat(scale.end).isEqualTo(3000L)
        assertThat(scale.min).isLessThan(0.1)
        assertThat(scale.max).isGreaterThan(0.8)
        assertThat(scale.x(500L)).isEqualTo(0f)
        assertThat(scale.x(3000L)).isEqualTo(1f)
        assertThat(scale.y(scale.min)).isEqualTo(1f)
        assertThat(scale.y(scale.max)).isEqualTo(0f)
    }

    @Test
    fun `GIVEN no history WHEN scaling THEN no chart range is fabricated`() {
        // Arrange
        val series = createSeries()

        // Act
        val scale = ProbabilityChartScale.from(listOf(series))

        // Assert
        assertThat(scale).isNull()
    }

    @Test
    fun `GIVEN single timestamp across outcomes WHEN scaling THEN the chart remains empty`() {
        // Arrange
        val series = listOf(
            createSeries(values = listOf(1000L to 0.3)),
            createSeries(id = "second", values = listOf(1000L to 0.7)),
        )

        // Act
        val scale = ProbabilityChartScale.from(series)

        // Assert
        assertThat(scale).isNull()
    }

    @Test
    fun `GIVEN complete probability range WHEN scaling THEN the axis contains zero and one`() {
        // Arrange
        val series = createSeries(values = listOf(1000L to 0.0, 2000L to 1.0))

        // Act
        val scale = requireNotNull(ProbabilityChartScale.from(listOf(series)))

        // Assert
        assertThat(scale.min).isEqualTo(0.0)
        assertThat(scale.max).isEqualTo(1.0)
        assertThat(scale.ticks).containsAtLeast(0.0, 1.0)
    }

    @Test
    fun `GIVEN distinct time steps WHEN reading between points THEN probability uses elapsed time`() {
        // Arrange
        val points = listOf(Point(1000L, 0.2), Point(3000L, 0.8))

        // Act
        val probability = points.probabilityAt(1500L)

        // Assert
        assertThat(requireNotNull(probability)).isWithin(1e-9).of(0.35)
    }

    @ParameterizedTest
    @ValueSource(longs = [999L, 3001L])
    fun `GIVEN a partial history WHEN reading beyond its extent THEN probability is unavailable`(time: Long) {
        // Arrange
        val points = listOf(Point(1000L, 0.2), Point(3000L, 0.8))

        // Act
        val probability = points.probabilityAt(time)

        // Assert
        assertThat(probability).isNull()
    }

    @Test
    fun `GIVEN a single point WHEN reading its exact timestamp THEN the original probability is retained`() {
        // Arrange
        val points = listOf(Point(1000L, 0.42))

        // Act
        val probability = points.probabilityAt(1000L)

        // Assert
        assertThat(probability).isEqualTo(0.42)
    }

    private fun createSeries(id: String = "outcome", values: List<Pair<Long, Double>> = emptyList()) = Series(
        id = id,
        title = "Outcome",
        color = SeriesColor.Blue,
        points = values.map { (time, value) -> Point(time, value) }.toImmutableList(),
    )
}