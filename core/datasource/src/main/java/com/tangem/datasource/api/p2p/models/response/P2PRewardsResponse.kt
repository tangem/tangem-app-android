package com.tangem.datasource.api.p2p.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Response for GET /api/v1/staking/pool/{network}/account/{delegatorAddress}/vault/{vaultAddress}/rewards
 */
@JsonClass(generateAdapter = true)
data class P2PRewardsResponse(
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "rewards")
    val rewards: List<P2PRewardEntryDTO>,
)

@JsonClass(generateAdapter = true)
data class P2PRewardEntryDTO(
    @Json(name = "date")
    val date: DateTime,
    @Json(name = "apy")
    val apy: Double,
    @Json(name = "balance")
    val balance: BigDecimal,
    @Json(name = "rewards")
    val rewards: BigDecimal,
)