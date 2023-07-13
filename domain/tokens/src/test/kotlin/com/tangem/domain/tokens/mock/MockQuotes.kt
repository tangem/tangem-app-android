package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import com.tangem.domain.tokens.model.Quote
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockQuotes {

    val quote1 = Quote(
        tokenId = MockTokens.token1.id,
        fiatRate = BigDecimal("1.23"),
        priceChange = BigDecimal("0.01"),
    )

    val quote2 = Quote(
        tokenId = MockTokens.token2.id,
        fiatRate = BigDecimal("2.34"),
        priceChange = BigDecimal("-0.02"),
    )

    val quote3 = Quote(
        tokenId = MockTokens.token3.id,
        fiatRate = BigDecimal("3.45"),
        priceChange = BigDecimal("0.03"),
    )

    val quote4 = Quote(
        tokenId = MockTokens.token4.id,
        fiatRate = BigDecimal("4.56"),
        priceChange = BigDecimal("-0.04"),
    )

    val quote5 = Quote(
        tokenId = MockTokens.token5.id,
        fiatRate = BigDecimal("5.67"),
        priceChange = BigDecimal("0.05"),
    )

    val quote6 = Quote(
        tokenId = MockTokens.token6.id,
        fiatRate = BigDecimal("6.78"),
        priceChange = BigDecimal("-0.06"),
    )

    val quote7 = Quote(
        tokenId = MockTokens.token7.id,
        fiatRate = BigDecimal("7.89"),
        priceChange = BigDecimal("0.07"),
    )

    val quote8 = Quote(
        tokenId = MockTokens.token8.id,
        fiatRate = BigDecimal("8.90"),
        priceChange = BigDecimal("-0.08"),
    )

    val quote9 = Quote(
        tokenId = MockTokens.token9.id,
        fiatRate = BigDecimal("9.01"),
        priceChange = BigDecimal("0.09"),
    )

    val quote10 = Quote(
        tokenId = MockTokens.token10.id,
        fiatRate = BigDecimal("10.12"),
        priceChange = BigDecimal("-0.10"),
    )

    val quotes = nonEmptySetOf(quote1, quote2, quote3, quote4, quote5, quote6, quote7, quote8, quote9, quote10)
}
