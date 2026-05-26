package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.ExpressTxType
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Verifies the bridge re-route in `manageDex` / `manageDexSolana`: when the quote response
 * carries `txType == SEND`, the flow must switch to the CEX path. Cases without SEND are
 * exercised as regression guards.
 *
 * Routing is asserted by side-effects: `repository.getExchangeData` and `getAllowanceInfoUseCase`
 * run only on the DEX path, never on the CEX one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplBridgeReRouteTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val solanaNetwork = Blockchain.Solana.toNetworkId()

    @BeforeEach
    fun setup() {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(any()) } returns emptySet()
        coEvery { multiQuoteStatusFetcher.invoke(any()) } returns Unit.right()
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any()) } returns null.right()
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
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
        } returns com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck(
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
        // Default: allowance is Enough — pushes manageDex into the loadDexSwapDataNoFee path so
        // we can validate routing by which side-effects ran (allowance + exchangeData for DEX,
        // neither for CEX).
        coEvery {
            getAllowanceInfoUseCase.invoke(any(), any(), any(), any())
        } returns (AllowanceInfo.Enough(allowance = BigDecimal("100")) as AllowanceInfo).right()
        coEvery {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = any(), rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = any(),
            )
        } returns happyDexSwapData().right()
    }

    private fun happyDexSwapData(): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
        transaction = ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal("1.0"), 18),
            toAmount = SwapAmount(BigDecimal("0.5"), 18),
            txValue = "1000000000000000000",
            txId = "tx-id",
            txTo = "0xTo",
            txExtraId = null,
            txFrom = "0xFrom",
            txData = "0xdata",
            otherNativeFeeWei = null,
            gas = java.math.BigInteger.valueOf(21_000L),
            allowanceContract = null,
        ),
    )

    // -------------------------------------------------------------------------
    // DEX provider on EVM
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN DEX provider with quote txType SEND on EVM WHEN findBestQuote THEN routes to manageCex path`() = runTest {
        val provider = buildSwapProvider(ExchangeProviderType.DEX, providerId = "dex-with-send")
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quote = buildQuoteModel(allowanceContract = null, txType = ExpressTxType.SEND)
        stubFindBestQuote(provider, quote)

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(provider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertManageCexPathTaken(result, provider)
    }

    @Test
    fun `GIVEN DEX provider with quote txType SWAP on EVM WHEN findBestQuote THEN routes to manageDex path`() = runTest {
        val provider = buildSwapProvider(ExchangeProviderType.DEX, providerId = "real-dex")
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quote = buildQuoteModel(allowanceContract = "0xSpender", txType = ExpressTxType.SWAP)
        stubFindBestQuote(provider, quote)

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(provider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertManageDexPathTaken(result, provider)
    }

    @Test
    fun `GIVEN DEX provider with quote txType null on EVM WHEN findBestQuote THEN routes to manageDex path`() = runTest {
        // Legacy backend that hasn't started returning txType on quote yet.
        val provider = buildSwapProvider(ExchangeProviderType.DEX, providerId = "legacy-dex")
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quote = buildQuoteModel(allowanceContract = "0xSpender", txType = null)
        stubFindBestQuote(provider, quote)

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(provider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertManageDexPathTaken(result, provider)
    }

    // -------------------------------------------------------------------------
    // DEX_BRIDGE provider on EVM
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN DEX_BRIDGE provider with quote txType SEND WHEN findBestQuote THEN routes to manageCex path`() = runTest {
        val provider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE, providerId = "bridge-send")
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quote = buildQuoteModel(allowanceContract = null, txType = ExpressTxType.SEND)
        stubFindBestQuote(provider, quote)

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(provider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertManageCexPathTaken(result, provider)
    }

    @Test
    fun `GIVEN DEX_BRIDGE provider with quote txType SWAP WHEN findBestQuote THEN routes to manageDex path`() = runTest {
        val provider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE, providerId = "li-fi-like")
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quote = buildQuoteModel(allowanceContract = "0xSpender", txType = ExpressTxType.SWAP)
        stubFindBestQuote(provider, quote)

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(provider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertManageDexPathTaken(result, provider)
    }

    // -------------------------------------------------------------------------
    // CEX provider — regression guard
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN CEX provider with quote txType null WHEN findBestQuote THEN keeps manageCex path`() = runTest {
        val provider = buildSwapProvider(ExchangeProviderType.CEX, providerId = "cex-legacy")
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quote = buildQuoteModel(allowanceContract = null, txType = null)
        stubFindBestQuote(provider, quote)

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(provider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertManageCexPathTaken(result, provider)
    }

    @Test
    fun `GIVEN CEX provider with quote txType SEND WHEN findBestQuote THEN keeps manageCex path`() = runTest {
        // Defensive: even if backend starts sending txType=SEND for CEX, behavior stays CEX-only.
        val provider = buildSwapProvider(ExchangeProviderType.CEX, providerId = "cex-with-txtype")
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quote = buildQuoteModel(allowanceContract = null, txType = ExpressTxType.SEND)
        stubFindBestQuote(provider, quote)

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(provider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertManageCexPathTaken(result, provider)
    }

    // -------------------------------------------------------------------------
    // DEX provider on Solana
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN DEX provider with quote txType SEND on Solana WHEN findBestQuote THEN routes to manageCex path`() =
        runTest {
            val provider = buildSwapProvider(ExchangeProviderType.DEX, providerId = "dex-with-send-solana")
            val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork)
            val to = buildSwapCurrencyStatus(networkRawId = solanaNetwork)
            val quote = buildQuoteModel(allowanceContract = null, txType = ExpressTxType.SEND)
            stubFindBestQuote(provider, quote)

            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = from,
                toSwapCurrencyStatus = to,
                providers = listOf(provider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
            )

            assertManageCexPathTaken(result, provider)
        }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun stubFindBestQuote(provider: SwapProvider, quote: QuoteModel) {
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
        } returns quote.right()
    }

    /**
     * Asserts that the result is a CEX-style quote state produced by `manageCex`:
     *  - repository.getExchangeData NOT called at the quote stage (it runs inside
     *    loadDexSwapDataNoFee, only on the DEX path).
     *  - getAllowanceInfoUseCase NOT called (DEX-only artifact).
     */
    private fun assertManageCexPathTaken(
        result: Map<SwapProvider, SwapState>,
        provider: SwapProvider,
    ) {
        assertThat(result).hasSize(1)
        assertThat(result[provider]).isInstanceOf(SwapState.QuotesLoadedState::class.java)

        coVerify(exactly = 0) { getAllowanceInfoUseCase.invoke(any(), any(), any(), any()) }
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

    /**
     * Asserts that the result took the DEX path through `manageDex` / `manageDexSolana`. Both
     * paths drive `loadDexSwapDataNoFee` -> `repository.getExchangeData` when the quote returns
     * Right and balance is sufficient (the default setup ensures this). The presence of that
     * call is therefore a reliable signal that the bridge re-route did NOT fire.
     */
    private fun assertManageDexPathTaken(
        result: Map<SwapProvider, SwapState>,
        provider: SwapProvider,
    ) {
        assertThat(result).hasSize(1)
        assertThat(result[provider]).isInstanceOf(SwapState.QuotesLoadedState::class.java)
        coVerify(atLeast = 1) {
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
        }
    }
}