package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from `GET /v1/customer/cashback/{transaction_id}/details`
 */
@JsonClass(generateAdapter = true)
data class CashbackTransactionDetailsResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "cashback") val cashback: TransactionCashbackResponse?,
    )
}