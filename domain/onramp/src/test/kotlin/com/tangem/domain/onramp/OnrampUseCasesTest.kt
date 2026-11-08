package com.tangem.domain.onramp

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.HotCryptoRepository
import com.tangem.domain.onramp.repositories.LegacyTopUpRepository
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.tokens.model.Amount
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CheckOnrampAvailability {

        @Test
        fun `GIVEN supported country without currency WHEN checking availability THEN saves default currency`() = runTest {
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
        fun `GIVEN saved currency WHEN checking availability THEN preserves user preference`() = runTest {
            // Arrange
            val country = country()
            val savedCurrency = currency("SEK")
            coEvery { repository.getDefaultCountrySync() } returns country
            coEvery { repository.fetchCountries(userWallet) } returns emptyList()
            coEvery { repository.getDefaultCurrencySync() } returns savedCurrency

            // Act
            val result = CheckOnrampAvailabilityUseCase(repository, errorResolver)(userWallet)

            // Assert
            assertThat(result).isEqualTo(Either.Right(OnrampAvailability.Available(country, savedCurrency)))
            coVerify(exactly = 0) { repository.saveDefaultCurrency(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchHotCrypto {

        @Test
        fun `GIVEN repository data WHEN fetch hot crypto THEN delegates to repository`() {
            // Act
            FetchHotCryptoUseCase(hotCryptoRepository)()

            // Assert
            verify(exactly = 1) { hotCryptoRepository.fetchHotCrypto() }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetHotCrypto {

        @Test
        fun `GIVEN repository data WHEN get hot crypto THEN returns repository flow`() = runTest {
            // Arrange
            val expected = listOf(mockk<com.tangem.domain.onramp.model.HotCryptoCurrency>())
            every { hotCryptoRepository.getCurrencies(userWalletId) } returns flowOf(expected)

            // Act
            val result = GetHotCryptoUseCase(hotCryptoRepository)(userWalletId).toList()

            // Assert
            assertThat(result).containsExactly(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClearOnrampCache {

        @Test
        fun `GIVEN repository data WHEN clear cache THEN delegates to repository`() = runTest {
            // Act
            ClearOnrampCacheUseCase(repository)()

            // Assert
            coVerify { repository.clearCache() }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchOnrampCountries {

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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampCountry {

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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampCurrencies {

        @Test
        fun `GIVEN repository data WHEN get currencies THEN partitions unique fiat currencies into popular and other`() = runTest {
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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampQuotes {

        @Test
        fun `GIVEN repository data WHEN get quotes THEN sorts payment methods by speed and quotes by amount`() = runTest {
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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampPaymentMethods {

        @Test
        fun `GIVEN repository data WHEN get payment methods THEN sorts methods by configured speed priority`() = runTest {
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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampStatus {

        @Test
        fun `GIVEN repository data WHEN get status THEN delegates wallet and transaction id`() = runTest {
            // Arrange
            val status = mockk<OnrampStatus>()
            coEvery { repository.getStatus(userWallet, "tx") } returns status

            // Act
            val result = GetOnrampStatusUseCase(repository, errorResolver)(userWallet, "tx")

            // Assert
            assertThat(result.getOrNull()).isEqualTo(status)
            coVerify { repository.getStatus(userWallet, "tx") }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnrampGetDefaultCurrency {

        @Test
        fun `GIVEN repository data WHEN get default currency and save country currency THEN delegates and resolves values`() = runTest {
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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnrampFetchPairs {

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
        fun `GIVEN missing country WHEN fetching pairs THEN does not fetch pairs`() = runTest {
            // Arrange
            coEvery { repository.getDefaultCountrySync() } returns null

            // Act
            val result = OnrampFetchPairsUseCase(repository, errorResolver)(userWallet, cryptoCurrency)

            // Assert
            assertThat(result).isEqualTo(Either.Left(expectedError))
            coVerify(exactly = 0) { repository.fetchPairs(any(), any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnrampFetchQuotes {

        @Test
        fun `GIVEN repository data WHEN fetch quotes THEN forwards wallet amount and currency`() = runTest {
            // Arrange
            val amount = mockk<Amount>()

            // Act
            val result = OnrampFetchQuotesUseCase(repository, errorResolver)(userWallet, amount, cryptoCurrency)

            // Assert
            assertThat(result.isRight()).isTrue()
            coVerify { repository.fetchQuotes(userWallet, cryptoCurrency, amount) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampTransaction {

        @Test
        fun `GIVEN repository data WHEN get transaction THEN returns unhandled transaction`() = runTest {
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
        fun `GIVEN missing transaction WHEN retrieving transaction THEN maps validation error`() = runTest {
            // Arrange
            coEvery { transactionRepository.isHandledTransaction("missing") } returns false
            coEvery { transactionRepository.getTransactionById("missing") } returns null

            // Act
            val result = GetOnrampTransactionUseCase(transactionRepository, errorResolver)("missing")

            // Assert
            assertThat(result).isEqualTo(Either.Left(expectedError))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampTransactions {

        @Test
        fun `GIVEN repository data WHEN get transactions THEN maps repository flow to right`() = runTest {
            // Arrange
            val transactions = listOf(mockk<OnrampTransaction>())
            every { transactionRepository.getTransactions(userWalletId, cryptoCurrencyId) } returns flowOf(transactions)

            // Act
            val result = GetOnrampTransactionsUseCase(transactionRepository, errorResolver)
                .invoke(userWalletId, cryptoCurrencyId).toList()

            // Assert
            assertThat(result.single().getOrNull()).isEqualTo(transactions)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnrampRemoveTransaction {

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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnrampSaveTransaction {

        @Test
        fun `GIVEN repository data WHEN save transaction and update status THEN forwards repository values`() = runTest {
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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetLegacyTopUpUrl {

        @Test
        fun `GIVEN repository data WHEN get legacy top up url with address THEN delegates currency and address`() = runTest {
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

        @Test
        fun `GIVEN no address WHEN getting legacy URL THEN returns domain error without requesting URL`() = runTest {
            // Arrange
            val status = mockk<CryptoCurrencyStatus>(relaxed = true)
            every { status.value.networkAddress } returns null

            // Act
            val result = GetLegacyTopUpUrlUseCase(legacyTopUpRepository)(status)

            // Assert
            assertThat(result).isEqualTo(Either.Left(OnrampError.DomainError("Wallet address is null")))
            coVerify(exactly = 0) { legacyTopUpRepository.getTopUpUrl(any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FlowErrors {

        @ParameterizedTest
        @ValueSource(strings = ["countries", "currencies", "transactions", "allOffers", "offers"])
        fun `GIVEN success then upstream failure WHEN collecting THEN success precedes mapped error`(kind: String) =
            runTest {
                // Arrange
                every { repository.getCountries() } returns flow { emit(emptyList()); throw thrown }
                every { repository.getCurrencies() } returns flow { emit(emptyList()); throw thrown }
                val failQuotes = CompletableDeferred<Unit>()
                every { repository.getQuotes() } returns flow {
                    emit(emptyList())
                    failQuotes.await()
                    throw thrown
                }
                every { transactionRepository.getTransactions(userWalletId, cryptoCurrencyId) } returns flow {
                    emit(emptyList())
                    throw thrown
                }
                every { transactionRepository.getAllTransactions() } returns flowOf(emptyList())
                val collected = mutableListOf<Either<OnrampError, *>>()

                // Act
                val job = launch { errorFlow(kind).toList(collected) }
                runCurrent()
                failQuotes.complete(Unit)
                job.join()

                // Assert
                assertThat(collected).hasSize(2)
                assertThat(collected.first().isRight()).isTrue()
                assertThat(collected.last()).isEqualTo(Either.Left(expectedError))
                verify(exactly = 1) {
                    errorResolver.resolve(match { it is IllegalStateException && it.message == thrown.message })
                }
            }

        @Test
        fun `GIVEN transaction source fails WHEN offers collected THEN error is mapped`() = runTest {
            // Arrange
            every { repository.getQuotes() } returns flowOf(emptyList())
            every { transactionRepository.getAllTransactions() } returns flow { throw thrown }

            // Act
            val result = errorFlow("offers").toList()

            // Assert
            assertThat(result).containsExactly(Either.Left(expectedError))
            verify(exactly = 1) {
                errorResolver.resolve(match { it is IllegalStateException && it.message == thrown.message })
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["allOffers", "offers"])
        fun `GIVEN settings fail during processing WHEN offers collected THEN error is mapped`(kind: String) = runTest {
            // Arrange
            every { repository.getQuotes() } returns flowOf(listOf(quote(PaymentMethodType.CARD, "10")))
            every { transactionRepository.getAllTransactions() } returns flowOf(emptyList())
            coEvery { settingsRepository.isGooglePayAvailability() } throws thrown

            // Act
            val result = errorFlow(kind).toList()

            // Assert
            assertThat(result).containsExactly(Either.Left(expectedError))
            verify(exactly = 1) {
                errorResolver.resolve(match { it is IllegalStateException && it.message == thrown.message })
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["countries", "currencies", "transactions", "allOffers", "offers"])
        fun `GIVEN collector throws WHEN collecting domain flow THEN downstream failure is not mapped`(kind: String) =
            runTest {
                // Arrange
                every { repository.getCountries() } returns flowOf(emptyList())
                every { repository.getCurrencies() } returns flowOf(emptyList())
                every { repository.getQuotes() } returns flowOf(emptyList())
                every { transactionRepository.getTransactions(userWalletId, cryptoCurrencyId) } returns
                    flowOf(emptyList())
                every { transactionRepository.getAllTransactions() } returns flowOf(emptyList())

                // Act
                val actual = runCatching { errorFlow(kind).collect { throw thrown } }.exceptionOrNull()

                // Assert
                assertThat(actual).isInstanceOf(IllegalStateException::class.java)
                assertThat(actual).hasMessageThat().isEqualTo(thrown.message)
                verify(exactly = 0) { errorResolver.resolve(any()) }
            }

        @ParameterizedTest
        @ValueSource(strings = ["countries", "currencies", "transactions", "allOffers", "offers"])
        fun `GIVEN upstream failure WHEN collecting domain flow THEN emits mapped error`(kind: String) = runTest {
            // Arrange
            every { repository.getCountries() } returns flow { throw thrown }
            every { repository.getCurrencies() } returns flow { throw thrown }
            every { repository.getQuotes() } returns flow { throw thrown }
            every { transactionRepository.getTransactions(userWalletId, cryptoCurrencyId) } returns flow { throw thrown }
            every { transactionRepository.getAllTransactions() } returns flowOf(emptyList())

            // Act
            val result = errorFlow(kind).toList()

            // Assert
            assertThat(result).containsExactly(Either.Left(expectedError))
            verify(exactly = 1) {
                errorResolver.resolve(match { it is IllegalStateException && it.message == thrown.message })
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["countries", "currencies", "transactions", "allOffers", "offers"])
        fun `GIVEN cancelled source WHEN collecting domain flow THEN cancellation is not mapped`(kind: String) = runTest {
            // Arrange
            every { repository.getCountries() } returns flow { awaitCancellation() }
            every { repository.getCurrencies() } returns flow { awaitCancellation() }
            every { repository.getQuotes() } returns flow { awaitCancellation() }
            every { transactionRepository.getTransactions(userWalletId, cryptoCurrencyId) } returns flow {
                awaitCancellation()
            }
            every { transactionRepository.getAllTransactions() } returns flowOf(emptyList())
            val collected = mutableListOf<Either<OnrampError, *>>()

            // Act
            val job = launch { errorFlow(kind).toList(collected) }
            runCurrent()
            job.cancelAndJoin()

            // Assert
            assertThat(collected).isEmpty()
            verify(exactly = 0) { errorResolver.resolve(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampCountries {

        @Test
        fun `GIVEN country updates WHEN collecting countries THEN preserves emission order`() = runTest {
            // Arrange
            val countries = listOf(country())
            every { repository.getCountries() } returns flowOf(emptyList(), countries)

            // Act
            val result = GetOnrampCountriesUseCase(repository, errorResolver)().toList()

            // Assert
            assertThat(result).containsExactly(Either.Right(emptyList<OnrampCountry>()), Either.Right(countries)).inOrder()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOnrampRedirectUrl {

        @Test
        fun `GIVEN redirect data WHEN requesting redirect THEN stores transaction before returning it`() = runTest {
            // Arrange
            val quote = mockk<com.tangem.domain.onramp.model.OnrampProviderWithQuote.Data>()
            val transaction = mockk<OnrampTransaction>()
            coEvery { repository.getOnrampData(userWallet, cryptoCurrency, quote, true) } returns transaction
            val releaseStorage = CompletableDeferred<Unit>()
            coEvery { transactionRepository.storeTransaction(transaction) } coAnswers { releaseStorage.await() }

            // Act
            val result = async {
                GetOnrampRedirectUrlUseCase(repository, transactionRepository, errorResolver)(
                    userWallet, quote, cryptoCurrency, true,
                )
            }
            runCurrent()

            // Assert
            assertThat(result.isCompleted).isFalse()
            coVerify(exactly = 1) { transactionRepository.storeTransaction(transaction) }

            // Act
            releaseStorage.complete(Unit)

            // Assert
            assertThat(result.await()).isEqualTo(Either.Right(transaction))
        }

        @Test
        fun `GIVEN redirect data fails WHEN requesting redirect THEN nothing is persisted`() = runTest {
            // Arrange
            val quote = mockk<com.tangem.domain.onramp.model.OnrampProviderWithQuote.Data>()
            coEvery { repository.getOnrampData(userWallet, cryptoCurrency, quote, false) } throws thrown

            // Act
            val result = GetOnrampRedirectUrlUseCase(repository, transactionRepository, errorResolver)(
                userWallet, quote, cryptoCurrency, false,
            )

            // Assert
            assertThat(result).isEqualTo(Either.Left(expectedError))
            coVerify(exactly = 0) { transactionRepository.storeTransaction(any()) }
            verify(exactly = 1) { errorResolver.resolve(thrown) }
        }

        @Test
        fun `GIVEN storing redirect fails WHEN requesting redirect THEN returns mapped error`() = runTest {
            // Arrange
            val quote = mockk<com.tangem.domain.onramp.model.OnrampProviderWithQuote.Data>()
            val transaction = mockk<OnrampTransaction>()
            coEvery { repository.getOnrampData(userWallet, cryptoCurrency, quote, false) } returns transaction
            coEvery { transactionRepository.storeTransaction(transaction) } throws thrown

            // Act
            val result = GetOnrampRedirectUrlUseCase(repository, transactionRepository, errorResolver)(
                userWallet, quote, cryptoCurrency, false,
            )

            // Assert
            assertThat(result).isEqualTo(Either.Left(expectedError))
            verify(exactly = 1) { errorResolver.resolve(thrown) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RepositoryErrors {

        @ParameterizedTest
        @ValueSource(
            strings = [
                "fetchCurrencies", "status", "paymentMethods", "saveCountry", "saveCurrency",
                "fetchPairs", "fetchQuotes", "saveTransaction", "removeTransaction", "updateTransaction",
            ],
        )
        fun `GIVEN repository failure WHEN invoking use case THEN maps original exception`(kind: String) = runTest {
            // Arrange
            val country = country()
            coEvery { repository.fetchCurrencies(userWallet) } throws thrown
            coEvery { repository.getStatus(userWallet, "tx") } throws thrown
            coEvery { settingsRepository.isGooglePayAvailability() } throws thrown
            coEvery { repository.saveDefaultCountry(any()) } throws thrown
            coEvery { repository.saveDefaultCurrency(any()) } throws thrown
            coEvery { repository.getDefaultCountrySync() } returns country
            coEvery { repository.getDefaultCurrencySync() } returns country.defaultCurrency
            coEvery { repository.fetchPairs(any(), any(), any(), any()) } throws thrown
            coEvery { repository.fetchQuotes(any(), any(), any()) } throws thrown
            coEvery { transactionRepository.storeTransaction(any()) } throws thrown
            coEvery { transactionRepository.removeTransaction(any()) } throws thrown
            coEvery { transactionRepository.updateTransactionStatus(any(), any(), any(), any()) } throws thrown

            // Act
            val result = when (kind) {
                "fetchCurrencies" -> FetchOnrampCurrenciesUseCase(repository, errorResolver)(userWallet)
                "status" -> GetOnrampStatusUseCase(repository, errorResolver)(userWallet, "tx")
                "paymentMethods" -> GetOnrampPaymentMethodsUseCase(repository, settingsRepository, errorResolver)()
                "saveCountry" -> OnrampSaveDefaultCountryUseCase(repository, errorResolver)(country)
                "saveCurrency" -> OnrampSaveDefaultCurrencyUseCase(repository, errorResolver)(country.defaultCurrency)
                "fetchPairs" -> OnrampFetchPairsUseCase(repository, errorResolver)(userWallet, cryptoCurrency)
                "fetchQuotes" -> OnrampFetchQuotesUseCase(repository, errorResolver)(userWallet, mockk(), cryptoCurrency)
                "saveTransaction" -> OnrampSaveTransactionUseCase(transactionRepository, errorResolver)(mockk())
                "removeTransaction" -> OnrampRemoveTransactionUseCase(transactionRepository, errorResolver)("tx")
                "updateTransaction" -> OnrampUpdateTransactionStatusUseCase(transactionRepository, errorResolver)(
                    "tx", "external", "url", OnrampStatus.Status.Paid,
                )
                else -> error("Unknown use case: $kind")
            }

            // Assert
            assertThat(result).isEqualTo(Either.Left(expectedError))
            verify(exactly = 1) { errorResolver.resolve(thrown) }
        }
    }

    private fun errorFlow(kind: String): kotlinx.coroutines.flow.Flow<Either<OnrampError, *>> = when (kind) {
        "countries" -> GetOnrampCountriesUseCase(repository, errorResolver)()
        "currencies" -> GetOnrampCurrenciesUseCase(repository, errorResolver)()
        "transactions" -> GetOnrampTransactionsUseCase(transactionRepository, errorResolver)(
            userWalletId, cryptoCurrencyId,
        )
        "allOffers" -> GetOnrampAllOffersUseCase(repository, errorResolver, settingsRepository)(
            userWalletId, cryptoCurrencyId,
        )
        "offers" -> GetOnrampOffersUseCase(repository, transactionRepository, errorResolver, settingsRepository)()
        else -> error("Unknown flow: $kind")
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