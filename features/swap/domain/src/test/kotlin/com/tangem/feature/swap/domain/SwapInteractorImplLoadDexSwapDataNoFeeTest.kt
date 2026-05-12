package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapFeeState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TxFeeState
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for `loadDexSwapDataNoFee` — the replacement for the legacy `loadDexSwapData`.
 *
 * Verifies:
 *  - `dexSwapFeeCalculator.calculate` is NEVER called during quote loading (fee is owned by
 *    the fee selector now).
 *  - The returned `preparedSwapConfigState.balanceStatus` is [SwapBalanceStatus.Pending].
 *  - `swapDataModel` is populated from the Express response so `applySwapFee` (and
 *    `FeeSelectorRepository.loadFeeExtended`) can consume it later.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplLoadDexSwapDataNoFeeTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

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
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(any()) } returns emptySet()
        coEvery { multiQuoteStatusFetcher.invoke(any()) } returns Unit.right()
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any()) } returns null.right()
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns null
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
        coEvery {
            getAllowanceInfoUseCase.invoke(any(), any(), any(), any())
        } returns (AllowanceInfo.Enough(allowance = BigDecimal("100")) as AllowanceInfo).right()
    }

    @Test
    fun `DEX findBestQuote returns QuotesLoadedState without invoking DexSwapFeeCalculator`() = runTest {
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true, amount = BigDecimal("10"))
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quoteModel = buildQuoteModel(allowanceContract = null)
        val swapDataModel = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = ExpressTransactionModel.DEX(
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                toAmount = SwapAmount(BigDecimal("0.5"), 18),
                txValue = "1000000000000000000",
                txId = "tx-id",
                txTo = "0xToAddress",
                txExtraId = null,
                txFrom = "0xFromAddress",
                txData = "0xdata",
                otherNativeFeeWei = null,
                gas = BigInteger.valueOf(21_000L),
            ),
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
        } returns swapDataModel.right()

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(dexProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertThat(result).hasSize(1)
        val state = result[dexProvider]
        assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
        val quotesState = state as SwapState.QuotesLoadedState
        // Fee not computed yet — balanceStatus is Pending until applySwapFee patches the state.
        assertThat(quotesState.preparedSwapConfigState.balanceStatus)
            .isInstanceOf(SwapBalanceStatus.Pending::class.java)
        // swapDataModel is propagated so the fee selector can later call loadSwapFee with it.
        assertThat(quotesState.swapDataModel).isEqualTo(swapDataModel)
        // Fee calculator must not be invoked during quote loading.
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculate(any(), any(), any()) }
    }
}