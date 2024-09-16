package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class TokenMarketChartResponse(
    @Json(name = "prices")
    val prices: Map<Long, BigDecimal>,
)