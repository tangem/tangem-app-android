package com.tangem.features.earn.impl.widgets.calculator.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class CalculatorInputTest {

    @Test
    fun `GIVEN Default WHEN read THEN 2500 and 500 and 3 years`() {
        // Act
        val actual = CalculatorInput.Default

        // Assert
        assertThat(actual.initialFiat).isEquivalentAccordingToCompareTo(BigDecimal(2500))
        assertThat(actual.monthlyTopUpFiat).isEquivalentAccordingToCompareTo(BigDecimal(500))
        assertThat(actual.horizon).isEqualTo(EarnHorizon.Year3)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StepInitial {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN initial balance WHEN stepInitial THEN stepped by 1000 within bounds`(model: StepModel) {
            // Act
            val actual = createInput(initial = model.from).stepInitial(model.direction)

            // Assert
            assertThat(actual.initialFiat).isEquivalentAccordingToCompareTo(BigDecimal(model.expected))
        }

        @Test
        fun `GIVEN input WHEN stepInitial THEN top-up and horizon are untouched`() {
            // Act
            val actual = createInput().stepInitial(direction = 1)

            // Assert
            assertThat(actual.monthlyTopUpFiat).isEquivalentAccordingToCompareTo(BigDecimal(500))
            assertThat(actual.horizon).isEqualTo(EarnHorizon.Year3)
        }

        private fun provideTestModels() = listOf(
            StepModel(name = "up from default", from = 2500, direction = 1, expected = 3500),
            StepModel(name = "down from default", from = 2500, direction = -1, expected = 1500),
            StepModel(name = "down below zero clamps to zero", from = 500, direction = -1, expected = 0),
            StepModel(name = "down at zero stays", from = 0, direction = -1, expected = 0),
            StepModel(name = "up above max clamps to max", from = 999_500, direction = 1, expected = 1_000_000),
            StepModel(name = "up at max stays", from = 1_000_000, direction = 1, expected = 1_000_000),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StepMonthlyTopUp {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN top-up WHEN stepMonthlyTopUp THEN stepped by 100 within bounds`(model: StepModel) {
            // Act
            val actual = createInput(topUp = model.from).stepMonthlyTopUp(model.direction)

            // Assert
            assertThat(actual.monthlyTopUpFiat).isEquivalentAccordingToCompareTo(BigDecimal(model.expected))
        }

        @Test
        fun `GIVEN input WHEN stepMonthlyTopUp THEN initial balance and horizon are untouched`() {
            // Act
            val actual = createInput().stepMonthlyTopUp(direction = 1)

            // Assert
            assertThat(actual.initialFiat).isEquivalentAccordingToCompareTo(BigDecimal(2500))
            assertThat(actual.horizon).isEqualTo(EarnHorizon.Year3)
        }

        private fun provideTestModels() = listOf(
            StepModel(name = "up from default", from = 500, direction = 1, expected = 600),
            StepModel(name = "down from default", from = 500, direction = -1, expected = 400),
            StepModel(name = "down below zero clamps to zero", from = 50, direction = -1, expected = 0),
            StepModel(name = "down at zero stays", from = 0, direction = -1, expected = 0),
            StepModel(name = "up above max clamps to max", from = 999_950, direction = 1, expected = 1_000_000),
            StepModel(name = "up at max stays", from = 1_000_000, direction = 1, expected = 1_000_000),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Flags {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN input WHEN reading flags THEN a button is disabled only at its bound`(model: FlagsModel) {
            // Act
            val actual = listOf(
                model.input.canDecreaseInitial,
                model.input.canIncreaseInitial,
                model.input.canDecreaseTopUp,
                model.input.canIncreaseTopUp,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FlagsModel(name = "default", input = CalculatorInput.Default, expected = listOf(true, true, true, true)),
            FlagsModel(
                name = "initial at zero",
                input = createInput(initial = 0),
                expected = listOf(false, true, true, true),
            ),
            FlagsModel(
                name = "initial at max",
                input = createInput(initial = 1_000_000),
                expected = listOf(true, false, true, true),
            ),
            FlagsModel(
                name = "top-up at zero",
                input = createInput(topUp = 0),
                expected = listOf(true, true, false, true),
            ),
            FlagsModel(
                name = "top-up at max",
                input = createInput(topUp = 1_000_000),
                expected = listOf(true, true, true, false),
            ),
        )
    }

    internal data class StepModel(val name: String, val from: Long, val direction: Int, val expected: Long) {
        override fun toString(): String = "$name: $from ($direction) -> $expected"
    }

    internal data class FlagsModel(val name: String, val input: CalculatorInput, val expected: List<Boolean>) {
        override fun toString(): String = "$name -> $expected"
    }

    private fun createInput(
        initial: Long = 2500,
        topUp: Long = 500,
        horizon: EarnHorizon = EarnHorizon.Year3,
    ) = CalculatorInput(
        initialFiat = BigDecimal(initial),
        monthlyTopUpFiat = BigDecimal(topUp),
        horizon = horizon,
    )
}