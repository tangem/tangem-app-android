package com.tangem.data.common.quote

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.data.common.quote.DefaultQuotesFetcher.QuoteMetadata
import com.tangem.data.common.quote.QuotesFetcher.Field
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultQuotesFetcherTest {

    private val tangemTechApi = mockk<TangemTechApi>()
    private val fetcher = DefaultQuotesFetcher(
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemTechApi)
        fetcher.clearCache()
    }

    @Test
    fun `fetch if fiatCurrencyId is EMPTY`() = runTest {
        // Act
        val actual = fetcher.fetch(
            fiatCurrencyId = "",
            currenciesIds = setOf("ethereum"),
            fields = setOf(Field.PRICE),
        )

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected = QuotesFetcher.Error.InvalidArgumentsError.left()
        Truth.assertThat(actual).isEqualTo(expected)

        val expectedCacheData = ConcurrentHashMap<String, Set<QuoteMetadata>>()
        Truth.assertThat(actualCacheData).isEqualTo(expectedCacheData)

        coVerify(inverse = true) { tangemTechApi.getQuotes(currencyId = any(), coinIds = any(), fields = any()) }
    }

    @Test
    fun `fetch if currenciesIds is EMPTY`() = runTest {
        // Act
        val actual = fetcher.fetch(
            fiatCurrencyId = "usd",
            currenciesIds = emptySet(),
            fields = setOf(Field.PRICE),
        )

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected = QuotesResponse(quotes = emptyMap()).right()
        Truth.assertThat(actual).isEqualTo(expected)

        val expectedCacheData = ConcurrentHashMap<String, Set<QuoteMetadata>>()
        Truth.assertThat(actualCacheData).isEqualTo(expectedCacheData)

        coVerify(inverse = true) { tangemTechApi.getQuotes(currencyId = any(), coinIds = any(), fields = any()) }
    }

    @Test
    fun `fetch if fields is EMPTY`() = runTest {
        // Act
        val actual = fetcher.fetch(
            fiatCurrencyId = "usd",
            currenciesIds = setOf("ethereum"),
            fields = emptySet(),
        )

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected = QuotesFetcher.Error.InvalidArgumentsError.left()
        Truth.assertThat(actual).isEqualTo(expected)

        val expectedCacheData = ConcurrentHashMap<String, Set<QuoteMetadata>>()
        Truth.assertThat(actualCacheData).isEqualTo(expectedCacheData)

        coVerify(inverse = true) { tangemTechApi.getQuotes(currencyId = any(), coinIds = any(), fields = any()) }
    }

    @Test
    fun `fetch if all currencies ids ARE CACHED and ARE NOT EXPIRED`() = runTest {
        // Arrange
        val quote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ZERO)

        fetcher.setCachedQuotes(
            fiatCurrencyId = "usd",
            quotes = setOf(
                QuoteMetadata(
                    cryptoCurrencyId = "ethereum",
                    timestamp = DateTime.now().millis,
                    value = quote,
                ),
            ),
        )

        // Act
        val actual = fetcher.fetch(
            fiatCurrencyId = "usd",
            currenciesIds = setOf("ethereum"),
            fields = setOf(Field.PRICE),
        )

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected = QuotesResponse(quotes = mapOf("ethereum" to quote)).right()
        Truth.assertThat(actual).isEqualTo(expected)

        Truth.assertThat(actualCacheData.keys().toList().size).isEqualTo(1)
        Truth.assertThat(actualCacheData["usd"].toResponseQuotes()).isEqualTo(mapOf("ethereum" to quote))

        coVerify(inverse = true) { tangemTechApi.getQuotes(currencyId = any(), coinIds = any(), fields = any()) }
    }

    @Test
    fun `fetch if all currencies ids ARE CACHED and EXPIRED`() = runTest {
        // Arrange
        val cachedQuote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)
        val apiQuote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)
        val apiResponse = ApiResponse.Success(
            data = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)),
        )

        fetcher.setCachedQuotes(
            fiatCurrencyId = "usd",
            quotes = setOf(
                QuoteMetadata(
                    cryptoCurrencyId = "ethereum",
                    timestamp = DateTime.now().millis - 10_000,
                    value = cachedQuote,
                ),
            ),
        )

        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price")
        } returns apiResponse

        // Act
        val actual = fetcher.fetch(
            fiatCurrencyId = "usd",
            currenciesIds = setOf("ethereum"),
            fields = setOf(Field.PRICE),
        )

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)).right()
        Truth.assertThat(actual).isEqualTo(expected)

        Truth.assertThat(actualCacheData.keys().toList().size).isEqualTo(1)
        Truth.assertThat(actualCacheData["usd"].toResponseQuotes()).isEqualTo(mapOf("ethereum" to apiQuote))

        coVerify(exactly = 1) { tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price") }
    }

    @Test
    fun `fetch if cache contain EXPIRED and NOT EXPIRED quotes`() = runTest {
        // Arrange
        val cachedQuote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ZERO)
        val apiQuote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)
        val apiResponse = ApiResponse.Success(
            data = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)),
        )

        fetcher.setCachedQuotes(
            fiatCurrencyId = "usd",
            quotes = setOf(
                QuoteMetadata(
                    cryptoCurrencyId = "ethereum",
                    timestamp = DateTime.now().millis - 10_000,
                    value = cachedQuote,
                ),
                QuoteMetadata(
                    cryptoCurrencyId = "bitcoin",
                    timestamp = DateTime.now().millis,
                    value = cachedQuote,
                ),
            ),
        )

        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price")
        } returns apiResponse

        // Act
        val actual = fetcher.fetch(
            fiatCurrencyId = "usd",
            currenciesIds = setOf("ethereum", "bitcoin"),
            fields = setOf(Field.PRICE),
        )

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected = QuotesResponse(
            quotes = mapOf("ethereum" to apiQuote, "bitcoin" to cachedQuote),
        ).right()

        Truth.assertThat(actual).isEqualTo(expected)

        Truth.assertThat(actualCacheData.keys().toList().size).isEqualTo(1)
        Truth.assertThat(actualCacheData["usd"].toResponseQuotes())
            .isEqualTo(mapOf("ethereum" to apiQuote, "bitcoin" to cachedQuote))

        coVerify(exactly = 1) { tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price") }
    }

    @Test
    fun `fetch if cache DOES NOT CONTAIN fiat currency`() = runTest {
        // Arrange
        val apiQuote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)
        val apiResponse = ApiResponse.Success(
            data = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)),
        )

        fetcher.setCachedQuotes(fiatCurrencyId = "eu", quotes = emptySet())

        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price")
        } returns apiResponse

        // Act
        val actual = fetcher.fetch(
            fiatCurrencyId = "usd",
            currenciesIds = setOf("ethereum"),
            fields = setOf(Field.PRICE),
        )

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)).right()
        Truth.assertThat(actual).isEqualTo(expected)

        Truth.assertThat(actualCacheData.keys().toList().size).isEqualTo(2)
        Truth.assertThat(actualCacheData["eu"]).isEmpty()
        Truth.assertThat(actualCacheData["usd"].toResponseQuotes()).isEqualTo(mapOf("ethereum" to apiQuote))

        coVerify(exactly = 1) { tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price") }
    }

    @Test
    fun `fetch if cached quotes are ABSENT`() = runTest {
        // Arrange
        val apiQuote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)
        val apiResponse = ApiResponse.Success(
            data = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)),
        )

        fetcher.setCachedQuotes(fiatCurrencyId = "usd", quotes = emptySet())

        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price")
        } returns apiResponse

        // Act
        val actual = fetcher.fetch(
            fiatCurrencyId = "usd",
            currenciesIds = setOf("ethereum"),
            fields = setOf(Field.PRICE),
        )

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)).right()
        Truth.assertThat(actual).isEqualTo(expected)

        Truth.assertThat(actualCacheData.keys().toList().size).isEqualTo(1)
        Truth.assertThat(actualCacheData["usd"].toResponseQuotes())
            .isEqualTo(mapOf("ethereum" to apiQuote))

        coVerify(exactly = 1) { tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price") }
    }

    @Test
    fun `two parallel fetch if cache is empty`() = runTest {
        // Arrange
        val apiQuote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)

        val usdApiResponse = ApiResponse.Success(
            data = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)),
        )

        val euApiResponse = ApiResponse.Success(
            data = QuotesResponse(quotes = mapOf("bitcoin" to apiQuote)),
        )

        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price")
        } returns usdApiResponse

        coEvery {
            tangemTechApi.getQuotes(currencyId = "eu", coinIds = "bitcoin", fields = "price")
        } returns euApiResponse

        // Act
        val actual = listOf(
            async {
                fetcher.fetch(
                    fiatCurrencyId = "usd",
                    currenciesIds = setOf("ethereum"),
                    fields = setOf(Field.PRICE),
                )
            },
            async {
                fetcher.fetch(
                    fiatCurrencyId = "eu",
                    currenciesIds = setOf("bitcoin"),
                    fields = setOf(Field.PRICE),
                )
            },
        )
            .awaitAll()

        val actual1 = actual[0]
        val actual2 = actual[1]

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected1 = QuotesResponse(quotes = mapOf("ethereum" to apiQuote)).right()
        val expected2 = QuotesResponse(quotes = mapOf("bitcoin" to apiQuote)).right()
        Truth.assertThat(actual1).isEqualTo(expected1)
        Truth.assertThat(actual2).isEqualTo(expected2)

        Truth.assertThat(actualCacheData.keys().toList().size).isEqualTo(2)
        Truth.assertThat(actualCacheData["usd"].toResponseQuotes())
            .isEqualTo(mapOf("ethereum" to apiQuote))
        Truth.assertThat(actualCacheData["eu"].toResponseQuotes())
            .isEqualTo(mapOf("bitcoin" to apiQuote))

        coVerify(exactly = 1) {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum", fields = "price")
            tangemTechApi.getQuotes(currencyId = "eu", coinIds = "bitcoin", fields = "price")
        }
    }

    @Test
    fun `two parallel fetch`() = runTest {
        // Arrange
        val apiQuote = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)

        val firstApiResponse = ApiResponse.Success(
            data = QuotesResponse(
                quotes = mapOf("ethereum" to apiQuote, "solana" to apiQuote),
            ),
        )

        val secondApiResponse = ApiResponse.Success(
            data = QuotesResponse(
                quotes = mapOf("bitcoin" to apiQuote, "solana" to apiQuote),
            ),
        )

        fetcher.setCachedQuotes(
            fiatCurrencyId = "usd",
            quotes = setOf(
                QuoteMetadata(
                    cryptoCurrencyId = "solana",
                    timestamp = DateTime.now().millis - 10_000,
                    value = MockQuoteResponseFactory.createSinglePrice(BigDecimal.ZERO),
                ),
            ),
        )

        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum,solana", fields = "price")
        } returns firstApiResponse

        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "bitcoin", fields = "price")
        } returns secondApiResponse

        // Act
        val actual = listOf(
            async {
                fetcher.fetch(
                    fiatCurrencyId = "usd",
                    currenciesIds = setOf("ethereum", "solana"),
                    fields = setOf(Field.PRICE),
                )
            },
            async {
                fetcher.fetch(
                    fiatCurrencyId = "usd",
                    currenciesIds = setOf("bitcoin", "solana"),
                    fields = setOf(Field.PRICE),
                )
            },
        )
            .awaitAll()

        val actual1 = actual[0]
        val actual2 = actual[1]

        val actualCacheData = fetcher.getCachedQuotes()

        // Assert
        val expected1 = QuotesResponse(quotes = mapOf("ethereum" to apiQuote, "solana" to apiQuote)).right()
        val expected2 = QuotesResponse(quotes = mapOf("bitcoin" to apiQuote, "solana" to apiQuote)).right()
        Truth.assertThat(actual1).isEqualTo(expected1)
        Truth.assertThat(actual2).isEqualTo(expected2)

        Truth.assertThat(actualCacheData.keys().toList().size).isEqualTo(1)
        Truth.assertThat(actualCacheData["usd"].toResponseQuotes()).isEqualTo(
            mapOf("ethereum" to apiQuote, "bitcoin" to apiQuote, "solana" to apiQuote),
        )

        coVerifyOrder {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "ethereum,solana", fields = "price")
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = "bitcoin", fields = "price")
        }
    }

    private fun Iterable<QuoteMetadata>?.toResponseQuotes() = this!!.associate { it.cryptoCurrencyId to it.value }
}