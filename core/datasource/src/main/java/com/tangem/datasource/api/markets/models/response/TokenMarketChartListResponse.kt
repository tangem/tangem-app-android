package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json

class TokenMarketChartListResponse(
    @Json(name = "tokens")
    val tokens: Map<String, TokenMarketChartResponse>,
)
