package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class TransactionCashbackResponse(
    @Json(name = "status") val status: String,
    @Json(name = "amount") val amount: BigDecimal? = null,
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "cap_trimmed") val isCapTrimmed: Boolean? = null,
    @Json(name = "exclusion_reason") val exclusionReason: String? = null,
)