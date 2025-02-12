package com.tangem.datasource.local.quote.model

import com.squareup.moshi.JsonClass
import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigDecimal

internal typealias QuotesDM = Set<QuoteDM>

@JsonClass(generateAdapter = true)
internal data class QuoteDM(
    val rawCurrencyId: CryptoCurrency.RawID,
    val fiatRate: BigDecimal,
    val priceChange: BigDecimal,
)