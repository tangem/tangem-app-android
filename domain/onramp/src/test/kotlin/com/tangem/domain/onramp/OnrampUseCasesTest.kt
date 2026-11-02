package com.tangem.domain.onramp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.HotCryptoRepository
import com.tangem.domain.onramp.repositories.LegacyTopUpRepository
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampUseCasesTest {

    private val repository: OnrampRepository = mockk(relaxUnitFun = true)
    private val transactionRepository: OnrampTransactionRepository = mockk(relaxUnitFun = true)
    private val hotCryptoRepository: HotCryptoRepository = mockk(relaxUnitFun = true)
    private val legacyTopUpRepository: LegacyTopUpRepository = mockk()
    private val errorResolver: OnrampErrorResolver = mockk()
    private val settingsRepository: com.tangem.domain.settings.repositories.SettingsRepository = mockk()
    private val userWallet: UserWallet = mockk(relaxed = true)
    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    private val cryptoCurrencyId: CryptoCurrency.ID = mockk(relaxed = true)

    private val expectedError = OnrampError.DomainError("mapped")
    private val thrown = IllegalStateException("failure")

    @BeforeEach
    fun setUp() {
        clearMocks(
            repository,
            transactionRepository,
            hotCryptoRepository,
            legacyTopUpRepository,
            errorResolver,
            settingsRepository,
            userWallet,
            cryptoCurrency,
            cryptoCurrencyId,
        )
        every { errorResolver.resolve(any()) } returns expectedError
    }

    @Test
    fun `GIVEN saved supported country and no saved currency WHEN check availability THEN saves country currency and returns available`() = runTest {
        // Arrange
        val country = country(isAvailable = true)
        coEvery { repository.getDefaultCountrySync() } returns country
        coEvery { repository.fetchCountries(userWallet) } returns listOf(country)
        coEvery { repository.getDefaultCurrencySync() } returns null

        // Act
        val result = CheckOnrampAvailabilityUseCase(repository, errorResolver)(userWallet)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(OnrampAvailability.Available(country, country.defaultCurrency))
        coVerify { repository.fetchPaymentMethodsIfAbsent(userWallet) }
        coVerify { repository.saveDefaultCurrency(country.defaultCurrency) }
    }

    @Test
    fun `GIVEN no saved country WHEN check availability THEN asks to confirm detected residency`() = runTest {
        // Arrange
        val detected = country(isAvailable = true)
        coEvery { repository.getDefaultCountrySync() } returns null
        coEvery { repository.getCountryByIp(userWallet) } returns detected

        // Act
        val result = CheckOnrampAvailabilityUseCase(repository, errorResolver)(userWallet)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(OnrampAvailability.ConfirmResidency(detected))
        coVerify(exactly = 0) { repository.fetchCountries(any()) }
    }

    @Test
    fun `GIVEN saved country is unavailable WHEN check availability THEN returns not supported`() = runTest {
        // Arrange
        val saved = country(isAvailable = true)
        val current = saved.copy(isOnrampAvailable = false)
        coEvery { repository.getDefaultCountrySync() } returns saved
        coEvery { repository.fetchCountries(userWallet) } returns listOf(current)

        // Act
        val result = CheckOnrampAvailabilityUseCase(repository, errorResolver)(userWallet)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(OnrampAvailability.NotSupported(current))
    }

    @Test
    fun `GIVEN repository fails WHEN availability is checked THEN resolves error`() = runTest {
        // Arrange
        coEvery { repository.fetchPaymentMethodsIfAbsent(userWallet) } throws thrown

        // Act
        val result = CheckOnrampAvailabilityUseCase(repository, errorResolver)(userWallet)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(expectedError)
    }

    @Test
    fun `WHEN fetch hot crypto THEN delegates to repository`() {
        // Act
        FetchHotCryptoUseCase(hotCryptoRepository)()

        // Assert
        verify(exactly = 1) { hotCryptoRepository.fetchHotCrypto() }
    }

    @Test
    fun `WHEN get hot crypto THEN returns repository flow`() = runTest {
        // Arrange
        val expected = listOf(mockk<com.tangem.domain.onramp.model.HotCryptoCurrency>())
        every { hotCryptoRepository.getCurrencies(userWalletId) } returns flowOf(expected)

        // Act
        val result = GetHotCryptoUseCase(hotCryptoRepository)(userWalletId).toList()

        // Assert
        assertThat(result).containsExactly(expected)
    }

    @Test
    fun `WHEN clear cache THEN delegates to repository`() = runTest {
        // Act
        ClearOnrampCacheUseCase(repository)()

        // Assert
        coVerify { repository.clearCache() }
    }

    @Test
    fun `GIVEN successful fetch WHEN fetch countries and currencies THEN returns unit`() = runTest {
        // Arrange
        coEvery { repository.fetchCountries(userWallet) } returns emptyList()
        coEvery { repository.fetchCurrencies(userWallet) } returns Unit

        // Act
        val countries = FetchOnrampCountriesUseCase(repository, errorResolver)(userWallet)
        val currencies = FetchOnrampCurrenciesUseCase(repository, errorResolver)(userWallet)

        // Assert
        assertThat(countries.isRight()).isTrue()
        assertThat(currencies.isRight()).isTrue()
    }

    @Test
    fun `GIVEN fetch fails WHEN fetch countries THEN maps repository error`() = runTest {
        // Arrange
        coEvery { repository.fetchCountries(userWallet) } throws thrown

        // Act
        val result = FetchOnrampCountriesUseCase(repository, errorResolver)(userWallet)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(expectedError)
    }

    @Test
    fun `GIVEN no default country WHEN get country synchronously THEN loads country by ip from cache`() = runTest {
        // Arrange
        val country = country()
        coEvery { repository.getDefaultCountrySync() } returns null
        coEvery { repository.getCountryByIp(userWallet, fromCache = true) } returns country

        // Act
        val result = GetOnrampCountryUseCase(repository, errorResolver).invokeSync(userWallet)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(country)
    }

    @Test
    fun `GIVEN default country flow fails WHEN get country THEN emits resolved error`() = runTest {
        // Arrange
        every { repository.getDefaultCountry() } returns flow { throw thrown }

        // Act
        val result = GetOnrampCountryUseCase(repository, errorResolver)().toList()

        // Assert
        assertThat(result.single().leftOrNull()).isEqualTo(expectedError)
    }

    @Test
    fun `WHEN get currencies THEN partitions unique fiat currencies into popular and other`() = runTest {
        // Arrange
        val usd = currency("usd")
        val sek = currency("SEK")
        every { repository.getCurrencies() } returns flowOf(listOf(usd, usd, sek))

        // Act
        val result = GetOnrampCurrenciesUseCase(repository, errorResolver)().toList().single().getOrNull()!!

        // Assert
        assertThat(result.populars).containsExactly(usd)
        assertThat(result.others).containsExactly(sek)
    }

    @Test
    fun `WHEN get quotes THEN sorts payment methods by speed and quotes by amount`() = runTest {
        // Arrange
        val card = quote(PaymentMethodType.CARD, "10")
        val fast = quote(PaymentMethodType.REVOLUT_PAY, "5")
        val cardBest = quote(PaymentMethodType.CARD, "20")
        every { repository.getQuotes() } returns flowOf(listOf(card, fast, cardBest))
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        // Act
        val result = GetOnrampQuotesUseCase(settingsRepository, repository, errorResolver)().toList().single()

        // Assert
        assertThat(result.getOrNull()).containsExactly(fast, cardBest, card).inOrder()
    }

    @Test
    fun `GIVEN quote flow fails WHEN get quotes THEN emits resolved error`() = runTest {
        // Arrange
        every { repository.getQuotes() } returns flow { throw thrown }

        // Act
        val result = GetOnrampQuotesUseCase(settingsRepository, repository, errorResolver)().toList()

        // Assert
        assertThat(result.single().leftOrNull()).isEqualTo(expectedError)
    }

    @Test
    fun `WHEN get payment methods THEN sorts methods by configured speed priority`() = runTest {
        // Arrange
        val card = paymentMethod("card", PaymentMethodType.CARD)
        val revolut = paymentMethod("revolut", PaymentMethodType.REVOLUT_PAY)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { repository.getAvailablePaymentMethods() } returns setOf(card, revolut)

        // Act
        val result = GetOnrampPaymentMethodsUseCase(repository, settingsRepository, errorResolver)()

        // Assert
        assertThat(result.getOrNull()?.toList()).containsExactly(revolut, card).inOrder()
    }

    @Test
    fun `WHEN get status THEN delegates wallet and transaction id`() = runTest {
        // Arrange
        val status = mockk<OnrampStatus>()
        coEvery { repository.getStatus(userWallet, "tx") } returns status

        // Act
        val result = GetOnrampStatusUseCase(repository, errorResolver)(userWallet, "tx")

        // Assert
        assertThat(result.getOrNull()).isEqualTo(status)
        coVerify { repository.getStatus(userWallet, "tx") }
    }

    @Test
    fun `WHEN get default currency and save country currency THEN delegates and resolves values`() = runTest {
        // Arrange
        val country = country()
        val currency = country.defaultCurrency
        coEvery { repository.getDefaultCurrencySync() } returns currency

        // Act
        val result = OnrampGetDefaultCurrencyUseCase(repository, errorResolver)()
        val saveCountry = OnrampSaveDefaultCountryUseCase(repository, errorResolver)(country)
        val saveCurrency = OnrampSaveDefaultCurrencyUseCase(repository, errorResolver)(currency)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(currency)
        assertThat(saveCountry.isRight()).isTrue()
        assertThat(saveCurrency.isRight()).isTrue()
        coVerify { repository.saveDefaultCountry(country) }
        coVerify { repository.saveDefaultCurrency(currency) }
    }

    @Test
    fun `GIVEN missing default currency WHEN get default currency THEN maps error`() = runTest {
        // Arrange
        coEvery { repository.getDefaultCurrencySync() } returns null

        // Act
        val result = OnrampGetDefaultCurrencyUseCase(repository, errorResolver)()

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(expectedError)
        verify { errorResolver.resolve(any<Throwable>()) }
    }

    @Test
    fun `GIVEN default country and currency WHEN fetch pairs THEN forwards all values`() = runTest {
        // Arrange
        val country = country()
        val currency = country.defaultCurrency
        coEvery { repository.getDefaultCountrySync() } returns country
        coEvery { repository.getDefaultCurrencySync() } returns currency

        // Act
        val result = OnrampFetchPairsUseCase(repository, errorResolver)(userWallet, cryptoCurrency)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify { repository.fetchPairs(userWallet, currency, country, cryptoCurrency) }
    }

    @Test
    fun `WHEN fetch quotes THEN forwards wallet amount and currency`() = runTest {
        // Arrange
        val amount = mockk<Amount>()

        // Act
        val result = OnrampFetchQuotesUseCase(repository, errorResolver)(userWallet, amount, cryptoCurrency)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify { repository.fetchQuotes(userWallet, cryptoCurrency, amount) }
    }

    @Test
    fun `WHEN get transaction THEN returns unhandled transaction`() = runTest {
        // Arrange
        val transaction = mockk<OnrampTransaction>()
        coEvery { transactionRepository.isHandledTransaction("tx") } returns false
        coEvery { transactionRepository.getTransactionById("tx") } returns transaction

        // Act
        val result = GetOnrampTransactionUseCase(transactionRepository, errorResolver)("tx")

        // Assert
        assertThat(result.getOrNull()).isEqualTo(transaction)
    }

    @Test
    fun `GIVEN handled transaction WHEN get transaction THEN returns already handled`() = runTest {
        // Arrange
        coEvery { transactionRepository.isHandledTransaction("tx") } returns true

        // Act
        val result = GetOnrampTransactionUseCase(transactionRepository, errorResolver)("tx")

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(OnrampError.AlreadyHandledTransaction)
        coVerify(exactly = 0) { transactionRepository.getTransactionById(any()) }
    }

    @Test
    fun `WHEN get transactions THEN maps repository flow to right`() = runTest {
        // Arrange
        val transactions = listOf(mockk<OnrampTransaction>())
        every { transactionRepository.getTransactions(userWalletId, cryptoCurrencyId) } returns flowOf(transactions)

        // Act
        val result = GetOnrampTransactionsUseCase(transactionRepository, errorResolver)
            .invoke(userWalletId, cryptoCurrencyId).toList()

        // Assert
        assertThat(result.single().getOrNull()).isEqualTo(transactions)
    }

    @Test
    fun `GIVEN null id WHEN remove transaction THEN returns domain error without repository calls`() = runTest {
        // Act
        val result = OnrampRemoveTransactionUseCase(transactionRepository, errorResolver)(null)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(OnrampError.DomainError("Transaction id not provided"))
        coVerify(exactly = 0) { transactionRepository.removeTransaction(any()) }
    }

    @Test
    fun `GIVEN normal removal WHEN remove transaction THEN marks handled and removes`() = runTest {
        // Act
        val result = OnrampRemoveTransactionUseCase(transactionRepository, errorResolver)("tx")

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify { transactionRepository.storeHandledTransaction("tx") }
        coVerify { transactionRepository.removeTransaction("tx") }
    }

    @Test
    fun `GIVEN force removal WHEN remove transaction THEN skips handled marker`() = runTest {
        // Act
        OnrampRemoveTransactionUseCase(transactionRepository, errorResolver)("tx", forceRemove = true)

        // Assert
        coVerify(exactly = 0) { transactionRepository.storeHandledTransaction(any()) }
        coVerify { transactionRepository.removeTransaction("tx") }
    }

    @Test
    fun `WHEN save transaction and update status THEN forwards repository values`() = runTest {
        // Arrange
        val transaction = mockk<OnrampTransaction>()
        val status = OnrampStatus.Status.Paid

        // Act
        val save = OnrampSaveTransactionUseCase(transactionRepository, errorResolver)(transaction)
        val update = OnrampUpdateTransactionStatusUseCase(transactionRepository, errorResolver)(
            txId = "tx",
            externalTxId = "external",
            externalTxUrl = "https://example.com",
            status = status,
        )

        // Assert
        assertThat(save.isRight()).isTrue()
        assertThat(update.isRight()).isTrue()
        coVerify { transactionRepository.storeTransaction(transaction) }
        coVerify { transactionRepository.updateTransactionStatus("tx", "external", "https://example.com", status) }
    }

    @Test
    fun `WHEN get legacy top up url with address THEN delegates currency and address`() = runTest {
        // Arrange
        val currency = cryptoCurrency
        val status = mockk<CryptoCurrencyStatus>(relaxed = true)
        every { status.currency } returns currency
        every { status.value.networkAddress?.defaultAddress?.value } returns "address"
        coEvery { legacyTopUpRepository.getTopUpUrl(currency, "address") } returns "https://example.com"

        // Act
        val result = GetLegacyTopUpUrlUseCase(legacyTopUpRepository)(status)

        // Assert
        assertThat(result.getOrNull()).isEqualTo("https://example.com")
    }

    private fun country(isAvailable: Boolean = true) = OnrampCountry(
        id = "country",
        name = "Country",
        code = "CC",
        image = "",
        alpha3 = "CCC",
        continent = "Europe",
        defaultCurrency = currency("USD"),
        isOnrampAvailable = isAvailable,
    )

    private fun currency(code: String) = OnrampCurrency(
        name = code,
        code = code,
        image = null,
        precision = 2,
        unit = code,
    )

    private fun paymentMethod(id: String, type: PaymentMethodType) = com.tangem.domain.onramp.model.OnrampPaymentMethod(
        id = id,
        name = id,
        imageUrl = "",
        type = type,
    )

    private fun quote(type: PaymentMethodType, amount: String) = com.tangem.domain.onramp.model.OnrampQuote.Data(
        paymentMethod = paymentMethod(type.name, type),
        provider = mockk(relaxed = true),
        fromAmount = mockk { every { value } returns BigDecimal.TEN },
        countryCode = "CC",
        toAmount = mockk { every { value } returns BigDecimal(amount) },
        minFromAmount = null,
        maxFromAmount = null,
    )
}