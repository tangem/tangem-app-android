package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

data class TokenMarketChartResponse(
    @Json(name = "prices")
    val prices: Map<Long, BigDecimal>,
)