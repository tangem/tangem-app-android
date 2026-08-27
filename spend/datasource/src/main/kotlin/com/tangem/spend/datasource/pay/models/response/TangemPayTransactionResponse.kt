package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response of the single-transaction endpoints (`GET v1/transactions/{transaction_id}` and its legacy path). */
@JsonClass(generateAdapter = true)
data class TangemPayTransactionResponse(
    @Json(name = "result") val result: TangemPayTxHistoryResponse.Transaction,
)