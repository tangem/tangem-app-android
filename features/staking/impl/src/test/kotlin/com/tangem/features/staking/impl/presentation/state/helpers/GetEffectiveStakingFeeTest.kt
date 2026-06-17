package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetEffectiveStakingFeeTest {

    private val estimateFeeUseCase: EstimateFeeUseCase = mockk()
    private val userWallet: UserWallet = mockk(relaxed = true)

    private val getEffectiveStakingFee = GetEffectiveStakingFee(estimateFeeUseCase)

    private fun feeStatus(rawId: String): CryptoCurrencyStatus = mockk {
        every { currency.network.rawId } returns rawId
    }

    @Test
    fun `GIVEN stakekit fee present WHEN invoke THEN returns it without estimating`() = runTest {
        // Act
        val result = getEffectiveStakingFee(
            stakeKitFee = BigDecimal("0.5"),
            amount = BigDecimal.ONE,
            userWallet = userWallet,
            feeCurrencyStatus = feeStatus("solana"),
        )

        // Assert
        assertThat(result).isEqualTo(BigDecimal("0.5"))
        coVerify(exactly = 0) { estimateFeeUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN null stakekit fee AND solana WHEN invoke THEN returns client estimate`() = runTest {
        // Arrange
        val feeCurrencyStatus = feeStatus("solana")
        val estimated = BigDecimal("0.000005")
        val txFee: TransactionFee = mockk {
            every { normal.amount.value } returns estimated
        }
        coEvery { estimateFeeUseCase(any(), userWallet, feeCurrencyStatus) } returns txFee.right()

        // Act
        val result = getEffectiveStakingFee(
            stakeKitFee = null,
            amount = BigDecimal.ZERO,
            userWallet = userWallet,
            feeCurrencyStatus = feeCurrencyStatus,
        )

        // Assert
        assertThat(result).isEqualTo(estimated)
        coVerify(exactly = 1) { estimateFeeUseCase(BigDecimal.ZERO, userWallet, feeCurrencyStatus) }
    }

    @Test
    fun `GIVEN null stakekit fee AND non-solana WHEN invoke THEN returns null without estimating`() = runTest {
        // Act
        val result = getEffectiveStakingFee(
            stakeKitFee = null,
            amount = BigDecimal.ZERO,
            userWallet = userWallet,
            feeCurrencyStatus = feeStatus("ethereum"),
        )

        // Assert
        assertThat(result).isNull()
        coVerify(exactly = 0) { estimateFeeUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN null stakekit fee AND solana AND estimation fails WHEN invoke THEN returns null`() = runTest {
        // Arrange
        val feeCurrencyStatus = feeStatus("solana")
        coEvery { estimateFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()

        // Act
        val result = getEffectiveStakingFee(
            stakeKitFee = null,
            amount = BigDecimal.ZERO,
            userWallet = userWallet,
            feeCurrencyStatus = feeCurrencyStatus,
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN null fee currency status WHEN invoke THEN returns null`() = runTest {
        // Act
        val result = getEffectiveStakingFee(
            stakeKitFee = null,
            amount = BigDecimal.ZERO,
            userWallet = userWallet,
            feeCurrencyStatus = null,
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN null stakekit fee AND null amount AND solana WHEN invoke THEN estimates with zero amount`() = runTest {
        // Arrange
        val feeCurrencyStatus = feeStatus("solana")
        val estimated = BigDecimal("0.000005")
        val txFee: TransactionFee = mockk {
            every { normal.amount.value } returns estimated
        }
        coEvery {
            estimateFeeUseCase(BigDecimal.ZERO, userWallet, feeCurrencyStatus)
        } returns txFee.right()

        // Act
        val result = getEffectiveStakingFee(
            stakeKitFee = null,
            amount = null,
            userWallet = userWallet,
            feeCurrencyStatus = feeCurrencyStatus,
        )

        // Assert
        assertThat(result).isEqualTo(estimated)
        coVerify(exactly = 1) { estimateFeeUseCase(BigDecimal.ZERO, userWallet, feeCurrencyStatus) }
    }
}