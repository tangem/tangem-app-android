package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransactionEventBody(
    @Json(name = "transactionId") val transactionId: String,
    @Json(name = "operationType") val operationType: OperationType,
)

@JsonClass(generateAdapter = false)
enum class OperationType {

    @Json(name = "YIELD_DEPOSIT")
    YIELD_DEPOSIT,

    @Json(name = "YIELD_WITHDRAW")
    YIELD_WITHDRAW,

    @Json(name = "YIELD_SEND")
    YIELD_SEND,
}