package com.tangem.data.quotes.repository

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.QuotesRepository
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
    private val repository: QuotesRepository = DefaultQuotesRepository(quotesStatusesStore = quotesStatusesStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(quotesStatusesStore)
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

    data class GetMultiQuoteSyncOrNullModel(
        val initialStore: Set<QuoteStatus>?,
        val currencyIds: Set<CryptoCurrency.RawID>,
        val expected: Set<QuoteStatus>?,
    )
}