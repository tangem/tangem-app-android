package com.tangem.data.tokens.entity

import com.squareup.moshi.JsonClass
import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
internal data class QuoteDM(
    val rawCurrencyId: CryptoCurrency.RawID,
    val fiatRate: BigDecimal,
    val priceChange: BigDecimal,
)