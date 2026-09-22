package com.tangem.grow.datasource.yield.models.incentives

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldClaimCalldataResponse(
    @Json(name = "chainId") val chainId: Int? = null,
    @Json(name = "userAddress") val userAddress: String? = null,
    @Json(name = "moduleAddress") val moduleAddress: String? = null,
    @Json(name = "hasClaimable") val hasClaimable: Boolean? = null,
    @Json(name = "to") val to: String? = null,
    @Json(name = "data") val data: String? = null,
    @Json(name = "rewardTokens") val rewardTokens: List<String>? = null,
    @Json(name = "cumulativeAmounts") val cumulativeAmounts: List<String>? = null,
    @Json(name = "totalEstimatedServiceFeeUsd") val totalEstimatedServiceFeeUsd: BigDecimal? = null,
    @Json(name = "claimRecommended") val isClaimRecommended: Boolean? = null,
)