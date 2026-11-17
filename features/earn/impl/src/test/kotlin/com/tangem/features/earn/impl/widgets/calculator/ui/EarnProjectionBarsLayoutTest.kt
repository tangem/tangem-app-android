package com.tangem.features.earn.impl.widgets.calculator.ui

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class EarnProjectionBarsLayoutTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BarWidth {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN chart size WHEN geometry created THEN bar is as wide as the chart allows up to the maximum`(
            model: BarWidthModel,
        ) {
            // Act
            val actual = EarnBarsGeometry(
                width = model.width,
                barCount = model.barCount,
                maxBarWidth = model.maxBarWidth,
            )

            // Assert
            assertThat(actual.barWidth).isWithin(TOLERANCE).of(model.expected)
        }

        private fun provideTestModels() = listOf(
            BarWidthModel(
                name = "capped by the maximum",
                width = 300f,
                barCount = 24,
                maxBarWidth = 6f,
                expected = 6f,
            ),
            BarWidthModel(
                name = "limited by the width",
                width = 90f,
                barCount = 5,
                maxBarWidth = 20f,
                expected = 10f,
            ),
            BarWidthModel(
                name = "single bar capped",
                width = 100f,
                barCount = 1,
                maxBarWidth = 6f,
                expected = 6f,
            ),
            BarWidthModel(
                name = "single bar narrower than the maximum",
                width = 4f,
                barCount = 1,
                maxBarWidth = 6f,
                expected = 4f,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BarPosition {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN bar index WHEN barLeft and barCenter THEN positions match the canvas layout`(
            model: BarPositionModel,
        ) {
            // Arrange
            val geometry = EarnBarsGeometry(
                width = model.width,
                barCount = model.barCount,
                maxBarWidth = MAX_BAR_WIDTH,
            )

            // Act
            val left = geometry.barLeft(model.index)
            val center = geometry.barCenter(model.index)

            // Assert
            assertThat(left).isWithin(TOLERANCE).of(model.expectedLeft)
            assertThat(center).isWithin(TOLERANCE).of(model.expectedCenter)
        }

        @Test
        fun `GIVEN many bars WHEN last bar positioned THEN it ends at the chart width`() {
            // Arrange
            val geometry = EarnBarsGeometry(width = 300f, barCount = 24, maxBarWidth = MAX_BAR_WIDTH)

            // Act
            val actual = geometry.barLeft(index = 23) + geometry.barWidth

            // Assert
            assertThat(actual).isWithin(TOLERANCE).of(300f)
        }

        private fun provideTestModels() = listOf(
            BarPositionModel(
                name = "first of 24",
                width = 300f,
                barCount = 24,
                index = 0,
                expectedLeft = 0f,
                expectedCenter = 3f,
            ),
            BarPositionModel(
                name = "second of 24",
                width = 300f,
                barCount = 24,
                index = 1,
                expectedLeft = 12.78261f,
                expectedCenter = 15.78261f,
            ),
            BarPositionModel(
                name = "middle of 24",
                width = 300f,
                barCount = 24,
                index = 12,
                expectedLeft = 153.39130f,
                expectedCenter = 156.39130f,
            ),
            BarPositionModel(
                name = "last of 24",
                width = 300f,
                barCount = 24,
                index = 23,
                expectedLeft = 294f,
                expectedCenter = 297f,
            ),
            BarPositionModel(
                name = "single bar",
                width = 100f,
                barCount = 1,
                index = 0,
                expectedLeft = 0f,
                expectedCenter = 3f,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BarAt {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN touch position WHEN barAt THEN the nearest bar is returned`(model: BarAtModel) {
            // Arrange
            val geometry = EarnBarsGeometry(
                width = model.width,
                barCount = model.barCount,
                maxBarWidth = HIT_MAX_BAR_WIDTH,
            )

            // Act
            val actual = geometry.barAt(model.x)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            BarAtModel(name = "centre of the first bar", barCount = 3, x = 5f, expected = 0),
            BarAtModel(name = "centre of the middle bar", barCount = 3, x = 50f, expected = 1),
            BarAtModel(name = "centre of the last bar", barCount = 3, x = 95f, expected = 2),
            BarAtModel(name = "gap nearer to the left bar", barCount = 3, x = 20f, expected = 0),
            BarAtModel(name = "gap nearer to the right bar", barCount = 3, x = 35f, expected = 1),
            BarAtModel(name = "exactly mid-gap goes to the right bar", barCount = 3, x = 27.5f, expected = 1),
            BarAtModel(name = "exactly mid-gap of the last pair", barCount = 3, x = 72.5f, expected = 2),
            BarAtModel(name = "left of the chart", barCount = 3, x = -10f, expected = 0),
            BarAtModel(name = "far left of the chart", barCount = 3, x = -1000f, expected = 0),
            BarAtModel(name = "right of the chart", barCount = 3, x = 150f, expected = 2),
            BarAtModel(name = "far right of the chart", barCount = 3, x = 1000f, expected = 2),
            BarAtModel(name = "single bar inside", barCount = 1, x = 50f, expected = 0),
            BarAtModel(name = "single bar left of the chart", barCount = 1, x = -5f, expected = 0),
            BarAtModel(name = "single bar right of the chart", barCount = 1, x = 500f, expected = 0),
            BarAtModel(name = "no bars", barCount = 0, x = 50f, expected = null),
            BarAtModel(name = "zero width chart", width = 0f, barCount = 3, x = 0f, expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TooltipPosition {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN bar and tooltip size WHEN earnTooltipPosition THEN tooltip sits above the bar or beside it`(
            model: TooltipModel,
        ) {
            // Act
            val actual = earnTooltipPosition(
                barCenter = model.barCenter,
                barWidth = model.barWidth,
                barTop = model.barTop,
                tooltipWidth = model.tooltipWidth,
                tooltipHeight = TOOLTIP_HEIGHT,
                chartWidth = model.chartWidth,
                spacing = SPACING,
            )

            // Assert
            assertThat(actual.x).isWithin(TOLERANCE).of(model.expected.x)
            assertThat(actual.y).isWithin(TOLERANCE).of(model.expected.y)
        }

        private fun provideTestModels() = listOf(
            TooltipModel(
                name = "fits above, centred over the bar",
                barCenter = 150f,
                barTop = 100f,
                expected = Offset(x = 110f, y = 62f),
            ),
            TooltipModel(
                name = "fits above with no spare room",
                barCenter = 150f,
                barTop = 38f,
                expected = Offset(x = 110f, y = 0f),
            ),
            TooltipModel(
                name = "no room above, bar in the right half, tooltip to the left",
                barCenter = 250f,
                barTop = 37f,
                expected = Offset(x = 159f, y = 0f),
            ),
            TooltipModel(
                name = "no room above, bar in the left half, tooltip to the right",
                barCenter = 50f,
                barTop = 37f,
                expected = Offset(x = 61f, y = 0f),
            ),
            TooltipModel(
                name = "no room above, bar exactly in the middle, tooltip to the right",
                barCenter = 150f,
                barTop = 20f,
                expected = Offset(x = 161f, y = 0f),
            ),
            TooltipModel(
                name = "fits above, clamped to the left edge",
                barCenter = 10f,
                barTop = 100f,
                expected = Offset(x = 0f, y = 62f),
            ),
            TooltipModel(
                name = "fits above, clamped to the right edge",
                barCenter = 295f,
                barTop = 100f,
                expected = Offset(x = 220f, y = 62f),
            ),
            TooltipModel(
                name = "no room above, tooltip to the left clamped to the left edge",
                barCenter = 60f,
                barTop = 20f,
                chartWidth = 100f,
                expected = Offset(x = 0f, y = 0f),
            ),
            TooltipModel(
                name = "tooltip wider than the chart",
                barCenter = 30f,
                barTop = 100f,
                chartWidth = 60f,
                expected = Offset(x = 0f, y = 62f),
            ),
        )
    }

    internal data class BarWidthModel(
        val name: String,
        val width: Float,
        val barCount: Int,
        val maxBarWidth: Float,
        val expected: Float,
    ) {
        override fun toString(): String = name
    }

    internal data class BarPositionModel(
        val name: String,
        val width: Float,
        val barCount: Int,
        val index: Int,
        val expectedLeft: Float,
        val expectedCenter: Float,
    ) {
        override fun toString(): String = name
    }

    internal data class BarAtModel(
        val name: String,
        val width: Float = HIT_CHART_WIDTH,
        val barCount: Int,
        val x: Float,
        val expected: Int?,
    ) {
        override fun toString(): String = name
    }

    internal data class TooltipModel(
        val name: String,
        val barCenter: Float,
        val barTop: Float,
        val tooltipWidth: Float = TOOLTIP_WIDTH,
        val chartWidth: Float = CHART_WIDTH,
        val barWidth: Float = BAR_WIDTH,
        val expected: Offset,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val TOLERANCE = 1e-3f
        const val MAX_BAR_WIDTH = 6f
        const val HIT_MAX_BAR_WIDTH = 10f
        const val HIT_CHART_WIDTH = 100f
        const val CHART_WIDTH = 300f
        const val BAR_WIDTH = 6f
        const val TOOLTIP_WIDTH = 80f
        const val TOOLTIP_HEIGHT = 30f
        const val SPACING = 8f
    }
}