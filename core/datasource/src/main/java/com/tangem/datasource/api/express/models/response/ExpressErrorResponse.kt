package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

data class ExpressErrorResponse(
    @Json(name = "error")
    val error: ExpressError,
)

data class ExpressError(
    @Json(name = "code")
    val code: Int,

    @Json(name = "description")
    val description: String?,

    @Json(name = "value")
    val value: ExpressErrorValue?,
)

data class ExpressErrorValue(
    @Json(name = "minAmount")
    val minAmount: String?,

    @Json(name = "maxAmount")
    val maxAmount: String?,

    @Json(name = "decimals")
    val decimals: Int?,

    @Json(name = "currentAllowance")
    val currentAllowance: BigDecimal?,

    @Json(name = "receivedFromDecimals")
    val receivedFromDecimals: Int?,

    @Json(name = "expressFromDecimals")
    val expressFromDecimals: Int?,

    @Json(name = "fromAmount")
    val fromAmount: String?,

    @Json(name = "fromAmountProvider")
    val fromAmountProvider: String?,
)