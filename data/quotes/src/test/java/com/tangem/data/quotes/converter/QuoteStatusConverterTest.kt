package com.tangem.data.quotes.converter

import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.common.test.data.quote.toDomain
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class QuoteStatusConverterTest {

    private val ethQuoteDM = "ETH" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE)

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertTestModel) {
        // Act
        val actual = QuoteStatusConverter(source = model.source).convert(value = model.value)

        // Assert
        val expected = model.expected
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        ConvertTestModel(
            source = StatusSource.CACHE,
            value = createEntry(value = ethQuoteDM),
            expected = ethQuoteDM.toDomain(source = StatusSource.CACHE),
        ),
        ConvertTestModel(
            source = StatusSource.ONLY_CACHE,
            value = createEntry(value = ethQuoteDM),
            expected = ethQuoteDM.toDomain(source = StatusSource.ONLY_CACHE),
        ),
        ConvertTestModel(
            source = StatusSource.ACTUAL,
            value = createEntry(value = ethQuoteDM),
            expected = ethQuoteDM.toDomain(source = StatusSource.ACTUAL),
        ),
        ConvertTestModel(
            source = StatusSource.ACTUAL,
            value = createEntry(
                value = "ETH" to QuotesResponse.Quote(
                    price = null,
                    priceChange24h = null,
                    priceChange1w = null,
                    priceChange30d = null,
                ),
            ),
            expected = QuoteStatus(
                rawCurrencyId = CryptoCurrency.RawID("ETH"),
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = BigDecimal.ZERO,
                    priceChange = BigDecimal("0.00"),
                ),
            ),
        ),
        ConvertTestModel(
            source = StatusSource.ACTUAL,
            value = createEntry(
                value = "ETH" to QuotesResponse.Quote(
                    price = null,
                    priceChange24h = null,
                    priceChange1w = BigDecimal.ZERO,
                    priceChange30d = BigDecimal.ZERO,
                ),
            ),
            expected = QuoteStatus(
                rawCurrencyId = CryptoCurrency.RawID("ETH"),
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = BigDecimal.ZERO,
                    priceChange = BigDecimal("0.00"),
                ),
            ),
        ),
        ConvertTestModel(
            source = StatusSource.ACTUAL,
            value = createEntry(
                value = "ETH" to QuotesResponse.Quote(
                    price = BigDecimal.ONE,
                    priceChange24h = BigDecimal.ONE,
                    priceChange1w = null,
                    priceChange30d = null,
                ),
            ),
            expected = QuoteStatus(
                rawCurrencyId = CryptoCurrency.RawID("ETH"),
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = BigDecimal.ONE,
                    priceChange = BigDecimal("0.01"),
                ),
            ),
        ),
    )

    private fun createEntry(value: Pair<String, QuotesResponse.Quote>): Map.Entry<String, QuotesResponse.Quote> {
        return mapOf(value).entries.first()
    }

    data class ConvertTestModel(
        val source: StatusSource,
        val value: Map.Entry<String, QuotesResponse.Quote>,
        val expected: QuoteStatus,
    )
}