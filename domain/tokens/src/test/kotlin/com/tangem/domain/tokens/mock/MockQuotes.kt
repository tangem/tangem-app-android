package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockQuotes {

    val quote1 = QuoteStatus(
        rawCurrencyId = MockTokens.token1.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("1.23"),
            priceChange = BigDecimal("0.01"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote2 = QuoteStatus(
        rawCurrencyId = MockTokens.token2.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("2.34"),
            priceChange = BigDecimal("-0.02"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote3 = QuoteStatus(
        rawCurrencyId = MockTokens.token3.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("3.45"),
            priceChange = BigDecimal("0.03"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote4 = QuoteStatus(
        rawCurrencyId = MockTokens.token4.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("4.56"),
            priceChange = BigDecimal("-0.04"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote5 = QuoteStatus(
        rawCurrencyId = MockTokens.token5.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("5.67"),
            priceChange = BigDecimal("0.05"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote6 = QuoteStatus(
        rawCurrencyId = MockTokens.token6.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("6.78"),
            priceChange = BigDecimal("-0.06"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote7 = QuoteStatus(
        rawCurrencyId = MockTokens.token7.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("7.89"),
            priceChange = BigDecimal("0.07"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote8 = QuoteStatus(
        rawCurrencyId = MockTokens.token8.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("8.90"),
            priceChange = BigDecimal("-0.08"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote9 = QuoteStatus(
        rawCurrencyId = MockTokens.token9.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("9.01"),
            priceChange = BigDecimal("0.09"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote10 = QuoteStatus(
        rawCurrencyId = MockTokens.token10.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal("10.12"),
            priceChange = BigDecimal("-0.10"),
            source = StatusSource.ACTUAL,
        ),
    )

    val quote11 = QuoteStatus(rawCurrencyId = CryptoCurrency.RawID("null"), value = QuoteStatus.Empty)

    val quotes = nonEmptySetOf(
        quote1, quote2, quote3, quote4, quote5, quote6, quote7, quote8, quote9, quote10,
        quote11,
    )
}