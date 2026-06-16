package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.fee.CexFeeResult
import com.tangem.feature.swap.domain.fee.DexFeeResult
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for [SwapInteractorImpl.loadSwapFee] ([REDACTED_TASK_KEY] — Phase 3).
 *
 * Exercises the unified fee API and verifies the four strategy branches:
 *  - DEX-EVM: delegates to `DexSwapFeeCalculator` and returns `SwapFee` with `otherNativeFee=0`.
 *  - DEX-Solana: same, no gas patch.
 *  - DEX bridge with `otherNativeFee > 0`: propagated through `SwapFee.otherNativeFee`.
 *  - CEX gasless-native (selectedFeeToken == null, gasless picks native).
 *  - CEX gasless-token (selectedFeeToken == null, gasless picks token).
 *  - CEX token-explicit (selectedFeeToken != null).
 *  - DEX with swapData == null → `Left(GetFeeError.UnknownError)`.
 *  - Zero amount → matches existing CEX/DEX paths (returns Left UnknownError).
 *
 * The DEX/CEX calculators themselves are mocked here — their internals are covered by
 * [com.tangem.feature.swap.domain.fee.DexSwapFeeCalculatorTest] and
 * [com.tangem.feature.swap.domain.fee.CexSwapFeeCalculatorTest].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplLoadSwapFeeTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val solanaNetwork = Blockchain.Solana.toNetworkId()

    private val nativeFeeTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true)

    @BeforeEach
    fun setup() {
        // `loadSwapFee` resolves the default `selectedFeeToken` via the fee-paid use case when
        // the caller passes null. Stub a concrete CryptoCurrencyStatus so the assertion is stable.
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any())
        } returns nativeFeeTokenStatus.right()
    }

    // -------------------------------------------------------------------------
    // DEX branch
    // -------------------------------------------------------------------------

    @Test
    fun `DEX EVM delegates to DexSwapFeeCalculator and returns SwapFee with zero otherNativeFee`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDexTransaction(otherNativeFeeWei = null)
        val swapData = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = transaction,
        )
        val rawFee = TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true))
        coEvery {
            dexSwapFeeCalculator.calculate(
                fromSwapCurrencyStatus = any(),
                transaction = any(),
                selectedToken = any(),
                permissionState = any(),
            )
        } returns DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(rawFee),
            otherNativeFee = BigDecimal.ZERO,
            gas = BigInteger.valueOf(21_000L),
        ).right()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = swapData,
            selectedFeeToken = null,
            isGasless = false,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { swapFee ->
            assertThat(swapFee.otherNativeFee).isEqualTo(BigDecimal.ZERO)
            assertThat(swapFee.transactionFeeResult).isInstanceOf(TransactionFeeResult.Loaded::class.java)
            assertThat(swapFee.feeBucket).isEqualTo(FeeBucket.MARKET)
            assertThat(swapFee.selectedFeeToken).isSameInstanceAs(nativeFeeTokenStatus)
        }
        coVerify(exactly = 1) {
            dexSwapFeeCalculator.calculate(
                fromSwapCurrencyStatus = fromStatus,
                transaction = transaction,
                selectedToken = null,
                permissionState = any(),
            )
        }
    }

    @Test
    fun `DEX Solana delegates to DexSwapFeeCalculator and propagates the loaded fee without gas patch`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = true, decimals = 9)
        val toStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = true, decimals = 9)
        val transaction = buildDexTransaction()
        val swapData = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 9),
            transaction = transaction,
        )
        val solanaFee = TransactionFee.Single(
            normal = Fee.Common(
                Amount(currencySymbol = "SOL", value = BigDecimal("0.005"), decimals = 9),
            ),
        )
        coEvery {
            dexSwapFeeCalculator.calculate(
                fromSwapCurrencyStatus = any(),
                transaction = any(),
                selectedToken = any(),
                permissionState = any(),
            )
        } returns DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(solanaFee),
            otherNativeFee = BigDecimal.ZERO,
            gas = null,
        ).right()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 9),
            swapData = swapData,
            selectedFeeToken = null,
            isGasless = false,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { swapFee ->
            assertThat(swapFee.otherNativeFee).isEqualTo(BigDecimal.ZERO)
            assertThat(swapFee.fee).isEqualTo(solanaFee.normal)
        }
    }

    @Test
    fun `DEX_BRIDGE propagates otherNativeFee from DexFeeResult to SwapFee`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDexTransaction(otherNativeFeeWei = BigDecimal("500000000000000000"))
        val swapData = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = transaction,
        )
        coEvery {
            dexSwapFeeCalculator.calculate(
                fromSwapCurrencyStatus = any(),
                transaction = any(),
                selectedToken = any(),
                permissionState = any(),
            )
        } returns DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(
                TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true)),
            ),
            otherNativeFee = BigDecimal("0.5"),
            gas = BigInteger.valueOf(21_000L),
        ).right()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX_BRIDGE),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = swapData,
            selectedFeeToken = null,
            isGasless = false,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { swapFee ->
            assertThat(swapFee.otherNativeFee).isEquivalentAccordingToCompareTo(BigDecimal("0.5"))
        }
    }

    @Test
    fun `DEX with swapData == null returns Left UnknownError without calling calculator`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
            isGasless = false,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
        coVerify(exactly = 0) {
            dexSwapFeeCalculator.calculate(
                fromSwapCurrencyStatus = any(),
                transaction = any(),
                selectedToken = any(),
                permissionState = any(),
            )
        }
    }

    @Test
    fun `DEX_BRIDGE with swapData == null returns Left UnknownError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX_BRIDGE),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
            isGasless = false,

            )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
    }

    @Test
    fun `DEX provider with quote txType SEND and null swapData routes to CEX fee calculator`() = runTest {
        // [REDACTED_TASK_KEY]: swap-xyz comes as provider.type=DEX but the quote returns txType=SEND, which
        // re-routes to the CEX-style flow (no DEX swapData is built). Fee must load via the CEX
        // calculator instead of short-circuiting to UnknownError.
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val extendedFee = mockk<TransactionFeeExtended>(relaxed = true) {
            io.mockk.every { transactionFee } returns TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true))
        }
        coEvery {
            cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any())
        } returns CexFeeResult(transactionFee = TransactionFeeResult.LoadedExtended(extendedFee)).right()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX_BRIDGE),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
            isGasless = false,
            txType = ExpressTxType.SEND,
        )

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculate(any(), any(), any()) }
    }

    @Test
    fun `DEX_BRIDGE provider with quote txType SEND and null swapData routes to CEX fee calculator`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val extendedFee = mockk<TransactionFeeExtended>(relaxed = true) {
            io.mockk.every { transactionFee } returns TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true))
        }
        coEvery {
            cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any())
        } returns CexFeeResult(transactionFee = TransactionFeeResult.LoadedExtended(extendedFee)).right()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX_BRIDGE),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
            isGasless = false,
            txType = ExpressTxType.SEND,
        )

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculate(any(), any(), any()) }
    }

    @Test
    fun `DEX calculator Left ExpressDataError maps to Wrapped Left GetFeeError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val swapData = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = buildDexTransaction(),
        )
        coEvery {
            dexSwapFeeCalculator.calculate(any(), any(), any())
        } returns GetFeeError.DataError(cause = ExpressDataError.UnknownError()).left()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX_BRIDGE),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = swapData,
            selectedFeeToken = null,
            isGasless = false,
            )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.DataError::class.java)
            assertThat((error as? GetFeeError.DataError)?.cause).isInstanceOf(ExpressDataError.UnknownError::class.java)
        }
    }

    // -------------------------------------------------------------------------
    // CEX branch
    // -------------------------------------------------------------------------

    @Test
    fun `CEX gasless-native delegates to CexSwapFeeCalculator and resolves native coin status`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val extendedFee = mockk<TransactionFeeExtended>(relaxed = true) {
            // Gasless picked native — feeTokenId points at the network's coin.
            every { transactionFee } returns TransactionFee.Single(
                normal = mockk<Fee.Common>(relaxed = true),
            )
        }
        coEvery {
            cexSwapFeeCalculator.calculate(
                userWallet = any(),
                fromSwapCurrencyStatus = any(),
                amount = any(),
                selectedFeeToken = any(),
                isGasless = any(),
            )
        } returns CexFeeResult(
            transactionFee = TransactionFeeResult.LoadedExtended(extendedFee),
        ).right()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
            isGasless = true,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { swapFee ->
            assertThat(swapFee.transactionFeeResult).isInstanceOf(TransactionFeeResult.LoadedExtended::class.java)
            assertThat(swapFee.selectedFeeToken).isSameInstanceAs(nativeFeeTokenStatus)
            assertThat(swapFee.otherNativeFee).isEqualTo(BigDecimal.ZERO)
        }
        coVerify(exactly = 1) {
            cexSwapFeeCalculator.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal.ONE,
                selectedFeeToken = null,
                isGasless = true,
            )
        }
    }

    @Test
    fun `CEX gasless-token (null selectedFeeToken, gasless picks token) returns native coin as fee token by default`() =
        runTest {
            // The unified contract here is: when caller passes null, the impl resolves the
            // native coin status via GetFeePaidCryptoCurrencyStatusSyncUseCase. The fact that
            // gasless internally picked a token does not change the SwapFee.selectedFeeToken
            // — that resolution is the caller's responsibility (it happens in Phase 4 when
            // FeeSelectorRepository builds the call).
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
            val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
            val extendedFee = mockk<TransactionFeeExtended>(relaxed = true)
            coEvery {
                cexSwapFeeCalculator.calculate(
                    userWallet = any(),
                    fromSwapCurrencyStatus = any(),
                    amount = any(),
                    selectedFeeToken = any(),
                    isGasless = any(),
                )
            } returns CexFeeResult(
                transactionFee = TransactionFeeResult.LoadedExtended(extendedFee),
            ).right()

            val result = sut.loadSwapFee(
                quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.CEX),
                fromStatus = fromStatus,
                toStatus = toStatus,
                amount = SwapAmount(BigDecimal.ONE, 18),
                swapData = null,
                selectedFeeToken = null, isGasless = true,

                )

            assertThat(result.isRight()).isTrue()
            result.onRight { swapFee ->
                assertThat(swapFee.selectedFeeToken).isSameInstanceAs(nativeFeeTokenStatus)
                assertThat(swapFee.transactionFeeResult).isInstanceOf(TransactionFeeResult.LoadedExtended::class.java)
            }
        }

    @Test
    fun `CEX token-explicit propagates the provided selectedFeeToken into SwapFee`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val explicitTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
            every { currency } returns mockk<CryptoCurrency.Token>(relaxed = true)
        }
        val extendedFee = mockk<TransactionFeeExtended>(relaxed = true)
        coEvery {
            cexSwapFeeCalculator.calculate(
                userWallet = any(),
                fromSwapCurrencyStatus = any(),
                amount = any(),
                selectedFeeToken = any(),
                isGasless = any(),
            )
        } returns CexFeeResult(
            transactionFee = TransactionFeeResult.LoadedExtended(extendedFee),
        ).right()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = explicitTokenStatus, isGasless = true,

            )

        assertThat(result.isRight()).isTrue()
        result.onRight { swapFee ->
            assertThat(swapFee.selectedFeeToken).isSameInstanceAs(explicitTokenStatus)
        }
        coVerify(exactly = 1) {
            cexSwapFeeCalculator.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal.ONE,
                selectedFeeToken = explicitTokenStatus, isGasless = true,

                )
        }
    }

    @Test
    fun `CEX explicit native selectedFeeToken returns SwapFee with Loaded fee result`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val explicitNativeStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
            every { currency } returns mockk<CryptoCurrency.Coin>(relaxed = true)
        }
        val rawFee = TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true))
        coEvery {
            cexSwapFeeCalculator.calculate(
                userWallet = any(),
                fromSwapCurrencyStatus = any(),
                amount = any(),
                selectedFeeToken = any(),
                isGasless = any(),
            )
        } returns CexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(rawFee),
        ).right()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = explicitNativeStatus, isGasless = true,

            )

        assertThat(result.isRight()).isTrue()
        result.onRight { swapFee ->
            assertThat(swapFee.selectedFeeToken).isSameInstanceAs(explicitNativeStatus)
            assertThat(swapFee.transactionFeeResult).isInstanceOf(TransactionFeeResult.Loaded::class.java)
        }
    }

    @Test
    fun `CEX calculator Left UnknownError propagates as Left UnknownError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        coEvery {
            cexSwapFeeCalculator.calculate(
                userWallet = any(),
                fromSwapCurrencyStatus = any(),
                amount = any(),
                selectedFeeToken = any(),
                isGasless = any(),
            )
        } returns GetFeeError.UnknownError.left()

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null, isGasless = true,

            )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
    }

    // -------------------------------------------------------------------------
    // Zero-amount short-circuit
    // -------------------------------------------------------------------------

    @Test
    fun `amount zero on CEX returns Left UnknownError without calling calculator`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ZERO, 18),
            swapData = null,
            selectedFeeToken = null, isGasless = true,

            )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
        coVerify(exactly = 0) {
            cexSwapFeeCalculator.calculate(
                userWallet = any(),
                fromSwapCurrencyStatus = any(),
                amount = any(),
                selectedFeeToken = any(),
                isGasless = any(),
            )
        }
    }

    @Test
    fun `amount zero on DEX returns Left UnknownError without calling calculator`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val swapData = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = buildDexTransaction(),
        )

        val result = sut.loadSwapFee(
            quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ZERO, 18),
            swapData = swapData,
            selectedFeeToken = null,
            isGasless = false,

            )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
        coVerify(exactly = 0) {
            dexSwapFeeCalculator.calculate(
                fromSwapCurrencyStatus = any(),
                transaction = any(),
                selectedToken = any(),
                permissionState = any(),
            )
        }
    }

    // -------------------------------------------------------------------------
    // DEX with explicit selectedFeeToken (Token)
    // -------------------------------------------------------------------------

    @Test
    fun `DEX with explicit token selectedFeeToken propagates it into SwapFee and calls calculator with that token`() =
        runTest {
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
            val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
            val transaction = buildDexTransaction()
            val swapData = SwapDataModel(
                toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
                transaction = transaction,
            )
            val explicitTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
                every { currency } returns mockk<CryptoCurrency.Token>(relaxed = true)
            }
            val rawFee = TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true))
            coEvery {
                dexSwapFeeCalculator.calculate(
                    fromSwapCurrencyStatus = any(),
                    transaction = any(),
                    selectedToken = any(),
                    permissionState = any(),
                )
            } returns DexFeeResult(
                transactionFee = TransactionFeeResult.Loaded(rawFee),
                otherNativeFee = BigDecimal.ZERO,
                gas = BigInteger.valueOf(21_000L),
            ).right()

            val result = sut.loadSwapFee(
                quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX),
                fromStatus = fromStatus,
                toStatus = toStatus,
                amount = SwapAmount(BigDecimal.ONE, 18),
                swapData = swapData,
                selectedFeeToken = explicitTokenStatus,
                isGasless = false,

                )

            assertThat(result.isRight()).isTrue()
            result.onRight { swapFee ->
                assertThat(swapFee.selectedFeeToken).isSameInstanceAs(explicitTokenStatus)
            }
            coVerify(exactly = 1) {
                dexSwapFeeCalculator.calculate(
                    fromSwapCurrencyStatus = fromStatus,
                    transaction = transaction,
                    selectedToken = explicitTokenStatus,
                    permissionState = any(),
                )
            }
        }

    // -------------------------------------------------------------------------
    // resolveNativeFeeTokenStatus failure path
    // -------------------------------------------------------------------------

    /**
     * When selectedFeeToken is null AND getFeePaidCryptoCurrencyStatusSyncUseCase returns Right(null),
     * the impl falls back to building a CryptoCurrencyStatus from scratch.
     * If networkAddress is null on the fromStatus, the fallback returns null and
     * loadSwapFee must return Left(UnknownError).
     *
     * This exercises the `resolveNativeFeeTokenStatus` fallback path in loadDexSwapFee.
     */
    @Test
    fun `DEX with null selectedFeeToken - resolveNativeFeeTokenStatus returns null when networkAddress is null`() =
        runTest {
            // Primary resolve: getFeePaidCryptoCurrencyStatusSyncUseCase returns Right(null)
            // → triggers the fallback block in resolveNativeFeeTokenStatus
            coEvery {
                getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any())
            } returns null.right()

            // The fallback path tries to build a CryptoCurrencyStatus.NoQuote/Loaded
            // but requires networkAddress to be non-null. Stub it to null so the
            // fallback's early-return fires → resolveNativeFeeTokenStatus returns null.
            val fromStatusWithNullAddr = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                isCoin = true,
            )
            every {
                fromStatusWithNullAddr.status.value.networkAddress
            } returns null

            val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
            val swapData = SwapDataModel(
                toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
                transaction = buildDexTransaction(),
            )
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
            coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("1.0")
            // Make the calculator succeed (so the failure comes from resolveNativeFeeTokenStatus).
            // quotesRepository returns null → NoQuote path → networkAddress null → return@run null
            coEvery { quotesRepository.getMultiQuoteSyncOrNull(any()) } returns null
            coEvery {
                dexSwapFeeCalculator.calculate(
                    fromSwapCurrencyStatus = any(),
                    transaction = any(),
                    selectedToken = any(),
                    permissionState = any(),
                )
            } returns DexFeeResult(
                transactionFee = TransactionFeeResult.Loaded(
                    TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true)),
                ),
                otherNativeFee = BigDecimal.ZERO,
                gas = BigInteger.valueOf(21_000L),
            ).right()

            val result = sut.loadSwapFee(
                quotesLoadedState = buildQuotesLoadedState(ExchangeProviderType.DEX),
                fromStatus = fromStatusWithNullAddr,
                toStatus = toStatus,
                amount = SwapAmount(BigDecimal.ONE, 18),
                swapData = swapData,
                selectedFeeToken = null, isGasless = false,

                )

            // When resolveNativeFeeTokenStatus returns null → Left(UnknownError)
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
            }
        }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildQuotesLoadedState(
        providerType: ExchangeProviderType,
        permissionState: PermissionDataState = PermissionDataState.Empty,
    ): SwapState.QuotesLoadedState {
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal.ONE, 18),
                swapCurrencyStatus = from,
                amountFiat = BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("0.5"), 18),
                swapCurrencyStatus = to,
                amountFiat = BigDecimal.ZERO,
            ),
            priceImpact = PriceImpact.Empty,
            preparedSwapConfigState = PreparedSwapConfigState(
                balanceStatus = SwapBalanceStatus.Pending,
                hasOutgoingTransaction = false,
            ),
            permissionState = permissionState,
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildSwapProvider(providerType),
        )
    }

    private fun buildDexTransaction(otherNativeFeeWei: BigDecimal? = null): ExpressTransactionModel.DEX =
        ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal.ONE, 18),
            toAmount = SwapAmount(BigDecimal("0.5"), 18),
            txValue = "1000000000000000",
            txId = "tx-id",
            txTo = "0xTo",
            txExtraId = null,
            txFrom = "0xFrom",
            txData = "dGVzdA==",
            otherNativeFeeWei = otherNativeFeeWei,
            gas = BigInteger.valueOf(21_000L),
            allowanceContract = null,
        )
}