package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

data class ExchangeQuoteResponse(

    @Json(name = "fromAmount")
    val fromAmount: String,

    @Json(name = "fromDecimals")
    val fromDecimals: Int,

    @Json(name = "toAmount")
    val toAmount: String,

    @Json(name = "toDecimals")
    val toDecimals: Int,

    @Json(name = "allowanceContract")
    val allowanceContract: String?,

    @Json(name = "minAmount")
    val minAmount: BigDecimal,

)
