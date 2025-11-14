package com.tangem.datasource.api.p2p.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for creating deposit transaction
 *
 * Used in: POST /api/v1/staking/pool/{network}/staking/deposit
 */
@JsonClass(generateAdapter = true)
data class P2PDepositRequestBody(
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "amount")
    val amount: Double,
)