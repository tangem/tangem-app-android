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
            AvailableModel(held = "0.634567", expected = "0.63"),
            AvailableModel(held = "0.004567", expected = "0.00"),
            AvailableModel(held = "0", expected = "0.00"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Minimum {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN available shares WHEN minimum THEN one share, or the whole remainder below one`(
            model: MinimumModel,
        ) {
            // Act
            val actual = SharesCalculator.minimum(available = BigDecimal(model.available))

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
        }

        private fun provideTestModels(): List<MinimumModel> = listOf(
            MinimumModel(available = "51.23", expected = "1"),
            MinimumModel(available = "1.00", expected = "1.00"),
            MinimumModel(available = "0.63", expected = "0.63"),
            MinimumModel(available = "0.00", expected = "0.00"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Fraction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a percent WHEN fraction THEN whole shares, never below the minimum`(model: FractionModel) {
            // Arrange
            val available = BigDecimal(model.available)

            // Act
            val actual = SharesCalculator.fraction(available = available, percent = model.percent)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
            assertThat(actual).isAtMost(available)
        }

        private fun provideTestModels(): List<FractionModel> = listOf(
            FractionModel(available = "51.23", percent = 25, expected = "12"),
            FractionModel(available = "51.23", percent = 50, expected = "25"),
            FractionModel(available = "51.23", percent = 75, expected = "38"),
            FractionModel(available = "3.00", percent = 25, expected = "1"),
            FractionModel(available = "1.00", percent = 25, expected = "1.00"),
            FractionModel(available = "0.63", percent = 25, expected = "0.63"),
            FractionModel(available = "0.00", percent = 50, expected = "0"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FullFraction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the full percentage WHEN fraction THEN the position closes to its last fraction`(model: FullCloseModel) {
            // Arrange
            val available = BigDecimal(model.available)

            // Act
            val actual = SharesCalculator.fraction(available = available, percent = 100)

            // Assert
            assertThat(actual).isEqualTo(available)
        }

        private fun provideTestModels(): List<FullCloseModel> = listOf(
            FullCloseModel(available = "51.23"),
            FullCloseModel(available = "1.00"),
            FullCloseModel(available = "0.63"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Increased {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a selection WHEN increased THEN one whole share more and capped at available`(model: StepModel) {
            // Arrange
            val available = BigDecimal(model.available)

            // Act
            val actual = SharesCalculator.increased(selected = BigDecimal(model.selected), available = available)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
            assertThat(actual).isAtMost(available)
        }

        private fun provideTestModels(): List<StepModel> = listOf(
            StepModel(selected = "1.00", available = "51.23", expected = "2"),
            StepModel(selected = "12", available = "51.23", expected = "13"),
            StepModel(selected = "50", available = "51.23", expected = "51"),
            StepModel(selected = "51.23", available = "51.23", expected = "51.23"),
            StepModel(selected = "0.63", available = "0.63", expected = "0.63"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Decreased {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a selection WHEN decreased THEN one whole share less and never below the minimum`(
            model: StepModel,
        ) {
            // Arrange
            val available = BigDecimal(model.available)

            // Act
            val actual = SharesCalculator.decreased(selected = BigDecimal(model.selected), available = available)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
            assertThat(actual).isAtLeast(SharesCalculator.minimum(available))
        }

        private fun provideTestModels(): List<StepModel> = listOf(
            StepModel(selected = "51.23", available = "51.23", expected = "50"),
            StepModel(selected = "13", available = "51.23", expected = "12"),
            StepModel(selected = "1.00", available = "51.23", expected = "1"),
            StepModel(selected = "1.00", available = "1.00", expected = "1.00"),
            StepModel(selected = "0.63", available = "0.63", expected = "0.63"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsSteppable {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN available shares WHEN isSteppable THEN more than the minimum can be selected`(
            model: SteppableModel,
        ) {
            // Act
            val actual = SharesCalculator.isSteppable(available = BigDecimal(model.available))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<SteppableModel> = listOf(
            SteppableModel(available = "0.00", expected = false),
            SteppableModel(available = "0.63", expected = false),
            SteppableModel(available = "1.00", expected = false),
            SteppableModel(available = "2.00", expected = true),
            SteppableModel(available = "51.23", expected = true),
        )
    }

    internal data class AvailableModel(val held: String, val expected: String)

    internal data class MinimumModel(val available: String, val expected: String)

    internal data class FullCloseModel(val available: String)

    internal data class FractionModel(val available: String, val percent: Int, val expected: String)

    internal data class StepModel(val selected: String, val available: String, val expected: String)

    internal data class SteppableModel(val available: String, val expected: Boolean)
}