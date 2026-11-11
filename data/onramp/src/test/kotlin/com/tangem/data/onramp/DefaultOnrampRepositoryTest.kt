package com.tangem.data.onramp

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.txhistory.ExpressHistoryRepository
import com.tangem.datasource.api.common.response.map
import com.tangem.datasource.local.onramp.countries.DefaultOnrampCountriesStore
import com.tangem.datasource.local.onramp.country.DefaultOnrampCurrentCountryByIPStore
import com.tangem.datasource.local.onramp.currencies.DefaultOnrampCurrenciesStore
import com.tangem.datasource.local.onramp.pairs.DefaultOnrampPairsStore
import com.tangem.datasource.local.onramp.paymentmethods.DefaultOnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.quotes.DefaultOnrampQuotesStore
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampPair
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampProviderInfo
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.model.error.OnrampPairsError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.Amount
import com.tangem.grow.datasource.express.ExpressUtils
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.grow.datasource.express.models.response.ExchangeProvider
import com.tangem.grow.datasource.onramp.models.common.OnrampDestinationDTO
import com.tangem.grow.datasource.onramp.models.request.OnrampPairsRequest
import com.tangem.grow.datasource.onramp.models.response.OnrampQuoteResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampItemResponse
import com.tangem.grow.datasource.onramp.models.response.model.OnrampPairDTO
import com.tangem.grow.datasource.onramp.models.response.model.OnrampProviderDTO
import com.tangem.grow.datasource.onramp.OnrampApi
import com.tangem.grow.datasource.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.grow.datasource.onramp.models.response.model.PaymentMethodDTO
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class DefaultOnrampRepositoryTest {

    private val api = mockk<OnrampApi>()
    private val expressUtils = mockk<ExpressUtils>()
    private val wallet = mockk<UserWallet> {
        every { walletId } returns UserWalletId("0011")
    }
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val currencies = DefaultOnrampCurrenciesStore(RuntimeSharedMapStore())
    private val countries = DefaultOnrampCountriesStore(RuntimeSharedMapStore())
    private val countryByIp = DefaultOnrampCurrentCountryByIPStore(RuntimeSharedStore())
    private val methods = DefaultOnrampPaymentMethodsStore(RuntimeSharedMapStore())
    private val pairs = DefaultOnrampPairsStore(RuntimeSharedMapStore())
    private val quotes = DefaultOnrampQuotesStore(RuntimeSharedMapStore())
    private val expressApi: TangemExpressApi = mockk()
    private val history: ExpressHistoryRepository = mockk()
    private val crypto: CryptoCurrency.Coin = mockk {
        every { network.rawId } returns "ethereum"
        every { decimals } returns 18
        every { symbol } returns "ETH"
    }
    private val repository = DefaultOnrampRepository(
        onrampApi = api,
        expressApi = expressApi,
        dispatchers = dispatchers,
        appPreferencesStore = createAppPreferencesStore(
            legacyDataStoreMoshi, dispatchers, MockStateDataStore(emptyPreferences()),
        ),
        growPreferencesKeys = GrowPreferencesKeys(),
        expressUtils = expressUtils,
        paymentMethodsStore = methods,
        onrampCurrentCountryByIPStore = countryByIp,
        pairsStore = pairs,
        quotesStore = quotes,
        countriesStore = countries,
        currenciesStore = currencies,
        walletManagersFacade = mockk(),
        dataSignatureVerifier = mockk(),
        expressHistoryDao = mockk(relaxed = true),
        expressHistoryRepository = history,
        moshi = legacyDataStoreMoshi,
    )
    private val currency = OnrampCurrency("Dollar", "USD", null, 2, "USD")
    private val country = OnrampCountry("USA-US", "US", "US", "", "USA", "NA", currency, true)

    init {
        coEvery { expressUtils.getRefCode(wallet) } returns null
    }

        @ParameterizedTest
        @ValueSource(booleans = [false, true])
        fun `GIVEN status response WHEN fetched THEN complete status mapped and history saved only on success`(
            failed: Boolean,
        ) = runTest {
            // Arrange
            val response = OnrampItemResponse(
                txId = "tx", providerId = "provider", payoutAddress = "address", status = "finished",
                failReason = null, externalTxId = "external", externalTxUrl = "https://provider/tx", payoutHash = "hash",
                createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:01:00Z",
                fromCurrencyCode = "USD", fromAmount = "1234", fromPrecision = 2,
                toContractAddress = "0", toNetwork = "ethereum", toDecimals = 18,
                toAmount = "10000000000000000", toActualAmount = "9000000000000000",
                paymentMethod = "card", countryCode = "US",
            )
            coEvery { api.getStatus("0011", null, "tx") } returns if (failed) {
                ApiResponse.Error(ApiResponseError.NetworkException()).map<Nothing, OnrampItemResponse> { it }
            } else {
                ApiResponse.Success(response)
            }
            coEvery { history.storeOnramps(listOf(response)) } returns Unit

            // Act
            val result = runCatching { repository.getStatus(wallet, "tx") }

            // Assert
            coVerify(exactly = 1) { api.getStatus("0011", null, "tx") }
            if (failed) {
                assertThat(result.exceptionOrNull()).isInstanceOf(ApiResponseError.NetworkException::class.java)
                coVerify(exactly = 0) { history.storeOnramps(any()) }
            } else {
                assertThat(result.getOrThrow()).isEqualTo(
                    OnrampStatus(
                        txId = "tx", providerId = "provider", payoutAddress = "address", status = OnrampStatus.Status.Finished,
                        failReason = null, externalTxId = "external", externalTxUrl = "https://provider/tx", payoutHash = "hash",
                        createdAt = "2026-01-01T00:00:00Z", fromCurrencyCode = "USD", fromAmount = "1234",
                        toContractAddress = "0", toNetwork = "ethereum", toDecimals = "18",
                        toAmount = "10000000000000000", toActualAmount = "9000000000000000",
                        paymentMethod = "card", countryCode = "US",
                    ),
                )
                coVerify(exactly = 1) { history.storeOnramps(listOf(response)) }
            }
        }

    @Test
    fun `GIVEN successful response WHEN currencies fetched twice THEN mapped cache prevents second request`() = runTest {
        // Arrange
        coEvery { api.getCurrencies("0011", null) } returns ApiResponse.Success(
            listOf(OnrampCurrencyDTO("Dollar", "USD", null, 2, null)),
        )
        // Act
        repository.fetchCurrencies(wallet)
        repository.fetchCurrencies(wallet)
        // Assert
        assertThat(repository.getCurrencies().first()).containsExactly(currency)
        coVerify(exactly = 1) { api.getCurrencies("0011", null) }
    }

    @Test
    fun `GIVEN failed request WHEN currencies fetched again THEN error propagates and request is retried`() = runTest {
        // Arrange
        val failure = IllegalStateException("offline")
        coEvery { api.getCurrencies(any(), any()) } throws failure
        // Act
        val error = runCatching { repository.fetchCurrencies(wallet) }.exceptionOrNull()
        runCatching { repository.fetchCurrencies(wallet) }
        // Assert
        assertThat(error).isSameInstanceAs(failure)
        assertThat(currencies.getSyncOrNull("onramp_currencies")).isNull()
        coVerify(exactly = 2) { api.getCurrencies("0011", null) }
    }

    @Test
    fun `GIVEN cached country WHEN country by IP requested THEN cached value returned without API call`() = runTest {
        // Arrange
        countryByIp.store(country)
        // Act
        val actual = repository.getCountryByIp(wallet, fromCache = true)
        // Assert
        assertThat(actual).isEqualTo(country)
        coVerify(exactly = 0) { api.getCountryByIp(any(), any()) }
    }

    @Test
    fun `GIVEN Apple Pay and card WHEN payment methods fetched twice THEN Apple Pay excluded and cache reused`() = runTest {
        // Arrange
        val card = PaymentMethodDTO("card", "Card", "", null, null)
        coEvery { api.getPaymentMethods("0011", null) } returns ApiResponse.Success(
            listOf(card, card.copy(id = "apple-pay")),
        )
        // Act
        repository.fetchPaymentMethodsIfAbsent(wallet)
        repository.fetchPaymentMethodsIfAbsent(wallet)
        // Assert
        assertThat(methods.getSyncOrNull("onramp_payment_methods")).containsExactly(card)
        coVerify(exactly = 1) { api.getPaymentMethods("0011", null) }
    }

    @Test
    fun `GIVEN default country WHEN currency changed THEN country fields preserved in storage and flow`() = runTest {
        // Arrange
        repository.saveDefaultCountry(country)
        val euro = currency.copy(code = "EUR", unit = "€")
        // Act
        repository.saveDefaultCurrency(euro)
        // Assert
        assertThat(repository.getDefaultCountrySync()).isEqualTo(country.copy(defaultCurrency = euro))
        assertThat(repository.getDefaultCurrency().first()).isEqualTo(euro)
    }

    @Test
    fun `GIVEN country response WHEN countries fetched twice THEN cached country retains availability`() = runTest {
        // Arrange
        val dto = com.tangem.grow.datasource.onramp.models.response.model.OnrampCountryDTO(
            "US", "US", "", "USA", "NA", OnrampCurrencyDTO("Dollar", "USD", null, 2, null), false,
        )
        coEvery { api.getCountries("0011", null) } returns ApiResponse.Success(listOf(dto))
        // Act
        val actual = repository.fetchCountries(wallet)
        val cached = repository.fetchCountries(wallet)
        // Assert
        assertThat(actual).containsExactly(country.copy(isOnrampAvailable = false))
        assertThat(cached).isEmpty()
        assertThat(repository.getCountriesSync()).isEqualTo(actual)
        coVerify(exactly = 1) { api.getCountries("0011", null) }
    }

    @Test
    fun `GIVEN no default country WHEN currency saved THEN no default is created`() = runTest {
        // Act
        repository.saveDefaultCurrency(currency)
        // Assert
        assertThat(repository.getDefaultCountrySync()).isNull()
        assertThat(repository.getDefaultCurrencySync()).isNull()
    }

    @Test
    fun `GIVEN populated cache WHEN clearCache THEN all purchase caches removed`() = runTest {
        // Arrange
        currencies.store("onramp_currencies", listOf(currency))
        countries.store("onramp_countries", listOf(country))
        methods.store("onramp_payment_methods", emptyList())
        pairs.store("onramp_pairs", listOf(OnrampPair(emptyList())))
        quotes.store("onramp_quotes", listOf(mockk()))
        // Act
        repository.clearCache()
        // Assert
        assertThat(currencies.getSyncOrNull("onramp_currencies")).isNull()
        assertThat(repository.getCountriesSync()).isNull()
        assertThat(methods.contains("onramp_payment_methods")).isFalse()
        assertThat(pairs.getSyncOrNull("onramp_pairs")).isNull()
        assertThat(repository.getQuotesSync()).isNull()
    }

    @Test
    fun `GIVEN mixed quote responses WHEN fetched THEN amounts preserved and errors isolated per method`() = runTest {
        // Arrange
        val methods = listOf("card", "small", "failed", "offline").map {
            OnrampPaymentMethod(it, it, "", PaymentMethodType.CARD)
        }
        val provider = OnrampProvider("provider", OnrampProviderInfo("Provider", "", null, null), methods)
        pairs.store("onramp_pairs", listOf(OnrampPair(listOf(provider))))
        repository.saveDefaultCountry(country)
        val amount = Amount("USD", BigDecimal("12.34"), 2)
        val tooSmall = ApiResponseError.HttpException(
            ApiResponseError.HttpException.Code.BAD_REQUEST, "too small",
            """{"error":{"code":2250,"description":"too small","value":{"minAmount":"2000","decimals":2}}}""",
        )
        val generic = ApiResponseError.HttpException(
            ApiResponseError.HttpException.Code.BAD_REQUEST, "failed",
            """{"error":{"code":4001,"description":"failed","value":null}}""",
        )
        coEvery {
            api.getQuote("0011", null, "USD", 2, "0", "ethereum", any(), "US", "1234", 18, "provider")
        } answers {
            when (arg<String>(6)) {
                "card" -> ApiResponse.Success(
                    OnrampQuoteResponse(
                        "USD", "0", "ethereum", "card", "US", "1234", "10000000000000000",
                        18, "provider", null, null, null, null,
                    ),
                )
                "small" -> ApiResponse.Error(tooSmall).map<Nothing, OnrampQuoteResponse> { it }
                "failed" -> ApiResponse.Error(generic).map<Nothing, OnrampQuoteResponse> { it }
                else -> ApiResponse.Error(ApiResponseError.NetworkException())
                    .map<Nothing, OnrampQuoteResponse> { it }
            }
        }

        // Act
        repository.fetchQuotes(wallet, crypto, amount)

        // Assert
        val from = OnrampAmount("USD", BigDecimal("12.34"), 2)
        assertThat(repository.getQuotesSync()).containsExactly(
            OnrampQuote.Data(
                paymentMethod = methods[0], provider = provider, fromAmount = from, countryCode = "US",
                toAmount = OnrampAmount("ETH", BigDecimal("0.010000000000000000"), 18),
                minFromAmount = null, maxFromAmount = null,
            ),
            OnrampQuote.AmountError(
                paymentMethod = methods[1], provider = provider, fromAmount = from, countryCode = "US",
                error = OnrampError.AmountError.TooSmallError(BigDecimal("20.00")),
            ),
            OnrampQuote.Error(
                paymentMethod = methods[2], provider = provider, fromAmount = from, countryCode = "US",
                error = OnrampError.DataError("4001", "failed"),
            ),
        ).inOrder()
        coVerify(exactly = 4) {
            api.getQuote("0011", null, "USD", 2, "0", "ethereum", any(), "US", "1234", 18, "provider")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["valid", "empty", "missingProvider", "pairsError", "providersError"])
    fun `GIVEN pairs response WHEN fetched THEN provider mapping or failure preserves cache contract`(kind: String) =
        runTest {
            // Arrange
            val destination = OnrampDestinationDTO("0", "ethereum")
            methods.store("onramp_payment_methods", listOf(PaymentMethodDTO("card", "Card", "", null, null)))
            val response = listOf(
                OnrampPairDTO("USD", destination, listOf(OnrampProviderDTO("provider", listOf("card", "unknown")))),
            )
            val failure = ApiResponseError.NetworkException()
            coEvery { api.getPairs("0011", null, OnrampPairsRequest("USD", "US", listOf(destination))) } returns
                when (kind) {
                    "empty" -> ApiResponse.Success(emptyList())
                    "pairsError" -> ApiResponse.Error(failure).map<Nothing, List<OnrampPairDTO>> { it }
                    else -> ApiResponse.Success(response)
                }
            val metadata = mockk<ExchangeProvider>(relaxed = true) {
                every { id } returns "provider"
                every { name } returns "Provider"
                every { imageLargeUrl } returns "image"
                every { termsOfUse } returns null
                every { privacyPolicy } returns null
            }
            coEvery { expressApi.getProviders("0011", null) } returns when (kind) {
                "missingProvider" -> ApiResponse.Success(emptyList())
                "providersError" -> ApiResponse.Error(failure).map<Nothing, List<ExchangeProvider>> { it }
                else -> ApiResponse.Success(listOf(metadata))
            }

            // Act
            val error = runCatching { repository.fetchPairs(wallet, currency, country, crypto) }.exceptionOrNull()

            // Assert
            if (kind == "valid") {
                assertThat(error).isNull()
                assertThat(pairs.getSyncOrNull("onramp_pairs")).containsExactly(
                    OnrampPair(listOf(
                        OnrampProvider(
                            "provider", OnrampProviderInfo("Provider", "image", null, null),
                            listOf(OnrampPaymentMethod("card", "Card", "", PaymentMethodType.CARD)),
                        ),
                    )),
                )
            } else {
                assertThat(error).isInstanceOf(
                    if (kind.endsWith("Error")) ApiResponseError.NetworkException::class.java
                    else OnrampPairsError.PairsNotFound::class.java,
                )
                assertThat(pairs.getSyncOrNull("onramp_pairs")).isNull()
            }
        }
}