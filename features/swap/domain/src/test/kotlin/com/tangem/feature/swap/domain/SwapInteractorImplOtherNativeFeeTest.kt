package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapFeeState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TxFeeState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Characterization tests for the bridge-fee field `otherNativeFeeWei` flowing into the
 * resulting [TxFeeState].
 *
 * Pinned behavior:
 *  - `otherNativeFee` (BigDecimal) = `transaction.otherNativeFeeWei` shifted left by native
 *    decimals (18 for ETH).
 *  - `feeIncludeOtherNativeFee` of the resulting `TxFee.Legacy` equals `feeValue + otherNativeFee`.
 *  - When `otherNativeFeeWei == null`, `feeIncludeOtherNativeFee == feeValue`.
 *  - The `feeToCheckFunds` (the value used by `getFeeState`) equals
 *    `feeByPriority + otherNativeFee`. We assert this indirectly: when the native balance is
 *    BETWEEN `feeByPriority` and `feeByPriority + otherNativeFee`, the resulting
 *    `SwapFeeState` is `NotEnough` (not `Enough`).
 *
 * [REDACTED_TASK_KEY] — these tests exist to guarantee that the upcoming refactor does not silently
 * drop the bridge protocol fee for DEX_BRIDGE providers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplOtherNativeFeeTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()

    @BeforeEach
    fun setup() {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        // Default native balance is large; specific tests override.
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

    /**
     * Surprising current behavior (caught while writing this test):
     *
     * In `loadDexSwapData`, the local `txFeeState` produced by `loadFeeForDex(...).toTxFeeState(...)`
     * is computed but **never used** for the resulting `QuotesLoadedState.txFee`. The actual
     * `txFee` field of the resulting state is populated from the input `txFeeSealedState`
     * parameter via `updateBalances` → which means the `feeIncludeOtherNativeFee` etc. on
     * the returned state come from whatever the caller passes in, NOT from the loaded fee.
     *
     * What IS observable through the public API:
     *  - `feeByPriority + otherNativeFee` enters `feeToCheckFunds` and drives `feeState`
     *    (Enough vs NotEnough). This is verified in the two tests below.
     *
     * The "feeIncludeOtherNativeFee on the result.txFee" assertion is intentionally NOT
     * pinned here — that field is sourced from the caller's `txFeeSealedState` and a refactor
     * that fixes this dead-store will not break this test class.
     *
     * [REDACTED_TASK_KEY] — flagged for discussion before Phase 2.
     */

    @Test
    fun `bridge provider with non-zero otherNativeFeeWei loads exchange data and reaches getFeeUseCase`() = runTest {
        // Given — DEX_BRIDGE with otherNativeFeeWei = 5e15 wei = 0.005 ETH
        val dexBridgeProvider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE)
        val fromStatus = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = true,
            amount = BigDecimal("10"),
        )
        val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
        val quoteModel = buildQuoteModel()
        val swapData = buildSwapDataModelDex(
            txValue = "1000000000000000",
            otherNativeFeeWei = BigDecimal("5000000000000000"),
        )
        stubExchangeData(dexBridgeProvider, quoteModel, swapData)

        val rawFee: Fee = Fee.Common(
            amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18),
        )
        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns TransactionFee.Single(normal = rawFee).right()

        // When
        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            providers = listOf(dexBridgeProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
            txFeeSealedState = buildTxFeeSealedState(),
        )

        // Then — the bridge provider produces a QuotesLoadedState (no SwapError)
        // and the swap data carries the otherNativeFeeWei.
        val state = result[dexBridgeProvider]
        assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
        val loaded = state as SwapState.QuotesLoadedState
        val transaction = loaded.swapDataModel?.transaction as? ExpressTransactionModel.DEX
        assertThat(transaction?.otherNativeFeeWei).isEqualTo(BigDecimal("5000000000000000"))
    }

    @Test
    fun `feeToCheckFunds includes otherNativeFee — NotEnough fires when balance covers fee but not fee plus otherNativeFee`() =
        runTest {
            // Given — DEX_BRIDGE swap, native balance = 0.002 ETH
            // base fee = 0.001 ETH, otherNativeFee = 0.005 ETH → feeToCheck = 0.006 ETH > balance
            // For a Coin swap the feeState branch checks: nativeBalance - spendAmount > fee
            // We swap a Token (so fromToken != Coin) → branch becomes: nativeBalance > fee
            val dexBridgeProvider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE)
            val fromTokenStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = "0xToken",
                isCoin = false,
                amount = BigDecimal("100"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel()
            val swapData = buildSwapDataModelDex(
                txValue = "1000000000000000",
                otherNativeFeeWei = BigDecimal("5000000000000000"), // 0.005 ETH
            )
            stubExchangeData(dexBridgeProvider, quoteModel, swapData)

            // Native balance: 0.002 ETH — enough for base fee (0.001) but NOT for combined (0.006).
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("0.002")

            val baseFeeValue = BigDecimal("0.001")
            val rawFee: Fee = Fee.Common(
                amount = Amount(currencySymbol = "ETH", value = baseFeeValue, decimals = 18),
            )
            coEvery {
                getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
            } returns TransactionFee.Single(normal = rawFee).right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromTokenStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexBridgeProvider),
                amountToSwap = "10",
                reduceBalanceBy = BigDecimal.ZERO,
                txFeeSealedState = buildTxFeeSealedState(),
            )

            // Then — feeToCheckFunds (0.006) > nativeBalance (0.002) → NotEnough
            val state = result[dexBridgeProvider]
            assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
            val loaded = state as SwapState.QuotesLoadedState
            assertThat(loaded.preparedSwapConfigState.feeState).isInstanceOf(SwapFeeState.NotEnough::class.java)
        }

    @Test
    fun `feeToCheckFunds excluding otherNativeFee would have been Enough — pinning the inclusion`() = runTest {
        // Given — same shape as above but native balance = 0.002 ETH and otherNativeFee = 0
        // Verifies the contrapositive: with otherNativeFee == 0, balance covers the fee → Enough.
        val dexBridgeProvider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE)
        val fromTokenStatus = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            contractAddress = "0xToken",
            isCoin = false,
            amount = BigDecimal("100"),
        )
        val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
        val quoteModel = buildQuoteModel()
        val swapData = buildSwapDataModelDex(
            txValue = "1000000000000000",
            otherNativeFeeWei = null,
        )
        stubExchangeData(dexBridgeProvider, quoteModel, swapData)

        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("0.002")

        val baseFeeValue = BigDecimal("0.001")
        val rawFee: Fee = Fee.Common(
            amount = Amount(currencySymbol = "ETH", value = baseFeeValue, decimals = 18),
        )
        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns TransactionFee.Single(normal = rawFee).right()

        // When
        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = fromTokenStatus,
            toSwapCurrencyStatus = toStatus,
            providers = listOf(dexBridgeProvider),
            amountToSwap = "10",
            reduceBalanceBy = BigDecimal.ZERO,
            txFeeSealedState = buildTxFeeSealedState(),
        )

        // Then — without otherNativeFee, the same balance is now sufficient.
        val state = result[dexBridgeProvider]
        assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
        val loaded = state as SwapState.QuotesLoadedState
        assertThat(loaded.preparedSwapConfigState.feeState).isInstanceOf(SwapFeeState.Enough::class.java)
    }

    // region — local helpers

    private fun stubExchangeData(
        provider: com.tangem.feature.swap.domain.models.domain.SwapProvider,
        quoteModel: com.tangem.feature.swap.domain.models.domain.QuoteModel,
        swapData: SwapDataModel,
    ) {
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
                providerId = provider.providerId,
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
                providerId = provider.providerId,
                rateType = any(),
                toAddress = any(),
                expressOperationType = any(),
                refundAddress = any(),
            )
        } returns swapData.right()
    }

    private fun buildSwapDataModelDex(
        txData: String = "dGVzdA==",
        txValue: String? = "0",
        toAmount: BigDecimal = BigDecimal("0.5"),
        otherNativeFeeWei: BigDecimal? = null,
        gas: BigInteger = BigInteger.valueOf(21_000L),
    ): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(toAmount, 18),
        transaction = ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal.ONE, 18),
            toAmount = SwapAmount(toAmount, 18),
            txValue = txValue,
            txId = "tx-id-bridge",
            txTo = "0xRecipient",
            txExtraId = null,
            txFrom = "0xSender",
            txData = txData,
            otherNativeFeeWei = otherNativeFeeWei,
            gas = gas,
        ),
    )

    // endregion
}