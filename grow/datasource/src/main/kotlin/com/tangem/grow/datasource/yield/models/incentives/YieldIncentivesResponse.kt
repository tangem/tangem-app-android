package com.tangem.grow.datasource.yield.models.incentives

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldIncentivesResponse(
    @Json(name = "chainId") val chainId: Int? = null,
    @Json(name = "userAddress") val userAddress: String? = null,
    @Json(name = "moduleAddress") val moduleAddress: String? = null,
    @Json(name = "rewards") val rewards: List<YieldRewardDto>? = null,
    @Json(name = "lastUpdatedAt") val lastUpdatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class YieldRewardDto(
    @Json(name = "source") val source: String? = null,
    @Json(name = "protocol") val protocol: String? = null,
    @Json(name = "rewardToken") val rewardToken: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "tokenDecimals") val tokenDecimals: Int? = null,
    @Json(name = "claimableAmount") val claimableAmount: String? = null,
    @Json(name = "pendingAmount") val pendingAmount: String? = null,
    @Json(name = "usdValue") val usdValue: BigDecimal? = null,
    @Json(name = "serviceFeeRateBps") val serviceFeeRateBps: Int? = null,
    @Json(name = "estimatedServiceFeeUsd") val estimatedServiceFeeUsd: BigDecimal? = null,
    @Json(name = "estimatedNetValueUsd") val estimatedNetValueUsd: BigDecimal? = null,
    @Json(name = "claimRecommended") val isClaimRecommended: Boolean? = null,
    @Json(name = "finalDestination") val finalDestination: String? = null,
)