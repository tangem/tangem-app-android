package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

data class CardBalanceResponse(
    @Json(name = "result") val result: Result?,
    @Json(name = "error") val error: String?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "currency") val currency: String,
        @Json(name = "available_balance") val availableBalance: BigDecimal,
        @Json(name = "credit_limit") val creditLimit: BigDecimal,
        @Json(name = "pending_charges") val pendingCharges: BigDecimal,
        @Json(name = "posted_charges") val postedCharges: BigDecimal,
        @Json(name = "balance_due") val balanceDue: BigDecimal,
    )
}