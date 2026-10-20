package com.tangem.features.staking.impl.presentation.state.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class FeeCalculationTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CheckFeeCoverage {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN balance amount fee WHEN checkFeeCoverage THEN expected`(model: CoverageModel) {
            // Act
            val actual = checkFeeCoverage(
                isSubtractAvailable = model.isSubtract,
                balance = BigDecimal(model.balance),
                amountValue = BigDecimal(model.amount),
                feeValue = BigDecimal(model.fee),
                reduceAmountBy = model.reduceBy?.let(::BigDecimal),
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CoverageModel(isSubtract = false, "10", "10", "1", reduceBy = null, expected = false),
            CoverageModel(isSubtract = true, "10", "10", "1", reduceBy = null, expected = true),
            CoverageModel(isSubtract = true, "10", "9", "1", reduceBy = null, expected = false),
            CoverageModel(isSubtract = true, "10", "11", "1", reduceBy = null, expected = false),
            CoverageModel(isSubtract = true, "1", "1", "1", reduceBy = null, expected = false),
            CoverageModel(isSubtract = true, "10", "9", "1", reduceBy = "1", expected = true),
            CoverageModel(isSubtract = true, "10", "8", "1", reduceBy = "1", expected = false),
        )
    }

    @Nested
    inner class CheckAndCalculateSubtractedAmount {

        @Test
        fun `GIVEN no balance WHEN calculate THEN entered amount returned`() {
            // Act
            val actual = calculate(Blockchain.Solana, balance = null, amount = "5", fee = "1")

            // Assert
            assertThat(actual).isEquivalentAccordingToCompareTo(BigDecimal("5"))
        }

        @Test
        fun `GIVEN solana and fee coverage WHEN calculate THEN balance minus fee`() {
            // Act
            val actual = calculate(Blockchain.Solana, balance = "10", amount = "10", fee = "0.5")

            // Assert
            assertThat(actual).isEquivalentAccordingToCompareTo(BigDecimal("9.5"))
        }

        @Test
        fun `GIVEN reduceAmountBy WHEN calculate THEN balance minus reduce and fee`() {
            // Act
            val actual = calculate(Blockchain.Solana, balance = "10", amount = "9", fee = "0.5", reduceBy = "1")

            // Assert
            assertThat(actual).isEquivalentAccordingToCompareTo(BigDecimal("8.5"))
        }

        @Test
        fun `GIVEN no fee coverage WHEN calculate THEN entered amount returned`() {
            // Act
            val actual = calculate(Blockchain.Solana, balance = "10", amount = "5", fee = "0.5")

            // Assert
            assertThat(actual).isEquivalentAccordingToCompareTo(BigDecimal("5"))
        }

        @Test
        fun `GIVEN tron and fee coverage WHEN calculate THEN result floored to integer`() {
            // Act
            val actual = calculate(Blockchain.Tron, balance = "10.9", amount = "10.9", fee = "0.3")

            // Assert
            assertThat(actual).isEqualTo(BigDecimal("10"))
            assertThat(actual.scale()).isEqualTo(0)
        }

        // Pins current behaviour: MathContext(0, UP) is unlimited precision, so the Tron fee is never rounded up.
        @Test
        fun `GIVEN tron fractional fee WHEN calculate THEN fee is not rounded up before coverage check`() {
            // Act
            val actual = calculate(Blockchain.Tron, balance = "10", amount = "9.5", fee = "0.3")

            // Assert
            assertThat(actual).isEquivalentAccordingToCompareTo(BigDecimal("9.5"))
        }

        private fun calculate(
            blockchain: Blockchain,
            balance: String?,
            amount: String,
            fee: String,
            reduceBy: String = "0",
        ) = checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = true,
            cryptoCurrencyStatus = currencyStatus(coin(blockchain), amount = balance?.let(::BigDecimal)),
            amountValue = BigDecimal(amount),
            feeValue = BigDecimal(fee),
            reduceAmountBy = BigDecimal(reduceBy),
        )
    }

    internal data class CoverageModel(
        val isSubtract: Boolean,
        val balance: String,
        val amount: String,
        val fee: String,
        val reduceBy: String?,
        val expected: Boolean,
    )
}