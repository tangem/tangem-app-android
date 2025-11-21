package com.tangem.datasource.api.ethpool.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for creating unstake transaction
 *
 * Used in: POST /api/v1/staking/pool/{network}/staking/unstake
 *
 * Note: Documentation seems to contain Bitcoin-related fields (possibly copy-paste error).
 * Using as-is per specification.
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolUnstakeRequest(
    @Json(name = "stakerPublicKey")
    val stakerPublicKey: String,
    @Json(name = "stakeTransactionHash")
    val stakeTransactionHash: String,
)