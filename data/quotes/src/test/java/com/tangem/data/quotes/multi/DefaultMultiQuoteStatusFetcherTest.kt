package com.tangem.data.quotes.multi

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.common.test.utils.assertEither
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.data.quotes.store.setSourceAsCache
import com.tangem.data.quotes.store.setSourceAsOnlyCache
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMultiQuoteStatusFetcherTest {

    private val quotesFetcher = mockk<QuotesFetcher>()
    private val appCurrencyResponseStore = mockk<AppCurrencyResponseStore>()
    private val quotesStore = mockk<QuotesStatusesStore>(relaxed = true)

    private val fetcher = DefaultMultiQuoteStatusFetcher(
        quotesFetcher = quotesFetcher,
        appCurrencyResponseStore = appCurrencyResponseStore,
        quotesStatusesStore = quotesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(quotesFetcher, appCurrencyResponseStore, quotesStore)
    }

    @Test
    fun `fetch successfully`() = runTest {
        // Arrange
        val params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns usdAppCurrency

        val currenciesIds = setOf("BTC", "ETH")

        coEvery {
            quotesFetcher.fetch(fiatCurrencyId = "usd", currenciesIds = currenciesIds, fields = fields)
        } returns successResponse.right()

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = params.currenciesIds)
            appCurrencyResponseStore.getSyncOrNull()
            quotesFetcher.fetch(fiatCurrencyId = "usd", currenciesIds = currenciesIds, fields = fields)
            quotesStore.store(values = successResponse.quotes)
        }

        coVerify(inverse = true) {
            quotesStore.setSourceAsOnlyCache(currenciesIds = any())
        }
    }

    @Test
    fun `fetch successfully if currenciesIds from params is empty`() = runTest {
        // Arrange
        val params = MultiQuoteStatusFetcher.Params(currenciesIds = emptySet(), appCurrencyId = null)

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify(inverse = true) {
            quotesStore.setSourceAsCache(currenciesIds = any())
            appCurrencyResponseStore.getSyncOrNull()
            quotesFetcher.fetch(fiatCurrencyId = any(), currenciesIds = any(), fields = any())
            quotesStore.store(values = any())
            quotesStore.setSourceAsOnlyCache(currenciesIds = any())
        }
    }

    @Test
    fun `fetch successfully if appCurrencyId from params is not null`() = runTest {
        // Arrange
        val appCurrencyId = "usd"
        val params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = appCurrencyId)

        val currenciesIds = setOf("BTC", "ETH")

        coEvery {
            quotesFetcher.fetch(fiatCurrencyId = appCurrencyId, currenciesIds = currenciesIds, fields = fields)
        } returns successResponse.right()

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = params.currenciesIds)
            quotesFetcher.fetch(fiatCurrencyId = appCurrencyId, currenciesIds = currenciesIds, fields = fields)
            quotesStore.store(values = successResponse.quotes)
        }

        coVerify(inverse = true) {
            appCurrencyResponseStore.getSyncOrNull()
            quotesStore.setSourceAsOnlyCache(currenciesIds = any())
        }
    }

    @Test
    fun `fetch failure because appCurrencyId from params is blank`() = runTest {
        // Arrange
        val appCurrencyId = ""
        val params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = appCurrencyId)

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = IllegalStateException("Unable to get AppCurrency for updating quotes").left()

        assertEither(actual, expected)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = params.currenciesIds)
            quotesStore.setSourceAsOnlyCache(currenciesIds = params.currenciesIds)
        }

        coVerify(inverse = true) {
            appCurrencyResponseStore.getSyncOrNull()
            quotesFetcher.fetch(fiatCurrencyId = any(), currenciesIds = any(), fields = any())
            quotesStore.store(values = any())
        }
    }

    @Test
    fun `fetch failure because api request failed`() = runTest {
        // Arrange
        val params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null)

        val currenciesIds = setOf("BTC", "ETH")
        val error = QuotesFetcher.Error.ApiOperationError(ApiResponseError.NetworkException)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns usdAppCurrency

        coEvery {
            quotesFetcher.fetch(fiatCurrencyId = "usd", currenciesIds = currenciesIds, fields = fields)
        } returns error.left()

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = IllegalStateException("Cause: ApiOperationError(apiError=NetworkException)").left()
        assertEither(actual, expected)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = params.currenciesIds)
            appCurrencyResponseStore.getSyncOrNull()
            quotesFetcher.fetch(fiatCurrencyId = "usd", currenciesIds = currenciesIds, fields = fields)
            quotesStore.setSourceAsOnlyCache(currenciesIds = params.currenciesIds)
        }

        coVerify(inverse = true) {
            quotesStore.store(values = any())
        }
    }

    @Test
    fun `fetch failure because app currency not found`() = runTest {
        // Arrange
        val params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns null

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = IllegalStateException("Unable to get AppCurrency for updating quotes").left()

        assertEither(actual, expected)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = params.currenciesIds)
            appCurrencyResponseStore.getSyncOrNull()
            quotesStore.setSourceAsOnlyCache(currenciesIds = params.currenciesIds)
        }

        coVerify(inverse = true) {
            quotesFetcher.fetch(fiatCurrencyId = any(), currenciesIds = any(), fields = any())
            quotesStore.store(values = any())
        }
    }

    @Test
    fun `fetch successfully if not all quotes are fetched`() = runTest {
        // Arrange
        val params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns usdAppCurrency

        val currenciesIds = setOf("BTC", "ETH")
        val response = QuotesResponse(
            quotes = mapOf(
                "BTC" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE),
            ),
        )

        coEvery {
            quotesFetcher.fetch(fiatCurrencyId = "usd", currenciesIds = currenciesIds, fields = fields)
        } returns response.right()

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        val storedQuotes = QuotesResponse(
            quotes = mapOf(
                "BTC" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE),
                "ETH" to QuotesResponse.Quote.EMPTY,
            ),
        )

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = params.currenciesIds)
            appCurrencyResponseStore.getSyncOrNull()
            quotesFetcher.fetch(fiatCurrencyId = "usd", currenciesIds = currenciesIds, fields = fields)
            quotesStore.store(values = storedQuotes.quotes)
        }

        coVerify(inverse = true) {
            quotesStore.setSourceAsOnlyCache(currenciesIds = any())
        }
    }

    private companion object {

        val currenciesIds = setOf(
            CryptoCurrency.RawID(value = "BTC"),
            CryptoCurrency.RawID(value = "ETH"),
        )

        val usdAppCurrency = CurrenciesResponse.Currency(
            id = "USD".lowercase(),
            code = "USD",
            name = "US Dollar",
            unit = "$",
            type = "fiat",
            rateBTC = "",
        )

        val successResponse = QuotesResponse(
            quotes = mapOf(
                "BTC" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE),
                "ETH" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.TEN),
            ),
        )

        val fields = setOf(QuotesFetcher.Field.PRICE, QuotesFetcher.Field.PRICE_CHANGE_24H)
    }
}