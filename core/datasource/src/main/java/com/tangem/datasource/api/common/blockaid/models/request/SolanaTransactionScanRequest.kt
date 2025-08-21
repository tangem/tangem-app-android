package com.tangem.datasource.api.common.blockaid.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.common.blockaid.models.response.TransactionMetadata

@JsonClass(generateAdapter = true)
data class SolanaTransactionScanRequest(
    @Json(name = "encoding") val encoding: String = "base64",
    @Json(name = "blockchain") val blockchain: String,
    @Json(name = "method") val method: String,
    @Json(name = "options") val options: List<String> = listOf("simulation", "validation"),
    @Json(name = "metadata") val metadata: TransactionMetadata,
    @Json(name = "account_address") val accountAddress: String,
    @Json(name = "transactions") val transactions: List<String>,
)