package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockQuotes {

    val quote1 = Quote.Value(
        rawCurrencyId = MockTokens.token1.id.rawCurrencyId!!,
        fiatRate = BigDecimal("1.23"),
        h24ChangePercent = BigDecimal("0.01"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote2 = Quote.Value(
        rawCurrencyId = MockTokens.token2.id.rawCurrencyId!!,
        fiatRate = BigDecimal("2.34"),
        h24ChangePercent = BigDecimal("-0.02"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote3 = Quote.Value(
        rawCurrencyId = MockTokens.token3.id.rawCurrencyId!!,
        fiatRate = BigDecimal("3.45"),
        h24ChangePercent = BigDecimal("0.03"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote4 = Quote.Value(
        rawCurrencyId = MockTokens.token4.id.rawCurrencyId!!,
        fiatRate = BigDecimal("4.56"),
        h24ChangePercent = BigDecimal("-0.04"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote5 = Quote.Value(
        rawCurrencyId = MockTokens.token5.id.rawCurrencyId!!,
        fiatRate = BigDecimal("5.67"),
        h24ChangePercent = BigDecimal("0.05"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote6 = Quote.Value(
        rawCurrencyId = MockTokens.token6.id.rawCurrencyId!!,
        fiatRate = BigDecimal("6.78"),
        h24ChangePercent = BigDecimal("-0.06"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote7 = Quote.Value(
        rawCurrencyId = MockTokens.token7.id.rawCurrencyId!!,
        fiatRate = BigDecimal("7.89"),
        h24ChangePercent = BigDecimal("0.07"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote8 = Quote.Value(
        rawCurrencyId = MockTokens.token8.id.rawCurrencyId!!,
        fiatRate = BigDecimal("8.90"),
        h24ChangePercent = BigDecimal("-0.08"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote9 = Quote.Value(
        rawCurrencyId = MockTokens.token9.id.rawCurrencyId!!,
        fiatRate = BigDecimal("9.01"),
        h24ChangePercent = BigDecimal("0.09"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote10 = Quote.Value(
        rawCurrencyId = MockTokens.token10.id.rawCurrencyId!!,
        fiatRate = BigDecimal("10.12"),
        h24ChangePercent = BigDecimal("-0.10"),
        weekChangePercent = BigDecimal.ZERO,
        monthChangePercent = BigDecimal.ZERO,
    )

    val quote11 = Quote.Empty(CryptoCurrency.RawID("null"))

    val quotes = nonEmptySetOf(
        quote1, quote2, quote3, quote4, quote5, quote6, quote7, quote8, quote9, quote10,
        quote11,
    )
}