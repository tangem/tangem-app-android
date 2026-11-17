package com.tangem.features.earn.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnRowKind
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class EarnAnalyticsValuesTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PickerTabValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN picker tab WHEN toAnalyticsValue THEN spec string`(model: ValueModel<EarnPickerTab>) {
            // Act
            val actual = model.input.toAnalyticsValue()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ValueModel(EarnPickerTab.Calculator, "Calculator"),
            ValueModel(EarnPickerTab.Benefits, "Benefits"),
            ValueModel(EarnPickerTab.Details, "Details"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HorizonValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN horizon WHEN toAnalyticsValue THEN spec string`(model: ValueModel<EarnHorizon>) {
            // Act
            val actual = model.input.toAnalyticsValue()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ValueModel(EarnHorizon.Months6, "6M"),
            ValueModel(EarnHorizon.Year1, "1Y"),
            ValueModel(EarnHorizon.Year3, "3Y"),
            ValueModel(EarnHorizon.Year5, "5Y"),
            ValueModel(EarnHorizon.Year10, "10Y"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RowKindValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN row kind WHEN toAnalyticsValue THEN spec string`(model: ValueModel<EarnRowKind>) {
            // Act
            val actual = model.input.toAnalyticsValue()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ValueModel(EarnRowKind.Risk, "Risk"),
            ValueModel(EarnRowKind.Protocol, "Protocol"),
            ValueModel(EarnRowKind.Withdrawals, "Withdrawals"),
            ValueModel(EarnRowKind.PerformanceFee, "Performance Fee"),
            ValueModel(EarnRowKind.Audits, "Audits"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CalculatorInputValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN calculator input WHEN toAnalyticsValue THEN spec string`(
            model: ValueModel<EarnCalculatorComponent.Input>,
        ) {
            // Act
            val actual = model.input.toAnalyticsValue()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ValueModel(EarnCalculatorComponent.Input.InitialBalance, "Initial Balance"),
            ValueModel(EarnCalculatorComponent.Input.MonthlyTopUp, "Monthly Top-Up"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddFundsSourceValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN add funds source WHEN value read THEN spec string`(model: ValueModel<EarnAddFundsSource>) {
            // Act
            val actual = model.input.value

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ValueModel(EarnAddFundsSource.YieldPicker, "Yield Picker"),
            ValueModel(EarnAddFundsSource.InfoScreen, "Info Screen"),
        )
    }

    internal data class ValueModel<T>(val input: T, val expected: String) {
        override fun toString(): String = "$input -> $expected"
    }
}