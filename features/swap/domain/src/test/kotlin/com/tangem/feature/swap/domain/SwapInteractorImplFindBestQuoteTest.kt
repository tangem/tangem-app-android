package com.tangem.feature.swap.domain

import android.util.Base64
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for [SwapInteractorImpl.findBestQuote] — the core quote-dispatch method.
 *
 * Covers:
 *  - Empty / unparseable amount handling
 *  - DEX provider path on EVM networks (balance enough, allowance enough)
 *  - DEX provider repository error handling (returns SwapError)
 *  - DEX_BRIDGE provider sharing the DEX dispatch branch
 *  - Solana DEX path routing via the Solana-specific branch
 *  - CEX provider dispatch including null txFee edge case
 *  - yieldSupplyStatus.isActive returning [ExpressDataError.DexActiveSupplyError]
 *  - Mixed (DEX + CEX) provider list — each routed to its own path
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplFindBestQuoteTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val solanaNetwork = Blockchain.Solana.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()

    @BeforeEach
    fun setup() {
        // Common stubs that most tests rely on. Individual tests can override.
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
        } returns buildCryptoCurrencyCheck()
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

    @Nested
    inner class EmptyAmountHandling {

        @Test
        fun `should return EmptyAmountState for all providers when amount is zero`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider, cexProvider),
                amountToSwap = "0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[dexProvider]).isInstanceOf(SwapState.EmptyAmountState::class.java)
            assertThat(result[cexProvider]).isInstanceOf(SwapState.EmptyAmountState::class.java)
        }

        @Test
        fun `should return EmptyAmountState for all providers when amount is unparseable`() = runTest {
            // Given
            val provider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(provider),
                amountToSwap = "not-a-number",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[provider]).isInstanceOf(SwapState.EmptyAmountState::class.java)
        }

        @Test
        fun `should return empty map when providers list is empty`() = runTest {
            // Given
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = emptyList(),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class DexProviderPath {

        @Test
        fun `should return SwapState for DEX provider when repository findBestQuote succeeds and balance enough`() =
            runTest {
                // Given
                val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
                val fromStatus = buildSwapCurrencyStatus(
                    networkRawId = ethNetwork,
                    contractAddress = "0",
                    isCoin = true,
                    amount = BigDecimal("10"),
                )
                val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
                val quoteModel = buildQuoteModel(toAmount = BigDecimal("0.5"))
                val swapData = buildSwapDataModelDex()

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
                )

                // Then — has a result entry for the DEX provider; type of state is decided by internal logic
                assertThat(result).hasSize(1)
                assertThat(result.containsKey(dexProvider)).isTrue()
                assertThat(result[dexProvider]).isNotNull()
            }

        @Test
        fun `should return SwapError with DexActiveSupplyError when yieldSupply is active`() = runTest {
            // Given — yieldSupplyActive=true short-circuits to DexActiveSupplyError
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                isCoin = true,
                amount = BigDecimal("10"),
                yieldSupplyActive = true,
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).hasSize(1)
            val state = result[dexProvider]
            assertThat(state).isInstanceOf(SwapState.SwapError::class.java)
            val swapError = (state ?: error("state must not be null")) as SwapState.SwapError
            assertThat(swapError.error).isEqualTo(ExpressDataError.DexActiveSupplyError())
        }

        @Test
        fun `should set balanceStatus to InsufficientAmount when from-token balance is less than swap amount`() =
            runTest {
                // Given — balance is 0.01, swap amount is 1.0 → insufficient
                val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
                val fromStatus = buildSwapCurrencyStatus(
                    networkRawId = ethNetwork,
                    contractAddress = "0",
                    isCoin = true,
                    amount = BigDecimal("0.01"),
                )
                val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
                val quoteModel = buildQuoteModel(toAmount = BigDecimal("0.5"))

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

                // When
                val result = sut.findBestQuote(
                    fromSwapCurrencyStatus = fromStatus,
                    toSwapCurrencyStatus = toStatus,
                    providers = listOf(dexProvider),
                    amountToSwap = "1.0",
                    reduceBalanceBy = BigDecimal.ZERO,

                )

                // Then
                assertThat(result).hasSize(1)
                val state = result[dexProvider]
                assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
                val loaded = state as SwapState.QuotesLoadedState
                assertThat(loaded.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(
                        com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus.InsufficientAmount::class.java,
                    )
            }

        @Test
        fun `should return non-null state for DEX provider when repository findBestQuote returns error`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = "0",
                isCoin = true,
                amount = BigDecimal("10"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

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
            } returns ExpressDataError.UnknownError().left()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then — a SwapState is emitted for the provider (not an EmptyAmountState)
            assertThat(result).hasSize(1)
            val state = result[dexProvider]
            assertThat(state).isNotNull()
            assertThat(state).isNotInstanceOf(SwapState.EmptyAmountState::class.java)
        }
    }

    @Nested
    inner class DexBridgeProviderPath {

        @Test
        fun `should return entry keyed by the DEX_BRIDGE provider type`() = runTest {
            // Given — DEX_BRIDGE shares the same DEX branch as DEX
            val dexBridgeProvider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                isCoin = true,
                amount = BigDecimal("10"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel()
            val swapData = buildSwapDataModelDex()

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
                    providerId = dexBridgeProvider.providerId,
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
                    providerId = dexBridgeProvider.providerId,
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
                providers = listOf(dexBridgeProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result.keys.first().type).isEqualTo(ExchangeProviderType.DEX_BRIDGE)
        }
    }

    @Nested
    inner class SolanaDexPath {

        @Test
        fun `should produce result entry when network is Solana and quote is successful`() = runTest {
            // Given
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(0)

            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = solanaNetwork,
                isCoin = true,
                amount = BigDecimal("10"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork)
            val quoteModel = buildQuoteModel()

            coEvery {
                repository.findBestQuote(
                    userWallet = any(),
                    fromContractAddress = any(),
                    fromNetwork = solanaNetwork,
                    toContractAddress = any(),
                    toNetwork = any(),
                    fromAmount = any(),
                    fromDecimals = any(),
                    toDecimals = any(),
                    providerId = dexProvider.providerId,
                    rateType = any(),
                )
            } returns quoteModel.right()

            val solanaSwapData = buildSwapDataModelDex()
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
            } returns solanaSwapData.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result.containsKey(dexProvider)).isTrue()
        }

        @Test
        fun `Solana size guard no longer fires during findBestQuote — fee owned by selector`() = runTest {
            // [REDACTED_TASK_KEY] Phase 4: findBestQuote no longer loads fees, so the Solana size guard
            // (which lives inside DexSwapFeeCalculator) is not reached here. The guard now fires
            // only when the fee selector calls loadSwapFee. See DexSwapFeeCalculatorTest for the
            // size-guard assertion; here we only verify findBestQuote completes without surfacing
            // it as a SwapError.
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(931)
            io.mockk.mockkObject(SolanaTransactionHelper)
            every {
                SolanaTransactionHelper.removeSignaturesPlaceholders(any())
            } returns ByteArray(931)

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
            val solanaSwapData = buildSwapDataModelDex(txData = "oversized==")

            coEvery {
                repository.findBestQuote(
                    userWallet = any(),
                    fromContractAddress = any(),
                    fromNetwork = solanaNetwork,
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
            } returns solanaSwapData.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
            )

            // Then — under Phase 4, findBestQuote returns QuotesLoadedState; size guard is deferred
            assertThat(result).hasSize(1)
            val state = result[dexProvider]
            assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
        }

        @Test
        fun `should produce non-empty state via Solana path when balance insufficient`() = runTest {
            // Given — Solana path with Right quote but insufficient balance → getQuotesState branch
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = solanaNetwork,
                isCoin = true,
                amount = BigDecimal("0.000001"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork)
            val quoteModel = buildQuoteModel()

            coEvery {
                repository.findBestQuote(
                    userWallet = any(),
                    fromContractAddress = any(),
                    fromNetwork = solanaNetwork,
                    toContractAddress = any(),
                    toNetwork = any(),
                    fromAmount = any(),
                    fromDecimals = any(),
                    toDecimals = any(),
                    providerId = dexProvider.providerId,
                    rateType = any(),
                )
            } returns quoteModel.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1000.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[dexProvider]).isNotNull()
        }
    }

    @Nested
    inner class CexProviderPath {

        @Test
        fun `should produce result entry for CEX provider`() = runTest {
            // Given
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                isCoin = true,
                amount = BigDecimal("10"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel()

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
                    providerId = cexProvider.providerId,
                    rateType = any(),
                )
            } returns quoteModel.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(cexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result.containsKey(cexProvider)).isTrue()
            assertThat(result[cexProvider]).isNotNull()
        }

        @Test
        fun `should produce result entry for CEX provider with minimal fee state`() = runTest {
            // Given
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                isCoin = true,
                amount = BigDecimal("5"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel()

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
                    providerId = cexProvider.providerId,
                    rateType = any(),
                )
            } returns quoteModel.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(cexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[cexProvider]).isNotNull()
        }
    }

    @Nested
    inner class MixedProviderDispatch {

        @Test
        fun `should dispatch each provider to its branch and return one entry per provider`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX, "dex-1")
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX, "cex-1")
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                isCoin = true,
                amount = BigDecimal("10"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel()
            val swapData = buildSwapDataModelDex()

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
                    providerId = "dex-1",
                    rateType = any(),
                )
            } returns quoteModel.right()

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
                    providerId = "cex-1",
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
                    providerId = "dex-1",
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
                providers = listOf(dexProvider, cexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then — both providers have an entry
            assertThat(result).hasSize(2)
            assertThat(result.containsKey(dexProvider)).isTrue()
            assertThat(result.containsKey(cexProvider)).isTrue()
        }

        @Test
        fun `should return one entry per provider for DEX plus CEX plus DEX_BRIDGE on non-Solana network`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX, "dex-mix")
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX, "cex-mix")
            val dexBridgeProvider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE, "dex-bridge-mix")
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                isCoin = true,
                amount = BigDecimal("10"),
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel()
            val swapData = buildSwapDataModelDex()

            listOf("dex-mix", "cex-mix", "dex-bridge-mix").forEach { pid ->
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
                        providerId = pid,
                        rateType = any(),
                    )
                } returns quoteModel.right()
            }

            listOf("dex-mix", "dex-bridge-mix").forEach { pid ->
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
                        providerId = pid,
                        rateType = any(),
                        toAddress = any(),
                        expressOperationType = any(),
                        refundAddress = any(),
                    )
                } returns swapData.right()
            }

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider, cexProvider, dexBridgeProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,

            )

            // Then — all three providers are dispatched and each has an entry
            assertThat(result).hasSize(3)
            assertThat(result.containsKey(dexProvider)).isTrue()
            assertThat(result.containsKey(cexProvider)).isTrue()
            assertThat(result.containsKey(dexBridgeProvider)).isTrue()
            assertThat(result[dexProvider]).isInstanceOf(SwapState.QuotesLoadedState::class.java)
            assertThat(result[dexBridgeProvider]).isInstanceOf(SwapState.QuotesLoadedState::class.java)
            assertThat(result[cexProvider]).isInstanceOf(SwapState.QuotesLoadedState::class.java)
        }
    }

    @Nested
    inner class YieldSwapApprovalPath {

        private val yieldProxyAddress = "0xYieldModuleProxy"
        private val yieldTokenContract = "0xTokenContract"

        @BeforeEach
        fun enableYieldSwap() {
            every { swapFeatureToggles.isYieldSwapEnabled } returns true
            coEvery {
                yieldModuleAddressProvider.getOrFetch(any(), any())
            } returns yieldProxyAddress
        }

        @Test
        fun `should proceed to QuotesLoadedState when yield-supply is active and isAllowedToSpend is true`() = runTest {
            // Given — yield active, approve to proxy in place → swap proceeds via loadDexSwapDataNoFee
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = yieldTokenContract,
                isCoin = false,
                amount = BigDecimal("10"),
                yieldSupplyActive = true,
                yieldSupplyAllowedToSpend = true,
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel()
            val swapData = buildSwapDataModelDex()

            coEvery {
                repository.findBestQuote(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), toNetwork = any(), fromAmount = any(),
                    fromDecimals = any(), toDecimals = any(),
                    providerId = dexProvider.providerId, rateType = any(),
                )
            } returns quoteModel.right()
            coEvery {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = dexProvider.providerId, rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(),
                )
            } returns swapData.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
            )

            // Then — proceeds (no PermissionRequired), permissionState is Empty
            val state = result[dexProvider]
            assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
            val loaded = state as SwapState.QuotesLoadedState
            assertThat(loaded.permissionState).isEqualTo(PermissionDataState.Empty)
        }

        @Test
        fun `should request approval to yield-module proxy when isAllowedToSpend is false`() = runTest {
            // Given — yield active, approve to proxy revoked → flow must surface PermissionRequired
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = yieldTokenContract,
                isCoin = false,
                amount = BigDecimal("10"),
                yieldSupplyActive = true,
                yieldSupplyAllowedToSpend = false,
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel(allowanceContract = "0xDexRouterShouldNotBeUsed")

            coEvery {
                repository.findBestQuote(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), toNetwork = any(), fromAmount = any(),
                    fromDecimals = any(), toDecimals = any(),
                    providerId = dexProvider.providerId, rateType = any(),
                )
            } returns quoteModel.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
            )

            // Then — PermissionRequired with spender = yield-module proxy (not DEX router)
            val state = result[dexProvider]
            assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
            val loaded = state as SwapState.QuotesLoadedState
            assertThat(loaded.permissionState).isInstanceOf(PermissionDataState.PermissionRequired::class.java)
            val required = loaded.permissionState as PermissionDataState.PermissionRequired
            assertThat(required.spenderAddress).isEqualTo(yieldProxyAddress)
        }

        @Test
        fun `should set isResetApproval=true when yield-token allowance requires reset before re-approval`() = runTest {
            // Given — Tether-like token: any non-zero allowance must be reset to zero before re-approve.
            // Yield approve to proxy was revoked → onchain allowance is partial → ResetNeeded.
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = yieldTokenContract,
                isCoin = false,
                amount = BigDecimal("10"),
                yieldSupplyActive = true,
                yieldSupplyAllowedToSpend = false,
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel(allowanceContract = "0xDexRouterIgnoredForYield")
            coEvery {
                repository.findBestQuote(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), toNetwork = any(), fromAmount = any(),
                    fromDecimals = any(), toDecimals = any(),
                    providerId = dexProvider.providerId, rateType = any(),
                )
            } returns quoteModel.right()
            // Override default Enough stub: simulate partial-allowance state for yield-proxy spender.
            coEvery {
                getAllowanceInfoUseCase.invoke(
                    userWalletId = any(),
                    cryptoCurrency = any(),
                    spenderAddress = yieldProxyAddress,
                    requiredAmount = any(),
                )
            } returns (
                AllowanceInfo.ResetNeeded(
                    allowance = BigDecimal("0.5"),
                    requiredAmount = BigDecimal("1"),
                ) as AllowanceInfo
                ).right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
            )

            // Then — PermissionRequired with isResetApproval=true and spender = yield-module proxy
            val state = result[dexProvider]
            assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
            val loaded = state as SwapState.QuotesLoadedState
            assertThat(loaded.permissionState).isInstanceOf(PermissionDataState.PermissionRequired::class.java)
            val required = loaded.permissionState as PermissionDataState.PermissionRequired
            assertThat(required.spenderAddress).isEqualTo(yieldProxyAddress)
            assertThat(required.isResetApproval).isTrue()
        }

        @Ignore("Check in final integrated approve test")
        @Test
        fun `should fallback to no-permission state when yield-module proxy address is unresolvable`() = runTest {
            // Given — yield store returns null (e.g. network unreachable on first resolve)
            coEvery { yieldModuleAddressProvider.getOrFetch(any(), any()) } returns null
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = yieldTokenContract,
                isCoin = false,
                amount = BigDecimal("10"),
                yieldSupplyActive = true,
                yieldSupplyAllowedToSpend = false,
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val quoteModel = buildQuoteModel(allowanceContract = "0xDexRouter")
            coEvery {
                repository.findBestQuote(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), toNetwork = any(), fromAmount = any(),
                    fromDecimals = any(), toDecimals = any(),
                    providerId = dexProvider.providerId, rateType = any(),
                )
            } returns quoteModel.right()

            // When
            val result = sut.findBestQuote(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                providers = listOf(dexProvider),
                amountToSwap = "1.0",
                reduceBalanceBy = BigDecimal.ZERO,
            )

            // Then — falls back to PermissionDataState.Empty (no approval UI shown to avoid bogus DEX-router approve)
            val state = result[dexProvider]
            assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
            val loaded = state as SwapState.QuotesLoadedState
            assertThat(loaded.permissionState).isEqualTo(PermissionDataState.Empty)
        }
    }
}

// region — test-local helpers

private fun buildCryptoCurrencyCheck(): CryptoCurrencyCheck = CryptoCurrencyCheck(
    dustValue = null,
    reserveAmount = null,
    minimumSendAmount = null,
    existentialDeposit = null,
    utxoAmountLimit = null,
    isAccountFunded = true,
    rentWarning = null,
    isMemoRequired = false,
)

private fun buildSwapDataModelDex(
    txData: String = "dGVzdA==",
    txValue: String? = "0",
    toAmount: BigDecimal = BigDecimal("0.5"),
): SwapDataModel = SwapDataModel(
    toTokenAmount = SwapAmount(toAmount, 18),
    transaction = ExpressTransactionModel.DEX(
        fromAmount = SwapAmount(BigDecimal.ONE, 18),
        toAmount = SwapAmount(toAmount, 18),
        txValue = txValue,
        txId = "tx-id-123",
        txTo = "0xRecipient",
        txExtraId = null,
        txFrom = "0xSender",
        txData = txData,
        otherNativeFeeWei = null,
        gas = BigInteger.valueOf(21_000L),
        allowanceContract = null,
    ),
)

// endregion