package com.tangem.features.earn.lending.impl.session

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.ethFee
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.earn.product.api.session.WithdrawMode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LendingPlanTest {

    @Test
    fun `GIVEN Smart Gas plan WHEN sentGasLimits THEN the limits its quote signs`() {
        // Arrange
        val plan = withdrawPlan(fee = tokenFee(), gaslessFee = smartGasFee(callGasLimit = SIGNED_GAS))

        // Act
        val actual = plan.sentGasLimits()

        // Assert
        assertThat(actual).containsExactly(SIGNED_GAS)
    }

    @ParameterizedTest
    @MethodSource("provideFeesBelowEstimate")
    fun `GIVEN fee gas limit below the estimate WHEN isFeeBelowEstimate THEN only a coin fee is below it`(
        model: FeeBelowEstimateModel,
    ) {
        // Arrange
        val plan = withdrawPlan(fee = model.fee, gaslessFee = model.gaslessFee)

        // Act
        val actual = plan.isFeeBelowEstimate

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideFeesBelowEstimate() = listOf(
        FeeBelowEstimateModel(
            name = "coin fee",
            fee = ethFee(ESTIMATED_GAS - BigInteger.ONE),
            gaslessFee = null,
            expected = true,
        ),
        FeeBelowEstimateModel(
            name = "Smart Gas fee",
            fee = tokenFee(gasLimit = ESTIMATED_GAS - BigInteger.ONE),
            gaslessFee = smartGasFee(callGasLimit = ESTIMATED_GAS),
            expected = false,
        ),
    )

    private fun withdrawPlan(fee: Fee, gaslessFee: TransactionFeeExtended?) = LendingPlan(
        fee = fee,
        feeType = AnalyticsParam.FeeType.Normal,
        expectedAmount = BigDecimal.ONE,
        rows = emptyList(),
        withdrawMode = WithdrawMode.Instant,
        action = Withdraw(isGaslessAllowed = true),
        operation = LendingPlannedOperation.Withdraw(isEntire = false),
        amount = BigDecimal.ONE,
        txs = listOf(estimatedWithdrawal()),
        planKey = "plan",
        isWithUpgrade = false,
        gaslessFee = gaslessFee,
    )

    private fun estimatedWithdrawal(): TransactionData.Uncompiled = mockk {
        every { fee } returns ethFee(ESTIMATED_GAS)
    }

    private fun tokenFee(gasLimit: BigInteger = SIGNED_GAS): Fee = Fee.Ethereum.TokenCurrency(
        amount = Amount(currencySymbol = "USDC", value = BigDecimal("1.5"), decimals = 6),
        gasLimit = gasLimit,
        coinPriceInToken = BigInteger.ONE,
        feeTransferGasLimit = BigInteger.ZERO,
        baseGas = BigInteger.ZERO,
    )

    private fun smartGasFee(callGasLimit: BigInteger) = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = tokenFee()),
        feeTokenId = mockk(),
        callGasLimits = listOf(callGasLimit),
        gaslessProtocolData = GaslessProtocolData.Evm,
    )

    data class FeeBelowEstimateModel(
        val name: String,
        val fee: Fee,
        val gaslessFee: TransactionFeeExtended?,
        val expected: Boolean,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val ESTIMATED_GAS: BigInteger = BigInteger.valueOf(150_000)
        val SIGNED_GAS: BigInteger = BigInteger.valueOf(210_000)
    }
}