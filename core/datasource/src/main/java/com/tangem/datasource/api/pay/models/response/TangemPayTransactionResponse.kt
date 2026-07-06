package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response of `GET v1/customer/transactions/{transaction_id}` — a single transaction by its id. */
@JsonClass(generateAdapter = true)
data class TangemPayTransactionResponse(
    @Json(name = "result") val result: TangemPayTxHistoryResponse.Transaction,
)