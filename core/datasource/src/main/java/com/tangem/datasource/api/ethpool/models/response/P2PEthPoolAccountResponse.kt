package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response for GET /api/v1/staking/pool/{network}/account/{delegatorAddress}/vault/{vaultAddress}
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolAccountResponse(
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "stake")
    val stake: P2PEthPoolStakeDTO,
    @Json(name = "availableToUnstake")
    val availableToUnstake: BigDecimal,
    @Json(name = "availableToWithdraw")
    val availableToWithdraw: BigDecimal,
    @Json(name = "exitQueue")
    val exitQueue: P2PEthPoolExitQueueDTO,
)

@JsonClass(generateAdapter = true)
data class P2PEthPoolStakeDTO(
    @Json(name = "assets")
    val assets: BigDecimal,
    @Json(name = "totalEarnedAssets")
    val totalEarnedAssets: BigDecimal,
)

@JsonClass(generateAdapter = true)
data class P2PEthPoolExitQueueDTO(
    @Json(name = "total")
    val total: BigDecimal,
    @Json(name = "requests")
    val requests: List<P2PEthPoolExitRequestDTO>,
)

@JsonClass(generateAdapter = true)
data class P2PEthPoolExitRequestDTO(
    @Json(name = "ticket")
    val ticket: String,
    @Json(name = "totalAssets")
    val totalAssets: BigDecimal,
    @Json(name = "timestamp")
    val timestamp: Long,
    @Json(name = "withdrawalTimestamp")
    val withdrawalTimestamp: Long?,
    @Json(name = "isClaimable")
    val isClaimable: Boolean,
)