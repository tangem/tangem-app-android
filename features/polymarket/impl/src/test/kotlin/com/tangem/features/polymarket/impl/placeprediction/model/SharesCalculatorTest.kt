package com.tangem.features.polymarket.impl.placeprediction.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SharesCalculatorTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Available {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN held shares WHEN available THEN truncated down onto the two-decimal grid`(model: AvailableModel) {
            // Arrange
            val held = BigDecimal(model.held)

            // Act
            val actual = SharesCalculator.available(held)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
        }

        private fun provideTestModels(): List<AvailableModel> = listOf(
            AvailableModel(held = "51.234567", expected = "51.23"),
            AvailableModel(held = "51.239999", expected = "51.23"),
            AvailableModel(held = "51.23", expected = "51.23"),
            AvailableModel(held = "51.2", expected = "51.20"),
            AvailableModel(held = "0.004567", expected = "0.00"),
            AvailableModel(held = "0", expected = "0.00"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Fraction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a percent WHEN fraction THEN truncated down and never above available`(model: FractionModel) {
            // Arrange
            val available = BigDecimal(model.available)

            // Act
            val actual = SharesCalculator.fraction(available = available, percent = model.percent)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
            assertThat(actual).isAtMost(available)
        }

        private fun provideTestModels(): List<FractionModel> = listOf(
            FractionModel(available = "51.23", percent = 25, expected = "12.80"),
            FractionModel(available = "51.23", percent = 50, expected = "25.61"),
            FractionModel(available = "51.23", percent = 75, expected = "38.42"),
            FractionModel(available = "51.23", percent = 100, expected = "51.23"),
            FractionModel(available = "1.00", percent = 75, expected = "0.75"),
            FractionModel(available = "0.50", percent = 25, expected = "0.12"),
            FractionModel(available = "0.00", percent = 50, expected = "0.00"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Increased {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a selection WHEN increased THEN one share more and clamped at available`(model: StepUpModel) {
            // Arrange
            val available = BigDecimal(model.available)

            // Act
            val actual = SharesCalculator.increased(selected = BigDecimal(model.selected), available = available)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
            assertThat(actual).isAtMost(available)
        }

        private fun provideTestModels(): List<StepUpModel> = listOf(
            StepUpModel(selected = "0.00", available = "51.23", expected = "1.00"),
            StepUpModel(selected = "12.80", available = "51.23", expected = "13.80"),
            StepUpModel(selected = "50.23", available = "51.23", expected = "51.23"),
            StepUpModel(selected = "51.23", available = "51.23", expected = "51.23"),
            StepUpModel(selected = "0.00", available = "0.50", expected = "0.50"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Decreased {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a selection WHEN decreased THEN one share less and never below zero`(model: StepDownModel) {
            // Act
            val actual = SharesCalculator.decreased(selected = BigDecimal(model.selected))

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
        }

        private fun provideTestModels(): List<StepDownModel> = listOf(
            StepDownModel(selected = "51.23", expected = "50.23"),
            StepDownModel(selected = "13.80", expected = "12.80"),
            StepDownModel(selected = "1.00", expected = "0.00"),
            StepDownModel(selected = "0.50", expected = "0.00"),
            StepDownModel(selected = "0.00", expected = "0.00"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsSteppable {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN available shares WHEN isSteppable THEN a whole share fits`(model: SteppableModel) {
            // Act
            val actual = SharesCalculator.isSteppable(available = BigDecimal(model.available))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<SteppableModel> = listOf(
            SteppableModel(available = "0.00", expected = false),
            SteppableModel(available = "0.99", expected = false),
            SteppableModel(available = "1.00", expected = true),
            SteppableModel(available = "51.23", expected = true),
        )
    }

    internal data class AvailableModel(val held: String, val expected: String)

    internal data class FractionModel(val available: String, val percent: Int, val expected: String)

    internal data class StepUpModel(val selected: String, val available: String, val expected: String)

    internal data class StepDownModel(val selected: String, val expected: String)

    internal data class SteppableModel(val available: String, val expected: Boolean)
}