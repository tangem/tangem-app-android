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
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.fee.CexFeeResult
import com.tangem.feature.swap.domain.fee.DexFeeResult
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.FeeBucket
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
            dexSwapFeeCalculator.calculate(any(), any(), any())
        } returns DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(rawFee),
            otherNativeFee = BigDecimal.ZERO,
            gas = BigInteger.valueOf(21_000L),
        ).right()

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = swapData,
            selectedFeeToken = null,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { swapFee ->
            assertThat(swapFee.otherNativeFee).isEqualTo(BigDecimal.ZERO)
            assertThat(swapFee.transactionFeeResult).isInstanceOf(TransactionFeeResult.Loaded::class.java)
            assertThat(swapFee.feeBucket).isEqualTo(FeeBucket.MARKET)
            assertThat(swapFee.selectedFeeToken).isSameInstanceAs(nativeFeeTokenStatus)
        }
        coVerify(exactly = 1) {
            dexSwapFeeCalculator.calculate(fromStatus, transaction, null)
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
            dexSwapFeeCalculator.calculate(any(), any(), any())
        } returns DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(solanaFee),
            otherNativeFee = BigDecimal.ZERO,
            gas = null,
        ).right()

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 9),
            swapData = swapData,
            selectedFeeToken = null,
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
            dexSwapFeeCalculator.calculate(any(), any(), any())
        } returns DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(
                TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true)),
            ),
            otherNativeFee = BigDecimal("0.5"),
            gas = BigInteger.valueOf(21_000L),
        ).right()

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = swapData,
            selectedFeeToken = null,
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
            provider = buildSwapProvider(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculate(any(), any(), any()) }
    }

    @Test
    fun `DEX_BRIDGE with swapData == null returns Left UnknownError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
    }

    @Test
    fun `DEX calculator Left ExpressDataError maps to Left GetFeeError UnknownError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val swapData = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = buildDexTransaction(),
        )
        coEvery {
            dexSwapFeeCalculator.calculate(any(), any(), any())
        } returns ExpressDataError.UnknownError.left()

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = swapData,
            selectedFeeToken = null,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
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
            io.mockk.every { transactionFee } returns TransactionFee.Single(
                normal = mockk<Fee.Common>(relaxed = true),
            )
        }
        coEvery {
            cexSwapFeeCalculator.calculate(any(), any(), any(), any())
        } returns CexFeeResult(
            transactionFee = TransactionFeeResult.LoadedExtended(extendedFee),
        ).right()

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
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
                cexSwapFeeCalculator.calculate(any(), any(), any(), any())
            } returns CexFeeResult(
                transactionFee = TransactionFeeResult.LoadedExtended(extendedFee),
            ).right()

            val result = sut.loadSwapFee(
                provider = buildSwapProvider(ExchangeProviderType.CEX),
                fromStatus = fromStatus,
                toStatus = toStatus,
                amount = SwapAmount(BigDecimal.ONE, 18),
                swapData = null,
                selectedFeeToken = null,
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
            io.mockk.every { currency } returns mockk<CryptoCurrency.Token>(relaxed = true)
        }
        val extendedFee = mockk<TransactionFeeExtended>(relaxed = true)
        coEvery {
            cexSwapFeeCalculator.calculate(any(), any(), any(), any())
        } returns CexFeeResult(
            transactionFee = TransactionFeeResult.LoadedExtended(extendedFee),
        ).right()

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = explicitTokenStatus,
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
                selectedFeeToken = explicitTokenStatus,
            )
        }
    }

    @Test
    fun `CEX explicit native selectedFeeToken returns SwapFee with Loaded fee result`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val explicitNativeStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
            io.mockk.every { currency } returns mockk<CryptoCurrency.Coin>(relaxed = true)
        }
        val rawFee = TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true))
        coEvery {
            cexSwapFeeCalculator.calculate(any(), any(), any(), any())
        } returns CexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(rawFee),
        ).right()

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = explicitNativeStatus,
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
            cexSwapFeeCalculator.calculate(any(), any(), any(), any())
        } returns GetFeeError.UnknownError.left()

        val result = sut.loadSwapFee(
            provider = buildSwapProvider(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = null,
            selectedFeeToken = null,
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
            provider = buildSwapProvider(ExchangeProviderType.CEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ZERO, 18),
            swapData = null,
            selectedFeeToken = null,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
        coVerify(exactly = 0) { cexSwapFeeCalculator.calculate(any(), any(), any(), any()) }
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
            provider = buildSwapProvider(ExchangeProviderType.DEX),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ZERO, 18),
            swapData = swapData,
            selectedFeeToken = null,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.UnknownError::class.java)
        }
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculate(any(), any(), any()) }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildDexTransaction(
        otherNativeFeeWei: BigDecimal? = null,
    ): ExpressTransactionModel.DEX = ExpressTransactionModel.DEX(
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
    )
}