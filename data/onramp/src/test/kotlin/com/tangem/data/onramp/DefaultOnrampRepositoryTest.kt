package com.tangem.data.onramp

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
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
import com.tangem.grow.datasource.express.ExpressUtils
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
    private val repository = DefaultOnrampRepository(
        onrampApi = api,
        expressApi = mockk(),
        dispatchers = dispatchers,
        appPreferencesStore = createAppPreferencesStore(
            legacyDataStoreMoshi, dispatchers, MockStateDataStore(emptyPreferences()),
        ),
        growPreferencesKeys = GrowPreferencesKeys(),
        expressUtils = expressUtils,
        paymentMethodsStore = methods,
        onrampCurrentCountryByIPStore = countryByIp,
        pairsStore = DefaultOnrampPairsStore(RuntimeSharedMapStore()),
        quotesStore = DefaultOnrampQuotesStore(RuntimeSharedMapStore()),
        countriesStore = countries,
        currenciesStore = currencies,
        walletManagersFacade = mockk(),
        dataSignatureVerifier = mockk(),
        expressHistoryDao = mockk(relaxed = true),
        expressHistoryRepository = mockk(),
        moshi = legacyDataStoreMoshi,
    )
    private val currency = OnrampCurrency("Dollar", "USD", null, 2, "USD")
    private val country = OnrampCountry("USA-US", "US", "US", "", "USA", "NA", currency, true)

    init {
        coEvery { expressUtils.getRefCode(wallet) } returns null
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
    fun `GIVEN populated cache WHEN clearCache THEN currencies countries and methods removed`() = runTest {
        // Arrange
        currencies.store("onramp_currencies", listOf(currency))
        countries.store("onramp_countries", listOf(country))
        methods.store("onramp_payment_methods", emptyList())
        // Act
        repository.clearCache()
        // Assert
        assertThat(currencies.getSyncOrNull("onramp_currencies")).isNull()
        assertThat(repository.getCountriesSync()).isNull()
        assertThat(methods.contains("onramp_payment_methods")).isFalse()
    }
}