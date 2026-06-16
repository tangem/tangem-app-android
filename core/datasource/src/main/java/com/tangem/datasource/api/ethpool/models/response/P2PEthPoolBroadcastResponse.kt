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
    val status: String,
    @Json(name = "blockNumber")
    val blockNumber: Int? = null,
    @Json(name = "transactionIndex")
    val transactionIndex: Int? = null,
    @Json(name = "gasUsed")
    val gasUsed: String? = null,
    @Json(name = "cumulativeGasUsed")
    val cumulativeGasUsed: String? = null,
    @Json(name = "effectiveGasPrice")
    val effectiveGasPrice: String? = null,
    @Json(name = "from")
    val from: String,
    @Json(name = "to")
    val to: String,
)