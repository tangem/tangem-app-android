package com.tangem.datasource.api.ethpool.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for broadcasting signed transaction
 *
 * Used in: POST /api/v1/staking/pool/{network}/transaction/send
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolBroadcastRequest(
    @Json(name = "signedTransaction")
    val signedTransaction: String,
)