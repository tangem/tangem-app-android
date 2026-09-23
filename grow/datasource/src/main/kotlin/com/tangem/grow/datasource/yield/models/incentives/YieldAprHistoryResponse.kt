package com.tangem.grow.datasource.yield.models.incentives

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldAprHistoryResponse(
    @Json(name = "chainId") val chainId: Int? = null,
    @Json(name = "tokenAddress") val tokenAddress: String? = null,
    @Json(name = "points") val points: List<YieldAprHistoryPointDto>? = null,
)

@JsonClass(generateAdapter = true)
data class YieldAprHistoryPointDto(
    @Json(name = "date") val date: String? = null,
    @Json(name = "baseApy") val baseApy: BigDecimal? = null,
    @Json(name = "rewardsApr") val rewardsApr: BigDecimal? = null,
    @Json(name = "totalApyEst") val totalApyEst: BigDecimal? = null,
    @Json(name = "carriedForward") val isCarriedForward: Boolean? = null,
)