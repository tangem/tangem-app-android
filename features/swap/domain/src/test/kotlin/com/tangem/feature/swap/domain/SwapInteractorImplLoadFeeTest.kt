package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Tests for [SwapInteractorImpl.loadFeeForSwapTransaction] (both overloads).
 *
 * Overload 1 (returns [Either<GetFeeError, TransactionFeeExtended>]):
 *  - DEX / DEX_BRIDGE → always GaslessError.NetworkIsNotSupported
 *  - CEX + zero or unparseable amount → UnknownError
 *  - CEX + selectedFeeToken != null → delegates to [estimateFeeForTokenUseCase]
 *  - CEX + selectedFeeToken == null → delegates to [estimateFeeForGaslessTxUseCase]
 *
 * Overload 2 (returns [Either<GetFeeError, TransactionFee>]):
 *  - DEX / DEX_BRIDGE + zero amount → UnknownError
 *  - DEX / DEX_BRIDGE + getExchangeData error → UnknownError
 *  - CEX + zero amount → UnknownError
 *  - CEX + non-zero amount → delegates to [estimateFeeUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplLoadFeeTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()

    // -------------------------------------------------------------------------
    // Overload 1
    // -------------------------------------------------------------------------

    @Nested
    inner class `overload 1 — CEX and token fee paths` {

        @Test
        fun `should return Left GaslessError for DEX provider`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                amount = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = dexProvider,
                selectedFeeToken = null,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(GetFeeError.GaslessError.NetworkIsNotSupported::class.java)
            }
        }

        @Test
        fun `should return Left GaslessError for DEX_BRIDGE provider`() = runTest {
            // Given
            val dexBridgeProvider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                amount = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = dexBridgeProvider,
                selectedFeeToken = null,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(GetFeeError.GaslessError.NetworkIsNotSupported::class.java)
            }
        }

        @Test
        fun `should return Left UnknownError for CEX provider when amount is zero`() = runTest {
            // Given
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                amount = "0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = cexProvider,
                selectedFeeToken = null,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
            }
        }

        @Test
        fun `should return Left UnknownError for CEX provider when amount is invalid string`() = runTest {
            // Given
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                amount = "not-a-decimal",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = cexProvider,
                selectedFeeToken = null,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
            }
        }

        @Test
        fun `should delegate to estimateFeeForTokenUseCase when CEX provider has non-null selectedFeeToken`() =
            runTest {
                // Given
                val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
                val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
                val feeTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true)
                val expectedFeeExtended = mockk<TransactionFeeExtended>(relaxed = true)

                coEvery {
                    estimateFeeForTokenUseCase.invoke(
                        userWallet = any(),
                        feeTokenCurrencyStatus = feeTokenStatus,
                        sendingTokenCurrencyStatus = any(),
                        amount = any(),
                    )
                } returns expectedFeeExtended.right()

                // When
                val result = sut.loadFeeForSwapTransaction(
                    fromSwapCurrencyStatus = fromStatus,
                    amount = "1.5",
                    reduceBalanceBy = BigDecimal.ZERO,
                    provider = cexProvider,
                    selectedFeeToken = feeTokenStatus,
                )

                // Then
                assertThat(result.isRight()).isTrue()
                coVerify(exactly = 1) {
                    estimateFeeForTokenUseCase.invoke(
                        userWallet = any(),
                        feeTokenCurrencyStatus = feeTokenStatus,
                        sendingTokenCurrencyStatus = any(),
                        amount = BigDecimal("1.5"),
                    )
                }
            }

        @Test
        fun `should pass positive non-NaN amount to estimateFeeForGaslessTxUseCase for CEX with tiny nonzero amount and null selectedFeeToken`() =
            runTest {
                // Given — tiny but nonzero amount; null selectedFeeToken routes to estimateFeeForGaslessTxUseCase
                val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
                val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
                val feeExtended = mockk<TransactionFeeExtended>(relaxed = true)
                val capturedAmount = slot<BigDecimal>()

                coEvery {
                    estimateFeeForGaslessTxUseCase.invoke(
                        amount = capture(capturedAmount),
                        userWallet = any(),
                        sendingTokenCurrencyStatus = any(),
                    )
                } returns feeExtended.right()

                // When
                sut.loadFeeForSwapTransaction(
                    fromSwapCurrencyStatus = fromStatus,
                    amount = "0.000001",
                    reduceBalanceBy = BigDecimal.ZERO,
                    provider = cexProvider,
                    selectedFeeToken = null,
                )

                // Then — captured amount is positive, finite, non-NaN
                assertThat(capturedAmount.captured).isNotNull()
                assertThat(capturedAmount.captured.signum()).isGreaterThan(0)
                assertThat(capturedAmount.captured.toDouble().isNaN()).isFalse()
                assertThat(capturedAmount.captured.toDouble().isInfinite()).isFalse()
                // verify estimateFeeForGaslessTxUseCase was called with the exact parsed amount
                coVerify(exactly = 1) {
                    estimateFeeForGaslessTxUseCase.invoke(
                        amount = BigDecimal("0.000001"),
                        userWallet = any(),
                        sendingTokenCurrencyStatus = any(),
                    )
                }
            }

        @Test
        fun `should delegate to estimateFeeForGaslessTxUseCase when CEX provider has null selectedFeeToken`() =
            runTest {
                // Given
                val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
                val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
                val expectedFeeExtended = mockk<TransactionFeeExtended>(relaxed = true)

                coEvery {
                    estimateFeeForGaslessTxUseCase.invoke(
                        amount = any(),
                        userWallet = any(),
                        sendingTokenCurrencyStatus = any(),
                    )
                } returns expectedFeeExtended.right()

                // When
                val result = sut.loadFeeForSwapTransaction(
                    fromSwapCurrencyStatus = fromStatus,
                    amount = "2.0",
                    reduceBalanceBy = BigDecimal.ZERO,
                    provider = cexProvider,
                    selectedFeeToken = null,
                )

                // Then
                assertThat(result.isRight()).isTrue()
                coVerify(exactly = 1) {
                    estimateFeeForGaslessTxUseCase.invoke(
                        amount = BigDecimal("2.0"),
                        userWallet = any(),
                        sendingTokenCurrencyStatus = any(),
                    )
                }
            }
    }

    // -------------------------------------------------------------------------
    // Overload 2
    // -------------------------------------------------------------------------

    @Nested
    inner class `overload 2 — DEX and CEX TransactionFee paths` {

        @Test
        fun `should return Left UnknownError for DEX when amount is zero`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                amount = "0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = dexProvider,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
            }
        }

        @Test
        fun `should return Left UnknownError for DEX when getExchangeData returns error`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            coEvery {
                repository.getExchangeData(
                    userWallet = any(),
                    fromContractAddress = any(),
                    fromNetwork = any(),
                    toContractAddress = any(),
                    fromAddress = any(),
                    toNetwork = any(),
                    fromAmount = any(),
                    fromDecimals = any(),
                    toDecimals = any(),
                    providerId = any(),
                    rateType = any(),
                    toAddress = any(),
                    expressOperationType = any(),
                    refundAddress = any(),
                )
            } returns ExpressDataError.UnknownError.left()

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                amount = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = dexProvider,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
            }
        }

        @Test
        fun `should not call getExchangeData and return UnknownError for DEX when amount is zero`() = runTest {
            // Given — zero amount must short-circuit before hitting repository
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                amount = "0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = dexProvider,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error -> assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java) }
            coVerify(exactly = 0) {
                repository.getExchangeData(
                    userWallet = any(),
                    fromContractAddress = any(),
                    fromNetwork = any(),
                    toContractAddress = any(),
                    fromAddress = any(),
                    toNetwork = any(),
                    fromAmount = any(),
                    fromDecimals = any(),
                    toDecimals = any(),
                    providerId = any(),
                    rateType = any(),
                    toAddress = any(),
                    expressOperationType = any(),
                    refundAddress = any(),
                )
            }
        }

        @Test
        fun `should return Left UnknownError for DEX_BRIDGE when amount is zero`() = runTest {
            // Given
            val dexBridgeProvider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                amount = "0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = dexBridgeProvider,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
        }

        @Test
        fun `should return Left UnknownError for CEX when amount is zero`() = runTest {
            // Given
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                amount = "0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = cexProvider,
            )

            // Then
            assertThat(result.isLeft()).isTrue()
        }

        @Test
        fun `should delegate to estimateFeeUseCase for CEX provider with non-zero amount`() = runTest {
            // Given — return Left to avoid the patchTransactionFeeForSwap branch which requires concrete Fee types
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            coEvery {
                estimateFeeUseCase.invoke(
                    amount = any(),
                    userWallet = any(),
                    cryptoCurrencyStatus = any(),
                )
            } returns GetFeeError.UnknownError.left()

            // When
            sut.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                amount = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
                provider = cexProvider,
            )

            // Then
            coVerify(exactly = 1) {
                estimateFeeUseCase.invoke(
                    amount = BigDecimal("1.0"),
                    userWallet = any(),
                    cryptoCurrencyStatus = any(),
                )
            }
        }
    }
}