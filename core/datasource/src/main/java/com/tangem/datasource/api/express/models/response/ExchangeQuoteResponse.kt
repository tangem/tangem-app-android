package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

data class ExchangeQuoteResponse(

    @Json(name = "fromAmount")
    val fromAmount: BigDecimal,

    @Json(name = "fromDecimals")
    val fromDecimals: Int,

    @Json(name = "toAmount")
    val toAmount: BigDecimal,

    @Json(name = "toDecimals")
    val toDecimals: Int,

    @Json(name = "allowanceContract")
    val allowanceContract: String?,

    @Json(name = "minAmount")
    val minAmount: BigDecimal,

)