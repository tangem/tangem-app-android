package com.tangem.datasource.api.ethpool.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for creating withdrawal transaction
 *
 * Used in: POST /api/v1/staking/pool/{network}/staking/withdraw
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolWithdrawRequest(
    @Json(name = "stakerAddress")
    val stakerAddress: String,
)