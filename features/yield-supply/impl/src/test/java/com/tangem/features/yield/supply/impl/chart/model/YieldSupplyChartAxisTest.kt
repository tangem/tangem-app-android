package com.tangem.features.yield.supply.impl.chart.model

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartAxisLabel
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartScale
import com.tangem.test.core.ProvideTestModels
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.util.Locale

internal class YieldSupplyChartAxisTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScaleOf {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the tallest column WHEN scaleOf THEN the grid steps on a round value`(model: ScaleModel) {
            // Act
            val actual = YieldSupplyChartAxis.scaleOf(tallestColumn = model.tallestColumn)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ScaleModel(tallestColumn = 7.45, expected = YieldSupplyChartScale(max = 9.0, step = 3.0, decimals = 0)),
            ScaleModel(tallestColumn = 3.7, expected = YieldSupplyChartScale(max = 6.0, step = 2.0, decimals = 0)),
            ScaleModel(tallestColumn = 5.37, expected = YieldSupplyChartScale(max = 6.0, step = 2.0, decimals = 0)),
            ScaleModel(tallestColumn = 9.0, expected = YieldSupplyChartScale(max = 9.0, step = 3.0, decimals = 0)),
            ScaleModel(tallestColumn = 25.0, expected = YieldSupplyChartScale(max = 30.0, step = 10.0, decimals = 0)),
            ScaleModel(tallestColumn = 0.9, expected = YieldSupplyChartScale(max = 0.9, step = 0.3, decimals = 1)),
            ScaleModel(tallestColumn = 0.05, expected = YieldSupplyChartScale(max = 0.06, step = 0.02, decimals = 2)),
            ScaleModel(tallestColumn = 0.0, expected = YieldSupplyChartScale(max = 3.0, step = 1.0, decimals = 0)),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MonthLabels {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN dated columns WHEN monthLabels THEN each name starts at the first day of its month`(
            model: MonthLabelsModel,
        ) {
            // Arrange
            val columnStarts = List(model.columnCount) { index ->
                model.firstColumnStart.plus(index * model.columnDays, DateTimeUnit.DAY)
            }

            // Act
            val actual = YieldSupplyChartAxis.monthLabels(columnStarts = columnStarts, locale = Locale.US)

            // Assert
            assertThat(actual.map(YieldSupplyChartAxisLabel::text)).containsExactlyElementsIn(model.expectedTexts)
                .inOrder()
            assertThat(actual.map(YieldSupplyChartAxisLabel::position))
                .comparingElementsUsing(Correspondence.tolerance(POSITION_TOLERANCE))
                .containsExactlyElementsIn(model.expectedPositions)
                .inOrder()
            assertThat(actual.all { it.anchor == YieldSupplyChartAxisLabel.Anchor.Start }).isTrue()
        }

        @Test
        fun `GIVEN no dates WHEN monthLabels THEN no labels`() {
            // Act
            val actual = YieldSupplyChartAxis.monthLabels(columnStarts = emptyList(), locale = Locale.US)

            // Assert
            assertThat(actual).isEmpty()
        }

        private fun provideTestModels() = listOf(
            MonthLabelsModel(
                name = "four months of twelve-day buckets keep an even pace",
                firstColumnStart = LocalDate(year = 2026, monthNumber = 7, dayOfMonth = 3),
                columnCount = 11,
                columnDays = 12,
                expectedTexts = listOf("Jul", "Aug", "Sep", "Oct", "Nov"),
                expectedPositions = listOf(0.0, 29 / 12.0, 60 / 12.0, 90 / 12.0, 121 / 12.0),
            ),
            MonthLabelsModel(
                name = "a year of twelve-day buckets thinned to every third month",
                firstColumnStart = LocalDate(year = 2025, monthNumber = 11, dayOfMonth = 5),
                columnCount = 31,
                columnDays = 12,
                expectedTexts = listOf("Feb", "May", "Aug", "Nov"),
                expectedPositions = listOf(88 / 12.0, 177 / 12.0, 269 / 12.0, 361 / 12.0),
            ),
            MonthLabelsModel(
                name = "a year window opening in the current month leaves the opening month unnamed",
                firstColumnStart = LocalDate(year = 2025, monthNumber = 11, dayOfMonth = 7),
                columnCount = 31,
                columnDays = 12,
                expectedTexts = listOf("Feb", "May", "Aug", "Nov"),
                expectedPositions = listOf(86 / 12.0, 175 / 12.0, 267 / 12.0, 359 / 12.0),
            ),
            MonthLabelsModel(
                name = "a leading month too short to hold its name is skipped",
                firstColumnStart = LocalDate(year = 2026, monthNumber = 7, dayOfMonth = 20),
                columnCount = 4,
                columnDays = 12,
                expectedTexts = listOf("Aug", "Sep"),
                expectedPositions = listOf(1.0, 43 / 12.0),
            ),
            MonthLabelsModel(
                name = "a history inside one month keeps its only name",
                firstColumnStart = LocalDate(year = 2026, monthNumber = 10, dayOfMonth = 20),
                columnCount = 10,
                columnDays = 1,
                expectedTexts = listOf("Oct"),
                expectedPositions = listOf(0.0),
            ),
            MonthLabelsModel(
                name = "daily columns name a month at the column of its first day",
                firstColumnStart = LocalDate(year = 2026, monthNumber = 8, dayOfMonth = 6),
                columnCount = 90,
                columnDays = 1,
                expectedTexts = listOf("Aug", "Sep", "Oct", "Nov"),
                expectedPositions = listOf(0.0, 26.0, 56.0, 87.0),
            ),
        )
    }

    data class ScaleModel(val tallestColumn: Double, val expected: YieldSupplyChartScale)

    data class MonthLabelsModel(
        val name: String,
        val firstColumnStart: LocalDate,
        val columnCount: Int,
        val columnDays: Int,
        val expectedTexts: List<String>,
        val expectedPositions: List<Double>,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val POSITION_TOLERANCE = 1e-9
    }
}