package com.tangem.grow.datasource.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Response for GET /api/v1/staking/pool/{network}/account/{delegatorAddress}/vault/{vaultAddress}
 */
@Serializable
@JsonClass(generateAdapter = true)
data class P2PEthPoolAccountResponse(
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "stake")
    val stake: P2PEthPoolStakeDTO,
    @Json(name = "availableToUnstake")
    val availableToUnstake: SerializedBigDecimal,
    @Json(name = "availableToWithdraw")
    val availableToWithdraw: SerializedBigDecimal,
    @Json(name = "exitQueue")
    val exitQueue: P2PEthPoolExitQueueDTO,
)

@Serializable
@JsonClass(generateAdapter = true)
data class P2PEthPoolStakeDTO(
    @Json(name = "assets")
    val assets: SerializedBigDecimal,
    @Json(name = "totalEarnedAssets")
    val totalEarnedAssets: SerializedBigDecimal,
)

@Serializable
@JsonClass(generateAdapter = true)
data class P2PEthPoolExitQueueDTO(
    @Json(name = "total")
    val total: SerializedBigDecimal,
    @Json(name = "requests")
    val requests: List<P2PEthPoolExitRequestDTO>,
)

@Serializable
@JsonClass(generateAdapter = true)
data class P2PEthPoolExitRequestDTO(
    @Json(name = "ticket")
    val ticket: String,
    @Json(name = "totalAssets")
    val totalAssets: SerializedBigDecimal,
    @Json(name = "timestamp")
    val timestamp: Long,
    @Json(name = "withdrawalTimestamp")
    val withdrawalTimestamp: Long?,
    @Json(name = "isClaimable")
    val isClaimable: Boolean,
)