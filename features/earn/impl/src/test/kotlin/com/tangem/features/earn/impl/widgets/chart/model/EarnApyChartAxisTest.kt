package com.tangem.features.earn.impl.widgets.chart.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyScale
import com.tangem.test.core.ProvideTestModels
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.util.Locale

internal class EarnApyChartAxisTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScaleOf {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the tallest column WHEN scaleOf THEN the grid steps on a round value`(model: ScaleModel) {
            // Act
            val actual = EarnApyChartAxis.scaleOf(tallestColumn = model.tallestColumn)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ScaleModel(tallestColumn = 7.45, expected = EarnApyScale(max = 9.0, step = 3.0, decimals = 0)),
            ScaleModel(tallestColumn = 3.7, expected = EarnApyScale(max = 6.0, step = 2.0, decimals = 0)),
            ScaleModel(tallestColumn = 5.37, expected = EarnApyScale(max = 6.0, step = 2.0, decimals = 0)),
            ScaleModel(tallestColumn = 9.0, expected = EarnApyScale(max = 9.0, step = 3.0, decimals = 0)),
            ScaleModel(tallestColumn = 25.0, expected = EarnApyScale(max = 30.0, step = 10.0, decimals = 0)),
            ScaleModel(tallestColumn = 0.9, expected = EarnApyScale(max = 0.9, step = 0.3, decimals = 1)),
            ScaleModel(tallestColumn = 0.05, expected = EarnApyScale(max = 0.06, step = 0.02, decimals = 2)),
            ScaleModel(tallestColumn = 0.0, expected = EarnApyScale(max = 3.0, step = 1.0, decimals = 0)),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MonthLabels {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN dated columns WHEN monthLabels THEN the months the columns reach into are named oldest first`(
            model: MonthLabelsModel,
        ) {
            // Arrange
            val columnStarts = List(model.columnCount) { index ->
                model.firstColumnStart.plus(index * model.columnDays, DateTimeUnit.DAY)
            }

            // Act
            val actual = EarnApyChartAxis.monthLabels(columnStarts = columnStarts, locale = Locale.US)

            // Assert
            assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
        }

        @Test
        fun `GIVEN no dates WHEN monthLabels THEN no labels`() {
            // Act
            val actual = EarnApyChartAxis.monthLabels(columnStarts = emptyList(), locale = Locale.US)

            // Assert
            assertThat(actual).isEmpty()
        }

        private fun provideTestModels() = listOf(
            MonthLabelsModel(
                name = "five months of twelve-day buckets name every month",
                firstColumnStart = LocalDate(year = 2026, monthNumber = 7, dayOfMonth = 3),
                columnCount = 11,
                columnDays = 12,
                expected = listOf("Jul", "Aug", "Sep", "Oct", "Nov"),
            ),
            MonthLabelsModel(
                name = "six months of weekly buckets name every month",
                firstColumnStart = LocalDate(year = 2025, monthNumber = 12, dayOfMonth = 29),
                columnCount = 26,
                columnDays = 7,
                expected = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun"),
            ),
            MonthLabelsModel(
                name = "a year of twelve-day buckets thinned to every second month",
                firstColumnStart = LocalDate(year = 2025, monthNumber = 11, dayOfMonth = 5),
                columnCount = 31,
                columnDays = 12,
                expected = listOf("Jan", "Mar", "May", "Jul", "Sep", "Nov"),
            ),
            MonthLabelsModel(
                name = "a year window opening in the current month leaves the opening month unnamed",
                firstColumnStart = LocalDate(year = 2025, monthNumber = 11, dayOfMonth = 7),
                columnCount = 31,
                columnDays = 12,
                expected = listOf("Jan", "Mar", "May", "Jul", "Sep", "Nov"),
            ),
            MonthLabelsModel(
                name = "a leading month too short to hold its name is skipped",
                firstColumnStart = LocalDate(year = 2026, monthNumber = 7, dayOfMonth = 20),
                columnCount = 4,
                columnDays = 12,
                expected = listOf("Aug", "Sep"),
            ),
            MonthLabelsModel(
                name = "a history inside one month keeps its only name",
                firstColumnStart = LocalDate(year = 2026, monthNumber = 10, dayOfMonth = 20),
                columnCount = 10,
                columnDays = 1,
                expected = listOf("Oct"),
            ),
            MonthLabelsModel(
                name = "daily columns name every month the window reaches into",
                firstColumnStart = LocalDate(year = 2026, monthNumber = 8, dayOfMonth = 6),
                columnCount = 90,
                columnDays = 1,
                expected = listOf("Aug", "Sep", "Oct", "Nov"),
            ),
        )
    }

    data class ScaleModel(val tallestColumn: Double, val expected: EarnApyScale)

    data class MonthLabelsModel(
        val name: String,
        val firstColumnStart: LocalDate,
        val columnCount: Int,
        val columnDays: Int,
        val expected: List<String>,
    ) {
        override fun toString(): String = name
    }
}