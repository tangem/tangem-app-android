package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from `GET /v1/customer/cashback/{transaction_id}/details`.
 *
 * Rich per-transaction cashback that backs the "Cashback" row on the transaction detail screen,
 * loaded independently from the transaction. [cashback] reuses the shared cashback contract and is
 * `null` when cashback is not applicable to the transaction.
 */
@JsonClass(generateAdapter = true)
data class CashbackTransactionDetailsResponse(
    @Json(name = "cashback") val cashback: TransactionCashbackResponse? = null,
)