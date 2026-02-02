package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response for POST /api/v1/staking/pool/{network}/transaction/send
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolBroadcastResponse(
    @Json(name = "hash")
    val hash: String,
    @Json(name = "status")
    val status: P2PEthPoolTxStatusDTO,
    @Json(name = "blockNumber")
    val blockNumber: Int,
    @Json(name = "transactionIndex")
    val transactionIndex: Int,
    @Json(name = "gasUsed")
    val gasUsed: String,
    @Json(name = "cumulativeGasUsed")
    val cumulativeGasUsed: String,
    @Json(name = "effectiveGasPrice")
    val effectiveGasPrice: String?,
    @Json(name = "from")
    val from: String,
    @Json(name = "to")
    val to: String,
)

/**
 * Transaction status from P2PEthPool API
 */
@JsonClass(generateAdapter = false)
enum class P2PEthPoolTxStatusDTO {
    @Json(name = "success")
    SUCCESS,

    @Json(name = "failed")
    FAILED,
}