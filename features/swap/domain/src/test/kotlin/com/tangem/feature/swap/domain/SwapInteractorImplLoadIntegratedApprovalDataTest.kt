package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
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
import java.math.BigInteger

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

    // region patchIntegratedApprovalPriorityFee — INCREASE_GAS_PRICE_FOR_INTEGRATED_APPROVAL (115 = +15% gas-price)

    /**
     * The loaded approval fee is patched via
     * [com.tangem.lib.crypto.BlockchainFeeUtils.patchIntegratedApprovalPriorityFee] before being
     * returned. Scales the **gas-price** fields (Legacy `gasPrice`; EIP1559
     * `maxFeePerGas` and `priorityFee`) and the derived `amount` for [Fee.Ethereum] legs by 15%;
     * The new `amount` is recomputed from `gasLimit * newGasPrice` shifted left by `decimals`,
     * independent of the input amount value.
     */
    @Test
    fun `GIVEN Ethereum Legacy Single fee WHEN loaded THEN gasPrice and amount bumped by 15 percent`() = runTest {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
        val initialFee = Fee.Ethereum.Legacy(
            amount = ethAmount(BigDecimal("0.002")), // gasLimit * gasPrice / 1e18 = 100_000 * 20e9 / 1e18
            gasLimit = BigInteger.valueOf(100_000),
            gasPrice = BigInteger.valueOf(20_000_000_000),
        )
        coEvery {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        } returns TransactionFee.Single(normal = initialFee).right()

        // Act
        val result = loadLimited(fromStatus)

        // Assert
        val patched = result.singleNormal<Fee.Ethereum.Legacy>()
        // gasLimit is NOT changed by this patch (it bumps gas-price only)
        assertThat(patched.gasLimit).isEqualTo(BigInteger.valueOf(100_000))
        // 20_000_000_000 * 115 / 100 = 23_000_000_000
        assertThat(patched.gasPrice).isEqualTo(BigInteger.valueOf(23_000_000_000))
        // amount recomputed from gasLimit * newGasPrice: 100_000 * 23e9 / 1e18 = 0.0023
        assertThat(patched.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.0023"))
        // amount decimals preserved
        assertThat(patched.amount.decimals).isEqualTo(18)
    }

    @Test
    fun `GIVEN Ethereum EIP1559 Single fee WHEN loaded THEN gas-price fields bumped AND gasLimit untouched`() =
        runTest {
            // Arrange
            val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
            val initialFee = Fee.Ethereum.EIP1559(
                amount = ethAmount(BigDecimal("0.0032")), // gasLimit * maxFeePerGas / 1e18 = 80_000 * 40e9 / 1e18
                gasLimit = BigInteger.valueOf(80_000),
                maxFeePerGas = BigInteger.valueOf(40_000_000_000),
                priorityFee = BigInteger.valueOf(2_000_000_000),
            )
            coEvery {
                getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
            } returns TransactionFee.Single(normal = initialFee).right()

            // Act
            val result = loadLimited(fromStatus)

            // Assert
            val patched = result.singleNormal<Fee.Ethereum.EIP1559>()
            // gasLimit is NOT changed by this patch (it bumps gas-price only)
            assertThat(patched.gasLimit).isEqualTo(BigInteger.valueOf(80_000))
            // EIP1559 gas-price fields scaled by 115 / 100
            assertThat(patched.maxFeePerGas).isEqualTo(BigInteger.valueOf(46_000_000_000))
            assertThat(patched.priorityFee).isEqualTo(BigInteger.valueOf(2_300_000_000))
            // amount recomputed from gasLimit * newMaxFeePerGas: 80_000 * 46e9 / 1e18 = 0.00368
            assertThat(patched.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00368"))
        }

    @Test
    fun `GIVEN Choosable Ethereum fee WHEN loaded THEN all three legs gasPrice bumped by 15 percent`() = runTest {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
        val gasPrice = BigInteger.valueOf(20_000_000_000)
        val choosable = TransactionFee.Choosable(
            minimum = Fee.Ethereum.Legacy(
                amount = ethAmount(BigDecimal("0.0008")), // 40_000 * 20e9 / 1e18
                gasLimit = BigInteger.valueOf(40_000),
                gasPrice = gasPrice,
            ),
            normal = Fee.Ethereum.Legacy(
                amount = ethAmount(BigDecimal("0.0016")), // 80_000 * 20e9 / 1e18
                gasLimit = BigInteger.valueOf(80_000),
                gasPrice = gasPrice,
            ),
            priority = Fee.Ethereum.Legacy(
                amount = ethAmount(BigDecimal("0.0024")), // 120_000 * 20e9 / 1e18
                gasLimit = BigInteger.valueOf(120_000),
                gasPrice = gasPrice,
            ),
        )
        coEvery {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        } returns choosable.right()

        // Act
        val patched = (loadLimited(fromStatus).feeOrFail() as TransactionFee.Choosable)

        // Assert — every leg's gas-price scaled (gasPrice * 115 / 100 = 23e9), gasLimit unchanged
        val newGasPrice = BigInteger.valueOf(23_000_000_000)
        assertThat((patched.minimum as Fee.Ethereum.Legacy).gasLimit).isEqualTo(BigInteger.valueOf(40_000))
        assertThat((patched.minimum as Fee.Ethereum.Legacy).gasPrice).isEqualTo(newGasPrice)
        assertThat((patched.normal as Fee.Ethereum.Legacy).gasLimit).isEqualTo(BigInteger.valueOf(80_000))
        assertThat((patched.normal as Fee.Ethereum.Legacy).gasPrice).isEqualTo(newGasPrice)
        assertThat((patched.priority as Fee.Ethereum.Legacy).gasLimit).isEqualTo(BigInteger.valueOf(120_000))
        assertThat((patched.priority as Fee.Ethereum.Legacy).gasPrice).isEqualTo(newGasPrice)
    }

    @Test
    fun `GIVEN non-Ethereum approval fee WHEN loaded THEN fee is returned unchanged`() = runTest {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
        val commonFee = Fee.Common(amount = ethAmount(BigDecimal("0.5"), decimals = 8))
        coEvery {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        } returns TransactionFee.Single(normal = commonFee).right()

        // Act
        val result = loadLimited(fromStatus)

        // Assert — non-Ethereum legs pass through untouched (same instance)
        assertThat(result.singleNormal<Fee.Common>()).isSameInstanceAs(commonFee)
    }

    @Test
    fun `GIVEN Ethereum Legacy fee with zero gasLimit WHEN loaded THEN gasPrice bumped and amount is zero`() =
        runTest {
            // Arrange
            val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
            val zeroGasFee = Fee.Ethereum.Legacy(
                amount = ethAmount(BigDecimal("0.000002")),
                gasLimit = BigInteger.ZERO,
                gasPrice = BigInteger.valueOf(20_000_000_000),
            )
            coEvery {
                getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
            } returns TransactionFee.Single(normal = zeroGasFee).right()

            // Act
            val result = loadLimited(fromStatus)

            // Assert — the gas-price path does NOT short-circuit on zero gasLimit (unlike the
            // gas-limit path); gasPrice is still bumped and amount recomputes to gasLimit(0) * price = 0
            val patched = result.singleNormal<Fee.Ethereum.Legacy>()
            assertThat(patched.gasLimit).isEqualTo(BigInteger.ZERO)
            assertThat(patched.gasPrice).isEqualTo(BigInteger.valueOf(23_000_000_000))
            assertThat(patched.amount.value).isEquivalentAccordingToCompareTo(BigDecimal.ZERO)
        }

    @Test
    fun `GIVEN Ethereum TokenCurrency approval fee WHEN loaded THEN returns Left DataError wrapping [REDACTED_TASK_KEY]`() =
        runTest {
            // Arrange
            val fromStatus = buildSwapCurrencyStatus(isCoin = false, contractAddress = CONTRACT)
            val tokenFee = Fee.Ethereum.TokenCurrency(
                amount = ethAmount(BigDecimal("0.001")),
                gasLimit = BigInteger.valueOf(100_000),
                coinPriceInToken = BigInteger.ONE,
                feeTransferGasLimit = BigInteger.valueOf(50_000),
                baseGas = BigInteger.valueOf(21_000),
            )
            coEvery {
                getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
            } returns TransactionFee.Single(normal = tokenFee).right()

            // Act — the patch throws IllegalStateException, but the fee load is wrapped in
            // runSuspendCatching ([REDACTED_TASK_KEY]) so it is caught and converted to Left(DataError)
            // instead of crashing the DEX swap flow.
            val result = loadLimited(fromStatus)

            // Assert
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(GetFeeError.DataError::class.java)
                val cause = (error as GetFeeError.DataError).cause
                assertThat(cause).isInstanceOf(IllegalStateException::class.java)
                assertThat(cause?.message).contains("[REDACTED_TASK_KEY]")
            }
        }

    // endregion

    private suspend fun loadLimited(fromStatus: com.tangem.domain.swap.models.SwapCurrencyStatus) =
        sut.loadIntegratedApprovalData(
            fromStatus = fromStatus,
            spenderAddress = SPENDER,
            approveType = ApproveType.LIMITED,
            approvalAmount = SWAP_AMOUNT,
        )

    /** Unwraps a Right result into its [TransactionFee], failing the test on Left. */
    private fun arrow.core.Either<GetFeeError, IntegratedApprovalData>.feeOrFail(): TransactionFee {
        assertThat(isRight()).isTrue()
        return getOrNull()!!.approvalFee
    }

    /** Unwraps a Right result into the `normal` leg of a [TransactionFee.Single], cast to [T]. */
    private inline fun <reified T : Fee> arrow.core.Either<GetFeeError, IntegratedApprovalData>.singleNormal(): T {
        return (feeOrFail() as TransactionFee.Single).normal as T
    }

    private fun ethAmount(value: BigDecimal, decimals: Int = 18): Amount = Amount(
        currencySymbol = "ETH",
        value = value,
        decimals = decimals,
    )

    private companion object {
        const val SPENDER = "0xSpender"
        const val CONTRACT = "0xContract"
        val SWAP_AMOUNT: BigDecimal = BigDecimal("12.34")
    }
}