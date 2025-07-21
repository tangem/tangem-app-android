package com.tangem.domain.tokens.wallet

import arrow.core.left
import arrow.core.right
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.assertEither
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.wallet.FetchingSource.*
import com.tangem.domain.tokens.wallet.implementor.MultiWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.implementor.SingleWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.implementor.SingleWalletWithTokenBalanceFetcher
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletBalanceFetcherTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private val currenciesRepository: CurrenciesRepository = mockk()
    private val multiWalletBalanceFetcher: MultiWalletBalanceFetcher = mockk()
    private val singleWalletWithTokenBalanceFetcher: SingleWalletWithTokenBalanceFetcher = mockk()
    private val singleWalletBalanceFetcher: SingleWalletBalanceFetcher = mockk()
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher = mockk()
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher = mockk()
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher = mockk()

    private val fetcher = WalletBalanceFetcher(
        currenciesRepository = currenciesRepository,
        multiWalletBalanceFetcher = multiWalletBalanceFetcher,
        singleWalletWithTokenBalanceFetcher = singleWalletWithTokenBalanceFetcher,
        singleWalletBalanceFetcher = singleWalletBalanceFetcher,
        multiNetworkStatusFetcher = multiNetworkStatusFetcher,
        multiQuoteStatusFetcher = multiQuoteStatusFetcher,
        multiYieldBalanceFetcher = multiYieldBalanceFetcher,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            currenciesRepository,
            multiWalletBalanceFetcher,
            singleWalletWithTokenBalanceFetcher,
            singleWalletBalanceFetcher,
            multiNetworkStatusFetcher,
            multiQuoteStatusFetcher,
            multiYieldBalanceFetcher,
        )
    }

    @Test
    fun `fetch failure if getCardTypesResolver THROWS EXCEPTION`() = runTest {
        // Arrange
        val exception = IllegalStateException("Error")
        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } throws exception

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = exception.left()
        assertEither(actual, expected)

        verifyOrder { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) }

        coVerify(inverse = true) {
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            multiYieldBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if getCardTypesResolver cannot resolve wallet type`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns false
            every { isSingleWalletWithToken() } returns false
            every { isSingleWallet() } returns false
        }

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = IllegalStateException("Unknown type of wallet: $userWalletId").left()
        assertEither(actual, expected)

        verifyOrder { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) }

        coVerify(inverse = true) {
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            multiYieldBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if getCryptoCurrencies THROWS EXCEPTION`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val exception = IllegalStateException("Error")

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId) } throws exception

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = exception.left()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            multiYieldBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if getCryptoCurrencies RETURNS EMPTY SET`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId) } returns emptySet()

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = IllegalStateException("UserWallet doesn't contain crypto-currencies: $userWalletId").left()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            multiYieldBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if fetchNetworks RETURNS LEFT`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val exception = IllegalStateException("Error")

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId) } returns currencies
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(NETWORK)
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns exception.left()

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = IllegalStateException(
            "Failed to fetch next sources for UserWalletId(011...011):\n" +
                "NETWORK – java.lang.IllegalStateException: Error",
        ).left()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
            multiWalletBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiQuoteStatusFetcher(params = any())
            multiYieldBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if fetchQuotes RETURNS LEFT`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        val exception = IllegalStateException("Error")

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId) } returns currencies
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(QUOTE)
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns exception.left()

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = IllegalStateException(
            "Failed to fetch next sources for UserWalletId(011...011):\n" +
                "QUOTE – java.lang.IllegalStateException: Error",
        ).left()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
            multiWalletBalanceFetcher.fetchingSources
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiNetworkStatusFetcher(params = any())
            multiYieldBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if fetchStaking RETURNS LEFT`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val yieldBalanceFetcherParams = MultiYieldBalanceFetcher.Params(
            userWalletId = userWalletId,
            currencyIdWithNetworkMap = currencies.associateTo(hashMapOf()) { it.id to it.network },
        )

        val exception = IllegalStateException("Error")

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId) } returns currencies
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(STAKING)
        coEvery { multiYieldBalanceFetcher(params = yieldBalanceFetcherParams) } returns exception.left()

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = IllegalStateException(
            "Failed to fetch next sources for UserWalletId(011...011):\n" +
                "STAKING – java.lang.IllegalStateException: Error",
        ).left()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
            multiWalletBalanceFetcher.fetchingSources
            multiYieldBalanceFetcher(params = yieldBalanceFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if all fetching sources RETURNS LEFT`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        val yieldBalanceFetcherParams = MultiYieldBalanceFetcher.Params(
            userWalletId = userWalletId,
            currencyIdWithNetworkMap = currencies.associateTo(hashMapOf()) { it.id to it.network },
        )

        val exception = IllegalStateException("Error")

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId) } returns currencies
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(NETWORK, QUOTE, STAKING)
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns exception.left()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns exception.left()
        coEvery { multiYieldBalanceFetcher(params = yieldBalanceFetcherParams) } returns exception.left()

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = IllegalStateException(
            "Failed to fetch next sources for UserWalletId(011...011):\n" +
                "NETWORK – java.lang.IllegalStateException: Error\n" +
                "QUOTE – java.lang.IllegalStateException: Error\n" +
                "STAKING – java.lang.IllegalStateException: Error",
        ).left()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
            multiWalletBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
            multiYieldBalanceFetcher(params = yieldBalanceFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
        }
    }

    @Test
    fun `fetch successfully for multi-currency wallet`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        val yieldBalanceFetcherParams = MultiYieldBalanceFetcher.Params(
            userWalletId = userWalletId,
            currencyIdWithNetworkMap = currencies.associateTo(hashMapOf()) { it.id to it.network },
        )

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId) } returns currencies
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(NETWORK, QUOTE, STAKING)
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns Unit.right()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns Unit.right()
        coEvery { multiYieldBalanceFetcher(params = yieldBalanceFetcherParams) } returns Unit.right()

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
            multiWalletBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
            multiYieldBalanceFetcher(params = yieldBalanceFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
        }
    }

    @Test
    fun `fetch successfully for single-currency with token wallet`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns false
            every { isSingleWalletWithToken() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
        } returns currencies
        every { singleWalletWithTokenBalanceFetcher.fetchingSources } returns setOf(NETWORK, QUOTE)
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns Unit.right()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns Unit.right()

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
            singleWalletWithTokenBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
        }

        coVerify(inverse = true) {
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiYieldBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch successfully for single-currency wallet`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns false
            every { isSingleWalletWithToken() } returns false
            every { isSingleWallet() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        every { currenciesRepository.getCardTypesResolver(userWalletId = userWalletId) } returns cardTypesResolver
        coEvery { singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId) } returns currencies
        every { singleWalletBalanceFetcher.fetchingSources } returns setOf(NETWORK, QUOTE)
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns Unit.right()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns Unit.right()

        // Act
        val actual = fetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)
            singleWalletBalanceFetcher.getCryptoCurrencies(userWalletId = userWalletId)
            singleWalletBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
        }

        coVerify(inverse = true) {
            multiWalletBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWalletId = any())
            multiYieldBalanceFetcher(params = any())
        }
    }

    private companion object {

        val userWalletId = UserWalletId("011")
    }
}