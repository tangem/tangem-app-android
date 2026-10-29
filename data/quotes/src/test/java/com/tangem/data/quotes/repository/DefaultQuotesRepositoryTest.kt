package com.tangem.data.quotes.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultQuotesRepositoryTest {

    private val quotesStatusesStore = mockk<QuotesStatusesStore>()
    private val quotesFetcher = mockk<QuotesFetcher>()
    private val repository: QuotesRepository = DefaultQuotesRepository(
        quotesStatusesStore = quotesStatusesStore,
        quotesFetcher = quotesFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(quotesStatusesStore, quotesFetcher)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetMultiQuoteSyncOrNull {

        private val btcRawId = CryptoCurrency.RawID(value = "BTC")
        private val ethRawId = CryptoCurrency.RawID(value = "ETH")

        private val ethQuote = QuoteStatus(
            rawCurrencyId = ethRawId,
            value = QuoteStatus.Data(
                source = StatusSource.ACTUAL,
                fiatRate = BigDecimal.ZERO,
                fiatRateUSD = BigDecimal.ZERO,
                priceChange = BigDecimal.ZERO,
            ),
        )

        @ParameterizedTest
        @ProvideTestModels
        fun getMultiQuoteSyncOrNull(model: GetMultiQuoteSyncOrNullModel) = runTest {
            // Arrange
            coEvery { quotesStatusesStore.getAllSyncOrNull() } returns model.initialStore

            // Act
            val actual = repository.getMultiQuoteSyncOrNull(currenciesIds = model.currencyIds)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            GetMultiQuoteSyncOrNullModel(
                initialStore = null,
                currencyIds = emptySet(),
                expected = emptySet(),
            ),
            GetMultiQuoteSyncOrNullModel(
                initialStore = null,
                currencyIds = setOf(ethRawId),
                expected = setOf(QuoteStatus(rawCurrencyId = ethRawId)),
            ),
            GetMultiQuoteSyncOrNullModel(
                initialStore = emptySet(),
                currencyIds = emptySet(),
                expected = emptySet(),
            ),
            GetMultiQuoteSyncOrNullModel(
                initialStore = emptySet(),
                currencyIds = setOf(ethRawId),
                expected = setOf(QuoteStatus(rawCurrencyId = ethRawId)),
            ),
            GetMultiQuoteSyncOrNullModel(
                initialStore = setOf(ethQuote),
                currencyIds = setOf(ethRawId),
                expected = setOf(ethQuote),
            ),
            GetMultiQuoteSyncOrNullModel(
                initialStore = setOf(QuoteStatus(rawCurrencyId = btcRawId)),
                currencyIds = setOf(ethRawId),
                expected = setOf(QuoteStatus(rawCurrencyId = ethRawId)),
            ),
            GetMultiQuoteSyncOrNullModel(
                initialStore = setOf(ethQuote, QuoteStatus(rawCurrencyId = btcRawId)),
                currencyIds = setOf(ethRawId),
                expected = setOf(ethQuote),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCurrencyUSDQuote {

        private val btcRawId = CryptoCurrency.RawID(value = "BTC")
        private val ethRawId = CryptoCurrency.RawID(value = "ETH")

        private val ethQuote = QuoteStatus(
            rawCurrencyId = ethRawId,
            value = QuoteStatus.Data(
                source = StatusSource.ACTUAL,
                fiatRate = BigDecimal.ZERO,
                fiatRateUSD = BigDecimal.ZERO,
                priceChange = BigDecimal.ZERO,
            ),
        )

        @ParameterizedTest
        @ProvideTestModels
        fun getCurrencyUSDQuote(model: GetCurrencyUSDQuoteModel) = runTest {
            // Arrange
            coEvery { quotesStatusesStore.getAllSyncOrNull() } returns model.initialStore

            // Act
            val actual = repository.getCurrencyUSDQuote(currencyId = model.currencyId)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            GetCurrencyUSDQuoteModel(
                initialStore = null,
                currencyId = ethRawId,
                expected = QuoteStatus(rawCurrencyId = ethRawId),
            ),
            GetCurrencyUSDQuoteModel(
                initialStore = emptySet(),
                currencyId = ethRawId,
                expected = QuoteStatus(rawCurrencyId = ethRawId),
            ),
            GetCurrencyUSDQuoteModel(
                initialStore = setOf(ethQuote),
                currencyId = ethRawId,
                expected = ethQuote,
            ),
            GetCurrencyUSDQuoteModel(
                initialStore = setOf(QuoteStatus(rawCurrencyId = btcRawId)),
                currencyId = ethRawId,
                expected = QuoteStatus(rawCurrencyId = ethRawId),
            ),
            GetCurrencyUSDQuoteModel(
                initialStore = setOf(ethQuote, QuoteStatus(rawCurrencyId = btcRawId)),
                currencyId = ethRawId,
                expected = ethQuote,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetFiatUSDRate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN quotes fetch result WHEN getFiatUSDRate THEN returns derived rate or null`(
            model: GetFiatUSDRateModel,
        ) = runTest {
            // Arrange
            coEvery {
                quotesFetcher.fetch(
                    fiatCurrencyId = "EUR",
                    currenciesIds = setOf("bitcoin"),
                    fields = setOf(QuotesFetcher.Field.ALL_PRICES),
                )
            } returns model.fetchResult

            // Act
            val actual = repository.getFiatUSDRate(fiatCurrencyCode = "eur")

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            GetFiatUSDRateModel(
                fetchResult = btcQuote(price = BigDecimal("50000"), priceUsd = BigDecimal("55000")).right(),
                expected = BigDecimal("1.1"),
            ),
            GetFiatUSDRateModel(
                fetchResult = QuotesFetcher.Error.InvalidArgumentsError.left(),
                expected = null,
            ),
            GetFiatUSDRateModel(
                fetchResult = QuotesResponse(quotes = emptyMap()).right(),
                expected = null,
            ),
            GetFiatUSDRateModel(
                fetchResult = btcQuote(price = null, priceUsd = BigDecimal("55000")).right(),
                expected = null,
            ),
            GetFiatUSDRateModel(
                fetchResult = btcQuote(price = BigDecimal.ZERO, priceUsd = BigDecimal("55000")).right(),
                expected = null,
            ),
            GetFiatUSDRateModel(
                fetchResult = btcQuote(price = BigDecimal("50000"), priceUsd = null).right(),
                expected = null,
            ),
        )

        private fun btcQuote(price: BigDecimal?, priceUsd: BigDecimal?) = QuotesResponse(
            quotes = mapOf("bitcoin" to QuotesResponse.Quote.EMPTY.copy(price = price, priceUsd = priceUsd)),
        )
    }

    data class GetMultiQuoteSyncOrNullModel(
        val initialStore: Set<QuoteStatus>?,
        val currencyIds: Set<CryptoCurrency.RawID>,
        val expected: Set<QuoteStatus>?,
    )

    data class GetCurrencyUSDQuoteModel(
        val initialStore: Set<QuoteStatus>?,
        val currencyId: CryptoCurrency.RawID,
        val expected: QuoteStatus?,
    )

    data class GetFiatUSDRateModel(
        val fetchResult: Either<QuotesFetcher.Error, QuotesResponse>,
        val expected: BigDecimal?,
    )
}