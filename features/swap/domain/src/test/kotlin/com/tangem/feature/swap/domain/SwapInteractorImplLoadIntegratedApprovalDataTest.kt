package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.feature.swap.domain.models.ui.IntegratedApprovalData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Tests for [SwapInteractorImpl.loadIntegratedApprovalData].
 *
 * Builds the ERC-20 approval transaction (via [createApprovalTransactionUseCase]) and loads its
 * [TransactionFee] (via [getFeeUseCase]). Honors [ApproveType]:
 *  - `LIMITED`   → approval amount = the passed swap amount.
 *  - `UNLIMITED` → approval amount = null (unbounded allowance).
 *
 * Error surfaces:
 *  - non-Token from-currency → `Left(GetFeeError.DataError)`.
 *  - `createApprovalTransactionUseCase` Left (throwable) → `Left(GetFeeError.DataError)`.
 *  - `getFeeUseCase` Left → propagated as Left verbatim.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplLoadIntegratedApprovalDataTest : SwapInteractorImplTestBase() {

    private val approvalTx = mockk<TransactionData.Uncompiled>(relaxed = true)
    private val approvalFee = TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true))

    @BeforeEach
    fun setup() {
        coEvery {
            createApprovalTransactionUseCase.invoke(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = any(),
                contractAddress = any(),
                spenderAddress = any(),
            )
        } returns approvalTx.right()
        coEvery {
            getFeeUseCase.invoke(
                transactionData = any(),
                userWallet = any(),
                network = any(),
            )
        } returns approvalFee.right()
    }

    @Test
    fun `GIVEN LIMITED THEN approval amount equals the swap amount`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
        val amountCaptures = mutableListOf<BigDecimal?>()
        coEvery {
            createApprovalTransactionUseCase.invoke(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = captureNullable(amountCaptures),
                contractAddress = any(),
                spenderAddress = any(),
            )
        } returns approvalTx.right()

        val result = sut.loadIntegratedApprovalData(
            fromStatus = fromStatus,
            spenderAddress = SPENDER,
            approveType = ApproveType.LIMITED,
            approvalAmount = SWAP_AMOUNT,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { data ->
            assertThat(data).isInstanceOf(IntegratedApprovalData::class.java)
            assertThat(data.approveType).isEqualTo(ApproveType.LIMITED)
            assertThat(data.approvalFee).isEqualTo(approvalFee)
            assertThat(data.approvalTransaction).isEqualTo(approvalTx)
        }
        assertThat(amountCaptures.single()).isEqualTo(SWAP_AMOUNT)
    }

    @Test
    fun `GIVEN UNLIMITED THEN approval amount is null`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
        val amountCaptures = mutableListOf<BigDecimal?>()
        coEvery {
            createApprovalTransactionUseCase.invoke(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = captureNullable(amountCaptures),
                contractAddress = any(),
                spenderAddress = any(),
            )
        } returns approvalTx.right()

        val result = sut.loadIntegratedApprovalData(
            fromStatus = fromStatus,
            spenderAddress = SPENDER,
            approveType = ApproveType.UNLIMITED,
            approvalAmount = SWAP_AMOUNT,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { data -> assertThat(data.approveType).isEqualTo(ApproveType.UNLIMITED) }
        assertThat(amountCaptures.single()).isNull()
    }

    @Test
    fun `GIVEN non-Token from-currency THEN returns Left DataError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(isCoin = true)

        val result = sut.loadIntegratedApprovalData(
            fromStatus = fromStatus,
            spenderAddress = SPENDER,
            approveType = ApproveType.LIMITED,
            approvalAmount = SWAP_AMOUNT,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error -> assertThat(error).isInstanceOf(GetFeeError.DataError::class.java) }
        coVerify(exactly = 0) {
            createApprovalTransactionUseCase.invoke(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = any(),
                contractAddress = any(),
                spenderAddress = any(),
            )
        }
    }

    @Test
    fun `GIVEN createApprovalTransaction Left THEN returns Left DataError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
        coEvery {
            createApprovalTransactionUseCase.invoke(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = any(),
                contractAddress = any(),
                spenderAddress = any(),
            )
        } returns IllegalStateException("cannot build approval tx").left()

        val result = sut.loadIntegratedApprovalData(
            fromStatus = fromStatus,
            spenderAddress = SPENDER,
            approveType = ApproveType.LIMITED,
            approvalAmount = SWAP_AMOUNT,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error -> assertThat(error).isInstanceOf(GetFeeError.DataError::class.java) }
        coVerify(exactly = 0) {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        }
    }

    @Test
    fun `GIVEN getFeeUseCase Left THEN propagates the Left error verbatim`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
        coEvery {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        } returns GetFeeError.BlockchainErrors.TronActivationError.left()

        val result = sut.loadIntegratedApprovalData(
            fromStatus = fromStatus,
            spenderAddress = SPENDER,
            approveType = ApproveType.LIMITED,
            approvalAmount = SWAP_AMOUNT,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isEqualTo(GetFeeError.BlockchainErrors.TronActivationError)
        }
    }

    private companion object {
        const val SPENDER = "0xSpender"
        const val CONTRACT = "0xContract"
        val SWAP_AMOUNT: BigDecimal = BigDecimal("12.34")
    }
}