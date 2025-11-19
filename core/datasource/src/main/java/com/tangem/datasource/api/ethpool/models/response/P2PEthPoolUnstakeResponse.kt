package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response for POST /api/v1/staking/pool/{network}/staking/unstake
 *
 * Note: Contains Bitcoin-related fields (likely documentation error).
 * Using as-is per specification.
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolUnstakeResponse(
    @Json(name = "stakerPublicKey")
    val stakerPublicKey: String,
    @Json(name = "stakeTransactionHash")
    val stakeTransactionHash: String,
    @Json(name = "unstakeTransactionHex")
    val unstakeTransactionHex: String, // unsigned
    @Json(name = "unstakeFee")
    val unstakeFee: Double,
)