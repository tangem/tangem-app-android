package com.tangem.datasource.api.p2p.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response for GET /api/v1/staking/pool/{network}/account/{delegatorAddress}/vault/{vaultAddress}
 */
@JsonClass(generateAdapter = true)
data class P2PAccountInfoResponse(
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "stake")
    val stake: P2PStakeDTO,
    @Json(name = "availableToUnstake")
    val availableToUnstake: BigDecimal,
    @Json(name = "availableToWithdraw")
    val availableToWithdraw: BigDecimal,
    @Json(name = "exitQueue")
    val exitQueue: P2PExitQueueDTO,
)

@JsonClass(generateAdapter = true)
data class P2PStakeDTO(
    @Json(name = "assets")
    val assets: BigDecimal,
    @Json(name = "totalEarnedAssets")
    val totalEarnedAssets: BigDecimal,
)

@JsonClass(generateAdapter = true)
data class P2PExitQueueDTO(
    @Json(name = "total")
    val total: Double,
    @Json(name = "requests")
    val requests: List<P2PExitRequestDTO>,
)

@JsonClass(generateAdapter = true)
data class P2PExitRequestDTO(
    @Json(name = "ticket")
    val ticket: String,
    @Json(name = "totalAssets")
    val totalAssets: Double,
    @Json(name = "timestamp")
    val timestamp: Long,
    @Json(name = "withdrawalTimestamp")
    val withdrawalTimestamp: Long,
    @Json(name = "isClaimable")
    val isClaimable: Boolean,
)