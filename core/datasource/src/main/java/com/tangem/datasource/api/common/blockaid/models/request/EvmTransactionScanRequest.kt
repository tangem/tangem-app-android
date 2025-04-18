package com.tangem.datasource.api.common.blockaid.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.common.blockaid.models.response.TransactionMetadata

@JsonClass(generateAdapter = true)
data class EvmTransactionScanRequest(
    @Json(name = "chain") val chain: String,
    @Json(name = "account_address") val accountAddress: String,
    @Json(name = "method") val method: String,
    @Json(name = "data") val data: RpcData,
    @Json(name = "options") val options: List<String> = listOf("simulation", "validation"),
    @Json(name = "metadata") val metadata: TransactionMetadata,
)

@JsonClass(generateAdapter = true)
data class RpcData(
    @Json(name = "jsonrpc") val jsonrpc: String = "2.0",
    @Json(name = "method") val method: String,
    @Json(name = "params") val params: String,
)