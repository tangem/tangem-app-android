package com.tangem.data.swap

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.express.models.request.PairsRequestBody
import com.tangem.datasource.api.express.models.response.*
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.express.models.*
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.swap.models.SwapStatus
import com.tangem.domain.swap.models.SwapTxType
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSwapRepositoryV2Test {

    private val tangemExpressApi: TangemExpressApi = mockk()
    private val expressRepository: ExpressRepository = mockk()
    private val appPreferencesStore: AppPreferencesStore = mockk(relaxed = true)
    private val dataSignatureVerifier: DataSignatureVerifier = mockk()
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier = mockk()
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher = mockk()
    private val moshi: Moshi = Moshi.Builder().build()
    private val featureTogglesManager: FeatureTogglesManager = mockk {
        every { isFeatureEnabled(any()) } returns false
    }

    private val repository = DefaultSwapRepositoryV2(
        tangemExpressApi = tangemExpressApi,
        expressRepository = expressRepository,
        coroutineDispatcher = TestingCoroutineDispatcherProvider(),
        appPreferencesStore = appPreferencesStore,
        dataSignatureVerifier = dataSignatureVerifier,
        singleQuoteStatusSupplier = singleQuoteStatusSupplier,
        singleQuoteStatusFetcher = singleQuoteStatusFetcher,
        featureTogglesManager = featureTogglesManager,
        moshi = moshi,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            tangemExpressApi,
            expressRepository,
            appPreferencesStore,
            dataSignatureVerifier,
            singleQuoteStatusSupplier,
            singleQuoteStatusFetcher,
            featureTogglesManager,
        )
        every { featureTogglesManager.isFeatureEnabled(any()) } returns false
    }

    // region getPairs(SwapCurrencyStatus, SwapCurrencyStatus)

    @Test
    fun `getPairs with SwapCurrencyStatus returns mapped pairs when providers match`() = runTest {
        // Arrange
        val primaryStatus = createCryptoCurrencyStatus(primaryCoin)
        val secondaryStatus = createCryptoCurrencyStatus(secondaryCoin)
        val primarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = primaryStatus,
            account = mockk(),
        )
        val secondarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = secondaryStatus,
            account = mockk(),
        )

        val swapPair = SwapPair(
            from = LeastTokenInfo(contractAddress = "0", network = ETH_BACKEND_ID),
            to = LeastTokenInfo(contractAddress = "0", network = BTC_BACKEND_ID),
            providers = listOf(SwapPairProvider(providerId = PROVIDER_ID, rateTypes = listOf(RateType.FLOAT))),
        )

        coEvery {
            tangemExpressApi.getPairs(any(), any(), any())
        } returns ApiResponse.Success(listOf(swapPair))

        coEvery {
            expressRepository.getProviders(any(), any())
        } returns listOf(expressProvider)

        // Act
        val result = repository.getPairs(
            primarySwapCurrencyStatus = primarySwapCurrencyStatus,
            secondarySwapCurrencyStatus = secondarySwapCurrencyStatus,
            filterProviderTypes = emptyList(),
            swapTxType = SwapTxType.Swap,
        )

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result.first().from).isEqualTo(primaryStatus)
        assertThat(result.first().to).isEqualTo(secondaryStatus)
        assertThat(result.first().providers).hasSize(1)
        assertThat(result.first().providers.first().providerId).isEqualTo(PROVIDER_ID)
    }

    @Test
    fun `getPairs with SwapCurrencyStatus returns empty when no providers match`() = runTest {
        // Arrange
        val primaryStatus = createCryptoCurrencyStatus(primaryCoin)
        val secondaryStatus = createCryptoCurrencyStatus(secondaryCoin)
        val primarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = primaryStatus,
            account = mockk(),
        )
        val secondarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = secondaryStatus,
            account = mockk(),
        )

        val swapPair = SwapPair(
            from = LeastTokenInfo(contractAddress = "0", network = ETH_BACKEND_ID),
            to = LeastTokenInfo(contractAddress = "0", network = BTC_BACKEND_ID),
            providers = listOf(SwapPairProvider(providerId = "unknown-provider", rateTypes = listOf(RateType.FLOAT))),
        )

        coEvery {
            tangemExpressApi.getPairs(any(), any(), any())
        } returns ApiResponse.Success(listOf(swapPair))

        coEvery {
            expressRepository.getProviders(any(), any())
        } returns listOf(expressProvider)

        // Act
        val result = repository.getPairs(
            primarySwapCurrencyStatus = primarySwapCurrencyStatus,
            secondarySwapCurrencyStatus = secondarySwapCurrencyStatus,
            filterProviderTypes = emptyList(),
            swapTxType = SwapTxType.Swap,
        )

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `getPairs with SwapCurrencyStatus returns empty when API returns empty pairs`() = runTest {
        // Arrange
        val primarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = createCryptoCurrencyStatus(primaryCoin),
            account = mockk(),
        )
        val secondarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = createCryptoCurrencyStatus(secondaryCoin),
            account = mockk(),
        )

        coEvery {
            tangemExpressApi.getPairs(any(), any(), any())
        } returns ApiResponse.Success(emptyList())

        coEvery {
            expressRepository.getProviders(any(), any())
        } returns listOf(expressProvider)

        // Act
        val result = repository.getPairs(
            primarySwapCurrencyStatus = primarySwapCurrencyStatus,
            secondarySwapCurrencyStatus = secondarySwapCurrencyStatus,
            filterProviderTypes = emptyList(),
            swapTxType = SwapTxType.Swap,
        )

        // Assert
        assertThat(result).isEmpty()
    }

    // endregion

    // region getPairs(UserWallet, CryptoCurrency, List<CryptoCurrencyStatus>)

    @Test
    fun `getPairs with currency status list returns mapped pairs`() = runTest {
        // Arrange
        val primaryStatus = createCryptoCurrencyStatus(primaryCoin)
        val secondaryStatus = createCryptoCurrencyStatus(secondaryCoin)

        val swapPair = SwapPair(
            from = LeastTokenInfo(contractAddress = "0", network = ETH_BACKEND_ID),
            to = LeastTokenInfo(contractAddress = "0", network = BTC_BACKEND_ID),
            providers = listOf(SwapPairProvider(providerId = PROVIDER_ID, rateTypes = listOf(RateType.FLOAT))),
        )

        coEvery {
            tangemExpressApi.getPairs(any(), any(), any())
        } returns ApiResponse.Success(listOf(swapPair))

        coEvery {
            expressRepository.getProviders(any(), any())
        } returns listOf(expressProvider)

        // Act
        val result = repository.getPairs(
            userWallet = userWallet,
            initialCurrency = primaryCoin,
            cryptoCurrencyStatusList = listOf(primaryStatus, secondaryStatus),
            filterProviderTypes = emptyList(),
            swapTxType = SwapTxType.Swap,
        )

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result.first().from).isEqualTo(primaryStatus)
        assertThat(result.first().to).isEqualTo(secondaryStatus)
    }

    @Test
    fun `getPairs with SendWithSwap only fetches forward pairs`() = runTest {
        // Arrange
        val primaryStatus = createCryptoCurrencyStatus(primaryCoin)
        val secondaryStatus = createCryptoCurrencyStatus(secondaryCoin)

        val swapPair = SwapPair(
            from = LeastTokenInfo(contractAddress = "0", network = ETH_BACKEND_ID),
            to = LeastTokenInfo(contractAddress = "0", network = BTC_BACKEND_ID),
            providers = listOf(SwapPairProvider(providerId = PROVIDER_ID, rateTypes = listOf(RateType.FLOAT))),
        )

        coEvery {
            tangemExpressApi.getPairs(any(), any(), any())
        } returns ApiResponse.Success(listOf(swapPair))

        coEvery {
            expressRepository.getProviders(any(), any())
        } returns listOf(expressProvider)

        // Act
        val result = repository.getPairs(
            userWallet = userWallet,
            initialCurrency = primaryCoin,
            cryptoCurrencyStatusList = listOf(primaryStatus, secondaryStatus),
            filterProviderTypes = emptyList(),
            swapTxType = SwapTxType.SendWithSwap,
        )

        // Assert
        assertThat(result).hasSize(1)

        // SendWithSwap should call getPairs only once (forward), not twice (forward + reverse)
        coVerify(exactly = 1) { tangemExpressApi.getPairs(any(), any(), any()) }
    }

    // endregion

    // region getSwapQuote

    @Test
    fun `getSwapQuote returns correct quote model for fromAmount`() = runTest {
        // Arrange
        val quoteResponse = ExchangeQuoteResponse(
            fromAmount = "1000000000000000000",
            fromDecimals = 18,
            toAmount = "100000000",
            toDecimals = 8,
            allowanceContract = "0xAllowance",
            minAmount = BigDecimal.ONE,
            quoteId = "quote-123",
        )

        coEvery {
            tangemExpressApi.getExchangeQuote(
                fromAmount = any(),
                toAmount = any(),
                fromNetwork = any(),
                fromContractAddress = any(),
                fromDecimals = any(),
                toNetwork = any(),
                toContractAddress = any(),
                toDecimals = any(),
                providerId = any(),
                rateType = any(),
                userWalletId = any(),
                refCode = any(),
            )
        } returns ApiResponse.Success(quoteResponse)

        // Act
        val result = repository.getSwapQuote(
            userWallet = userWallet,
            fromCryptoCurrency = primaryCoin,
            toCryptoCurrency = secondaryCoin,
            amount = BigDecimal.ONE,
            amountType = SwapAmountType.From,
            provider = expressProvider,
            rateType = ExpressRateType.Float,
        )

        // Assert
        assertThat(result.provider).isEqualTo(expressProvider)
        assertThat(result.toTokenAmount).isEqualTo(BigDecimal("1.00000000"))
        assertThat(result.fromTokenAmount).isEqualTo(BigDecimal("1.000000000000000000"))
        assertThat(result.allowanceContract).isEqualTo("0xAllowance")
        assertThat(result.quoteId).isEqualTo("quote-123")
    }

    @Test
    fun `getSwapQuote sends fromAmount when amountType is From`() = runTest {
        // Arrange
        val quoteResponse = ExchangeQuoteResponse(
            fromAmount = "1000000000000000000",
            fromDecimals = 18,
            toAmount = "100000000",
            toDecimals = 8,
            allowanceContract = null,
            minAmount = BigDecimal.ONE,
            quoteId = null,
        )

        coEvery {
            tangemExpressApi.getExchangeQuote(
                fromAmount = any(),
                toAmount = any(),
                fromNetwork = any(),
                fromContractAddress = any(),
                fromDecimals = any(),
                toNetwork = any(),
                toContractAddress = any(),
                toDecimals = any(),
                providerId = any(),
                rateType = any(),
                userWalletId = any(),
                refCode = any(),
            )
        } returns ApiResponse.Success(quoteResponse)

        // Act
        repository.getSwapQuote(
            userWallet = userWallet,
            fromCryptoCurrency = primaryCoin,
            toCryptoCurrency = secondaryCoin,
            amount = BigDecimal("2.5"),
            amountType = SwapAmountType.From,
            provider = expressProvider,
            rateType = ExpressRateType.Float,
        )

        // Assert — fromAmount is set, toAmount is null
        coVerify {
            tangemExpressApi.getExchangeQuote(
                fromAmount = "2500000000000000000",
                toAmount = null,
                fromNetwork = ETH_BACKEND_ID,
                fromContractAddress = "0",
                fromDecimals = 18,
                toNetwork = BTC_BACKEND_ID,
                toContractAddress = "0",
                toDecimals = 8,
                providerId = PROVIDER_ID,
                rateType = "float",
                userWalletId = any(),
                refCode = any(),
            )
        }
    }

    @Test
    fun `getSwapQuote sends toAmount when amountType is To`() = runTest {
        // Arrange
        val quoteResponse = ExchangeQuoteResponse(
            fromAmount = "1000000000000000000",
            fromDecimals = 18,
            toAmount = "100000000",
            toDecimals = 8,
            allowanceContract = null,
            minAmount = BigDecimal.ONE,
            quoteId = null,
        )

        coEvery {
            tangemExpressApi.getExchangeQuote(
                fromAmount = any(),
                toAmount = any(),
                fromNetwork = any(),
                fromContractAddress = any(),
                fromDecimals = any(),
                toNetwork = any(),
                toContractAddress = any(),
                toDecimals = any(),
                providerId = any(),
                rateType = any(),
                userWalletId = any(),
                refCode = any(),
            )
        } returns ApiResponse.Success(quoteResponse)

        // Act
        repository.getSwapQuote(
            userWallet = userWallet,
            fromCryptoCurrency = primaryCoin,
            toCryptoCurrency = secondaryCoin,
            amount = BigDecimal("1.5"),
            amountType = SwapAmountType.To,
            provider = expressProvider,
            rateType = ExpressRateType.Fixed,
        )

        // Assert — toAmount is set, fromAmount is null
        coVerify {
            tangemExpressApi.getExchangeQuote(
                fromAmount = null,
                toAmount = "150000000",
                fromNetwork = ETH_BACKEND_ID,
                fromContractAddress = "0",
                fromDecimals = 18,
                toNetwork = BTC_BACKEND_ID,
                toContractAddress = "0",
                toDecimals = 8,
                providerId = PROVIDER_ID,
                rateType = "fixed",
                userWalletId = any(),
                refCode = any(),
            )
        }
    }

    // endregion

    // region getExchangeStatus

    @Test
    fun `getExchangeStatus returns converted status model`() = runTest {
        // Arrange
        val statusResponse = ExchangeStatusResponse(
            providerId = PROVIDER_ID,
            status = ExchangeStatus.Finished,
            externalTxId = "ext-tx-1",
            externalTxUrl = "https://example.com/tx/1",
            error = null,
        )

        coEvery {
            tangemExpressApi.getExchangeStatus(any(), any(), any())
        } returns ApiResponse.Success(statusResponse)

        // Act
        val result = repository.getExchangeStatus(userWallet = userWallet, txId = "tx-123")

        // Assert
        assertThat(result.providerId).isEqualTo(PROVIDER_ID)
        assertThat(result.status).isEqualTo(SwapStatus.Finished)
        assertThat(result.txId).isEqualTo("ext-tx-1")
        assertThat(result.txExternalUrl).isEqualTo("https://example.com/tx/1")
    }

    // endregion

    // region swapTransactionSent

    @Test
    fun `swapTransactionSent calls exchangeSent API`() = runTest {
        // Arrange
        val fromStatus = createCryptoCurrencyStatus(primaryCoin)

        coEvery {
            tangemExpressApi.exchangeSent(any(), any(), any())
        } returns ApiResponse.Success(ExchangeSentResponseBody(txId = "tx-1", status = "ok"))

        // Act
        repository.swapTransactionSent(
            userWallet = userWallet,
            fromCryptoCurrencyStatus = fromStatus,
            payInAddress = "0xPayIn",
            txId = "tx-1",
            txHash = "0xHash",
            txExtraId = null,
        )

        // Assert
        coVerify {
            tangemExpressApi.exchangeSent(
                userWalletId = any(),
                refCode = any(),
                body = match { body ->
                    body.txId == "tx-1" &&
                        body.txHash == "0xHash" &&
                        body.payinAddress == "0xPayIn" &&
                        body.payinExtraId == null
                },
            )
        }
    }

    // endregion

    // region filterYieldSupplyProvider

    @Test
    fun `getPairs filters out DEX providers when yield supply is active and flag is off`() = runTest {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1326_YIELD_MODE_SWAP_ENABLED) } returns false
        val primaryStatus = createCryptoCurrencyStatusWithActiveYield(primaryCoin)
        val secondaryStatus = createCryptoCurrencyStatus(secondaryCoin)
        val primarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = primaryStatus,
            account = mockk(),
        )
        val secondarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = secondaryStatus,
            account = mockk(),
        )

        val swapPair = SwapPair(
            from = LeastTokenInfo(contractAddress = "0", network = ETH_BACKEND_ID),
            to = LeastTokenInfo(contractAddress = "0", network = BTC_BACKEND_ID),
            providers = listOf(
                SwapPairProvider(providerId = PROVIDER_ID, rateTypes = listOf(RateType.FLOAT)),
                SwapPairProvider(providerId = CEX_PROVIDER_ID, rateTypes = listOf(RateType.FLOAT)),
            ),
        )

        coEvery {
            tangemExpressApi.getPairs(any(), any(), any())
        } returns ApiResponse.Success(listOf(swapPair))

        coEvery {
            expressRepository.getProviders(any(), any())
        } returns listOf(dexProvider, cexProvider)

        // Act
        val result = repository.getPairs(
            primarySwapCurrencyStatus = primarySwapCurrencyStatus,
            secondarySwapCurrencyStatus = secondarySwapCurrencyStatus,
            filterProviderTypes = emptyList(),
            swapTxType = SwapTxType.Swap,
        )

        // Assert — only CEX provider should remain
        assertThat(result).hasSize(2)
        val providers = result.first().providers
        assertThat(providers).hasSize(1)
        assertThat(providers.first().type).isEqualTo(ExpressProviderType.CEX)
    }

    @Test
    fun `getPairs keeps DEX providers when yield supply is active and flag is on`() = runTest {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1326_YIELD_MODE_SWAP_ENABLED) } returns true
        val primaryStatus = createCryptoCurrencyStatusWithActiveYield(primaryCoin)
        val secondaryStatus = createCryptoCurrencyStatus(secondaryCoin)
        val primarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = primaryStatus,
            account = mockk(),
        )
        val secondarySwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = secondaryStatus,
            account = mockk(),
        )

        val swapPair = SwapPair(
            from = LeastTokenInfo(contractAddress = "0", network = ETH_BACKEND_ID),
            to = LeastTokenInfo(contractAddress = "0", network = BTC_BACKEND_ID),
            providers = listOf(
                SwapPairProvider(providerId = PROVIDER_ID, rateTypes = listOf(RateType.FLOAT)),
                SwapPairProvider(providerId = CEX_PROVIDER_ID, rateTypes = listOf(RateType.FLOAT)),
            ),
        )

        coEvery {
            tangemExpressApi.getPairs(any(), any(), any())
        } returns ApiResponse.Success(listOf(swapPair))

        coEvery {
            expressRepository.getProviders(any(), any())
        } returns listOf(dexProvider, cexProvider)

        // Act
        val result = repository.getPairs(
            primarySwapCurrencyStatus = primarySwapCurrencyStatus,
            secondarySwapCurrencyStatus = secondarySwapCurrencyStatus,
            filterProviderTypes = emptyList(),
            swapTxType = SwapTxType.Swap,
        )

        // Assert — both providers should remain
        assertThat(result).hasSize(2)
        val providers = result.first().providers
        assertThat(providers).hasSize(2)
    }

    // endregion

    // region getSwapData

    @Test
    fun `getSwapData throws InvalidSignatureError when signature verification fails`() = runTest {
        // Arrange
        val fromStatus = createCryptoCurrencyStatus(primaryCoin)
        val exchangeDataResponse = ExchangeDataResponse(
            fromAmount = "1000000000000000000",
            fromDecimals = 18,
            toAmount = "100000000",
            toDecimals = 8,
            txId = "tx-123",
            txDetailsJson = "{}",
            signature = "invalid-sig",
        )

        coEvery {
            tangemExpressApi.getExchangeData(
                fromContractAddress = any(),
                toContractAddress = any(),
                fromNetwork = any(),
                toNetwork = any(),
                fromAddress = any(),
                toAddress = any(),
                fromDecimals = any(),
                toDecimals = any(),
                fromAmount = any(),
                toAmount = any(),
                providerId = any(),
                rateType = any(),
                requestId = any(),
                refundAddress = any(),
                refundExtraId = any(),
                userWalletId = any(),
                partnerOperationType = any(),
                refCode = any(),
                toExtraId = any(),
                quoteId = any(),
            )
        } returns ApiResponse.Success(exchangeDataResponse)

        every { dataSignatureVerifier.verifySignature(any(), any()) } returns false

        // Act & Assert
        assertThrows<ExpressError.InvalidSignatureError> {
            repository.getSwapData(
                userWallet = userWallet,
                fromCryptoCurrencyStatus = fromStatus,
                toCryptoCurrency = secondaryCoin,
                amount = BigDecimal.ONE,
                amountType = SwapAmountType.From,
                toAddress = "0xToAddress",
                toExtraId = null,
                expressProvider = cexProvider,
                rateType = ExpressRateType.Float,
                expressOperationType = ExpressOperationType.SWAP,
                quoteId = null,
            )
        }
    }

    // endregion

    private companion object {
        const val ETH_BACKEND_ID = "ethereum"
        const val BTC_BACKEND_ID = "bitcoin"
        const val PROVIDER_ID = "dex-provider-1"
        const val CEX_PROVIDER_ID = "cex-provider-1"

        val userWallet: UserWallet = MockUserWalletFactory.create()

        val ethNetwork: Network = mockk(relaxed = true) {
            every { rawId } returns ETH_BACKEND_ID
        }

        val btcNetwork: Network = mockk(relaxed = true) {
            every { rawId } returns BTC_BACKEND_ID
        }

        val primaryCoin: CryptoCurrency.Coin = mockk(relaxed = true) {
            every { network } returns ethNetwork
            every { decimals } returns 18
            every { id } returns mockk(relaxed = true) {
                every { rawCurrencyId } returns CryptoCurrency.RawID("ethereum")
            }
        }

        val secondaryCoin: CryptoCurrency.Coin = mockk(relaxed = true) {
            every { network } returns btcNetwork
            every { decimals } returns 8
            every { id } returns mockk(relaxed = true) {
                every { rawCurrencyId } returns CryptoCurrency.RawID("bitcoin")
            }
        }

        val expressProvider = ExpressProvider(
            providerId = PROVIDER_ID,
            rateTypes = listOf(ExpressRateType.Float),
            name = "Test DEX",
            type = ExpressProviderType.DEX,
            imageLarge = "",
            termsOfUse = null,
            privacyPolicy = null,
            slippage = null,
        )

        val dexProvider = expressProvider

        val cexProvider = ExpressProvider(
            providerId = CEX_PROVIDER_ID,
            rateTypes = listOf(ExpressRateType.Float),
            name = "Test CEX",
            type = ExpressProviderType.CEX,
            imageLarge = "",
            termsOfUse = null,
            privacyPolicy = null,
            slippage = null,
        )

        fun createCryptoCurrencyStatus(currency: CryptoCurrency): CryptoCurrencyStatus {
            val value: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
                every { yieldSupplyStatus } returns null
                every { networkAddress } returns mockk(relaxed = true) {
                    every { defaultAddress } returns mockk(relaxed = true) {
                        every { value } returns "0xAddress"
                    }
                }
            }
            return CryptoCurrencyStatus(currency = currency, value = value)
        }

        fun createCryptoCurrencyStatusWithActiveYield(currency: CryptoCurrency): CryptoCurrencyStatus {
            val yieldStatus: YieldSupplyStatus = mockk {
                every { isActive } returns true
            }
            val value: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
                every { yieldSupplyStatus } returns yieldStatus
                every { networkAddress } returns mockk(relaxed = true) {
                    every { defaultAddress } returns mockk(relaxed = true) {
                        every { value } returns "0xAddress"
                    }
                }
            }
            return CryptoCurrencyStatus(currency = currency, value = value)
        }
    }
}