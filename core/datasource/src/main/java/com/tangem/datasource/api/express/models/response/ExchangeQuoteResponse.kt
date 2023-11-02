package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

data class ExchangeQuoteResponse(
    @Json(name = "toAmount")
    val toAmount: BigDecimal,

    @Json(name = "allowanceContract")
    val allowanceContract: String?,
)