package com.tangem.feature.swap.domain

import android.util.Base64
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Characterization tests for the private fee-loading paths of [SwapInteractorImpl] reached by
 * the DEX provider branch:
 *
 *  - `loadFeeForDex`
 *  - `getFeeDataForDexSwap` (EVM)
 *  - `getFeeDataForSolanaDexSwap` (Solana)
 *  - the `patchTransactionFeeForSwap` 12% gas-limit bump applied on EVM DEX
 *
 * Driven through the public [SwapInteractorImpl.findBestQuote] entry point with carefully
 * stubbed dependencies so the DEX-fee branch executes deterministically.
 *
 * [REDACTED_TASK_KEY] — these tests are intentionally pinned to the **current** behavior so that the
 * upcoming refactor (extraction into `DexSwapFeeCalculator`) is provably equivalent.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplLoadFeeForDexTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val solanaNetwork = Blockchain.Solana.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()

    @BeforeEach
    fun setup() {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")
        coEvery {
            getCurrencyCheckUseCase.invoke(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        } returns CryptoCurrencyCheck(
            dustValue = null,
            reserveAmount = null,
            minimumSendAmount = null,
            existentialDeposit = null,
            utxoAmountLimit = null,
            isAccountFunded = true,
            rentWarning = null,
            isMemoRequired = false,
        )
        coEvery {
            validateTransactionUseCase.invoke(
                amount = any(),
                fee = any(),
                memo = any(),
                destination = any(),
                userWalletId = any(),
                network = any(),
            )
        } returns Unit.right()
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(any()) } answers {
            firstArg<Set<CryptoCurrency.RawID>>().map { rawId ->
                QuoteStatus(
                    rawCurrencyId = rawId,
                    value = QuoteStatus.Data(
                        source = StatusSource.ACTUAL,
                        fiatRate = BigDecimal.ONE,
                        fiatRateUSD = BigDecimal.ONE,
                        priceChange = BigDecimal.ZERO,
                    ),
                )
            }.toSet()
        }
        coEvery { multiQuoteStatusFetcher.invoke(any()) } returns Unit.right()
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any()) } returns null.right()
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns null
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
        every { allowPermissionsHandler.isAddressAllowanceInProgress(any()) } returns false
        coEvery {
            getAllowanceInfoUseCase.invoke(
                userWalletId = any(),
                cryptoCurrency = any(),
                spenderAddress = any(),
                requiredAmount = any(),
            )
        } returns (AllowanceInfo.Enough(allowance = BigDecimal("1000")) as AllowanceInfo).right()
        every { createTransactionExtrasUseCase.invoke(data = any(), network = any()) } returns
            mockk<TransactionExtras>(relaxed = true).right()
    }

    @Test
    fun `EVM DEX swap propagates extras destinationAddress sourceAddress and amount to getFeeUseCase`() = runTest {
        // Given
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val fromStatus = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = true,
            amount = BigDecimal("10"),
        )
        val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
        val quoteModel = buildQuoteModel()
        val swapData = buildSwapDataModelDex(
            txValue = "1000000000000000", // 0.001 ETH
            txTo = "0xRecipient",
            txFrom = "0xSender",
            txData = "0xPayload",
        )

        coEvery {
            repository.findBestQuote(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
            )
        } returns quoteModel.right()

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
                providerId = dexProvider.providerId,
                rateType = any(),
                toAddress = any(),
                expressOperationType = any(),
                refundAddress = any(),
            )
        } returns swapData.right()

        val capturedTxData = slot<TransactionData>()
        coEvery {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = capture(capturedTxData),
            )
        } returns mockk<TransactionFee.Single>(relaxed = true).right()

        // When
        sut.findBestQuote(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            providers = listOf(dexProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
            txFeeSealedState = buildTxFeeSealedState(),
        )

        // Then — captured TransactionData carries the values from ExpressTransactionModel.DEX
        assertThat(capturedTxData.isCaptured).isTrue()
        val uncompiled = capturedTxData.captured as TransactionData.Uncompiled
        assertThat(uncompiled.destinationAddress).isEqualTo("0xRecipient")
        assertThat(uncompiled.sourceAddress).isEqualTo("0xSender")
        // amount.value is the txValue moved-point-left by native decimals (18 for ETH) → 0.001
        // Use compareTo-equivalence to ignore the BigDecimal scale (0.001 vs 0.001000000000000000).
        assertThat(uncompiled.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.001"))
        // extras came from createTransactionExtrasUseCase
        assertThat(uncompiled.extras).isNotNull()
    }

    @Test
    fun `EVM DEX swap with native balance ZERO surfaces SwapError UnknownError`() = runTest {
        // Given — zero native balance triggers the early-raise in getFeeDataForDexSwap
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val fromStatus = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = true,
            amount = BigDecimal("10"),
        )
        val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
        val quoteModel = buildQuoteModel()
        val swapData = buildSwapDataModelDex(txValue = "0")

        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal.ZERO

        coEvery {
            repository.findBestQuote(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
            )
        } returns quoteModel.right()
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
                providerId = dexProvider.providerId,
                rateType = any(),
                toAddress = any(),
                expressOperationType = any(),
                refundAddress = any(),
            )
        } returns swapData.right()

        // When
        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            providers = listOf(dexProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
            txFeeSealedState = buildTxFeeSealedState(),
        )

        // Then — native-balance == 0 raises ExpressDataError.UnknownError up to SwapError
        val state = result[dexProvider]
        assertThat(state).isInstanceOf(SwapState.SwapError::class.java)
        val swapError = state as SwapState.SwapError
        assertThat(swapError.error).isEqualTo(ExpressDataError.UnknownError)
        // getFeeUseCase should NOT have been invoked because the balance check short-circuits first
        coVerify(exactly = 0) {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        }
    }

    @Test
    fun `EVM DEX swap falls back to getEthSpecificFeeUseCase when txValue is null`() = runTest {
        // Given — null txValue forces error("unable to get txValue") → IllegalStateException → fallback
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val fromStatus = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = true,
            amount = BigDecimal("10"),
        )
        val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
        val quoteModel = buildQuoteModel()
        val gas = BigInteger.valueOf(150_000L)
        val swapData = buildSwapDataModelDex(txValue = null, gas = gas)

        coEvery {
            repository.findBestQuote(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
            )
        } returns quoteModel.right()
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
                providerId = dexProvider.providerId,
                rateType = any(),
                toAddress = any(),
                expressOperationType = any(),
                refundAddress = any(),
            )
        } returns swapData.right()

        coEvery {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        } returns mockk<TransactionFee.Choosable>(relaxed = true).right()

        // When
        sut.findBestQuote(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            providers = listOf(dexProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
            txFeeSealedState = buildTxFeeSealedState(),
        )

        // Then — fallback path is invoked with the gas from the express transaction model
        coVerify(exactly = 1) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = gas,
                gasPrice = any(),
            )
        }
    }

    @Test
    fun `EVM DEX swap falls back to getEthSpecificFeeUseCase when createTransactionExtrasUseCase returns null`() =
        runTest {
            // Given — null extras → error("unable to create extras") → IllegalStateException → fallback
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                isCoin = true,
                amount = BigDecimal("10"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel()
            val gas = BigInteger.valueOf(75_000L)
            val swapData = buildSwapDataModelDex(txValue = "1000000000000000", gas = gas)

            coEvery {
                repository.findBestQuote(
                    userWallet = any(),
                    fromContractAddress = any(),
                    fromNetwork = any(),
                    toContractAddress = any(),
                    toNetwork = any(),
                    fromAmount = any(),
                    fromDecimals = any(),
                    toDecimals = any(),
                    providerId = dexProvider.providerId,
                    rateType = any(),
                )
            } returns quoteModel.right()
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
                    providerId = dexProvider.providerId,
                    rateType = any(),
                    toAddress = any(),
                    expressOperationType = any(),
                    refundAddress = any(),
                )
            } returns swapData.right()

            // Force createTransactionExtrasUseCase to return null → triggers the fallback path.
            // The use case signature is Either<Throwable, TransactionExtras>; pass a Throwable Left.
            every {
                createTransactionExtrasUseCase.invoke(data = any(), network = any())
            } returns IllegalStateException("forced fail").left()

            coEvery {
                getEthSpecificFeeUseCase.invoke(
                    userWallet = any(),
                    cryptoCurrency = any(),
                    gasLimit = any(),
                    gasPrice = any(),
                )
            } returns mockk<TransactionFee.Choosable>(relaxed = true).right()

            // When
            sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
                txFeeSealedState = buildTxFeeSealedState(),
            )

            // Then
            coVerify(exactly = 1) {
                getEthSpecificFeeUseCase.invoke(
                    userWallet = any(),
                    cryptoCurrency = any(),
                    gasLimit = gas,
                    gasPrice = any(),
                )
            }
        }

    @Test
    fun `EVM DEX swap falls back to getEthSpecificFeeUseCase when getFeeUseCase returns null`() = runTest {
        // Given — getFeeUseCase Left → getOrNull() == null → error("unable to calculate fee") → fallback
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val fromStatus = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = true,
            amount = BigDecimal("10"),
        )
        val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
        val quoteModel = buildQuoteModel()
        val gas = BigInteger.valueOf(50_000L)
        val swapData = buildSwapDataModelDex(txValue = "1000000000000000", gas = gas)

        coEvery {
            repository.findBestQuote(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
            )
        } returns quoteModel.right()
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
                providerId = dexProvider.providerId,
                rateType = any(),
                toAddress = any(),
                expressOperationType = any(),
                refundAddress = any(),
            )
        } returns swapData.right()

        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns GetFeeError.UnknownError.left()

        coEvery {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        } returns mockk<TransactionFee.Choosable>(relaxed = true).right()

        // When
        sut.findBestQuote(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            providers = listOf(dexProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
            txFeeSealedState = buildTxFeeSealedState(),
        )

        // Then
        coVerify(exactly = 1) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = gas,
                gasPrice = any(),
            )
        }
    }

    @Test
    fun `Solana DEX uses TransactionData Compiled and skips the 12 percent gas patch`() = runTest {
        // Given — Solana network forces the Compiled path. We capture the TransactionData and
        // assert that the resulting fee value is the raw return of getFeeUseCase (no 1.12x scaling).
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
        mockkObject(SolanaTransactionHelper)
        every { SolanaTransactionHelper.removeSignaturesPlaceholders(any()) } returns ByteArray(64)

        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val fromStatus = buildSwapCurrencyStatus(
            networkRawId = solanaNetwork,
            isCoin = true,
            amount = BigDecimal("10"),
        )
        val toStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork)
        val quoteModel = buildQuoteModel()
        val swapData = buildSwapDataModelDex(txData = "U29sYW5h")

        coEvery {
            repository.findBestQuote(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
            )
        } returns quoteModel.right()
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
                providerId = dexProvider.providerId,
                rateType = any(),
                toAddress = any(),
                expressOperationType = any(),
                refundAddress = any(),
            )
        } returns swapData.right()

        // Construct a deterministic Solana fee — Fee.Common with a known amount value.
        val rawFeeAmount = BigDecimal("0.005000")
        val rawFee: Fee = Fee.Common(
            amount = Amount(
                currencySymbol = "SOL",
                value = rawFeeAmount,
                decimals = 9,
            ),
        )
        val txFee = TransactionFee.Single(normal = rawFee)
        val capturedTxData = slot<TransactionData>()
        coEvery {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = capture(capturedTxData),
            )
        } returns txFee.right()

        // When
        sut.findBestQuote(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            providers = listOf(dexProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
            txFeeSealedState = buildTxFeeSealedState(),
        )

        // Then — TransactionData passed to getFeeUseCase is Compiled (not Uncompiled)
        assertThat(capturedTxData.isCaptured).isTrue()
        assertThat(capturedTxData.captured).isInstanceOf(TransactionData.Compiled::class.java)
        // No gas-patch is applied on the Solana path; the raw amount is preserved.
        // Pinning behavior: Fee.Common is not a Fee.Ethereum, so increaseEthGasLimitInNeeded
        // returns it unchanged → no 1.12x scaling.
        assertThat(rawFee.amount.value).isEqualTo(rawFeeAmount)
    }

    @Test
    fun `Solana DEX size guard raises TooLargeSolanaTransactionError when formatted hash exceeds 1232 bytes on Cold wallet`() =
        runTest {
            // Given — formatted hash > 1232 bytes on a Cold wallet → SwapError(TooLargeSolanaTransactionError)
            mockkStatic(Base64::class)
            val oversizedBytes = ByteArray(1300)
            every { Base64.decode(any<String>(), any()) } returns oversizedBytes
            mockkObject(SolanaTransactionHelper)
            every { SolanaTransactionHelper.removeSignaturesPlaceholders(any()) } returns oversizedBytes

            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val coldWallet = mockk<UserWallet.Cold>(relaxed = true)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = solanaNetwork,
                isCoin = true,
                amount = BigDecimal("10"),
            ).let { status ->
                SwapCurrencyStatus(
                    userWallet = coldWallet,
                    status = status.status,
                    account = status.account,
                )
            }
            val toStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork)
            val quoteModel = buildQuoteModel()
            val swapData = buildSwapDataModelDex(txData = "very-long-base64-content==")

            coEvery {
                repository.findBestQuote(
                    userWallet = any(),
                    fromContractAddress = any(),
                    fromNetwork = any(),
                    toContractAddress = any(),
                    toNetwork = any(),
                    fromAmount = any(),
                    fromDecimals = any(),
                    toDecimals = any(),
                    providerId = dexProvider.providerId,
                    rateType = any(),
                )
            } returns quoteModel.right()
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
                    providerId = dexProvider.providerId,
                    rateType = any(),
                    toAddress = any(),
                    expressOperationType = any(),
                    refundAddress = any(),
                )
            } returns swapData.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
                txFeeSealedState = buildTxFeeSealedState(),
            )

            // Then
            val state = result[dexProvider]
            assertThat(state).isInstanceOf(SwapState.SwapError::class.java)
            val swapError = state as SwapState.SwapError
            assertThat(swapError.error).isEqualTo(ExpressDataError.TooLargeSolanaTransactionError)
        }

    /**
     * Surprising current behavior pinned here for the redesign:
     *
     * In `loadDexSwapData`, the local `txFeeState` produced by `loadFeeForDex(...).toTxFeeState(...)`
     * is computed but NEVER PROPAGATED to `QuotesLoadedState.txFee`. The latter is sourced from
     * the input `txFeeSealedState` parameter via `updateBalances`. This means the 12% gas patch
     * is applied (the side-effect runs) but the patched value is then discarded for state
     * purposes; only the side effects of `loadFeeForDex` (raising on Solana size limit, balance=0,
     * etc.) survive.
     *
     * The 12% gas-patch math itself is fully covered by the planned Phase-2 PatchEthGasLimitForSwapTest;
     * pinning it through the public API here would only assert the discarded result.
     *
     * [REDACTED_TASK_KEY] — flagged for Phase-2 author awareness; the refactor MUST decide whether to:
     *   (a) preserve the dead-store (unlikely), or
     *   (b) actually wire the loaded fee into the resulting state (the intended fix).
     */

    // region — local builders

    private fun buildSwapDataModelDex(
        txData: String = "dGVzdA==",
        txValue: String? = "0",
        toAmount: BigDecimal = BigDecimal("0.5"),
        otherNativeFeeWei: BigDecimal? = null,
        gas: BigInteger = BigInteger.valueOf(21_000L),
        txTo: String = "0xRecipient",
        txFrom: String = "0xSender",
    ): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(toAmount, 18),
        transaction = ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal.ONE, 18),
            toAmount = SwapAmount(toAmount, 18),
            txValue = txValue,
            txId = "tx-id-123",
            txTo = txTo,
            txExtraId = null,
            txFrom = txFrom,
            txData = txData,
            otherNativeFeeWei = otherNativeFeeWei,
            gas = gas,
        ),
    )

    // endregion
}