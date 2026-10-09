package com.tangem.grow.datasource.yield.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class YieldSupplyMarketTokenDto(
    @Json(name = "tokenAddress") val tokenAddress: String? = null,
    @Json(name = "tokenSymbol") val tokenSymbol: String? = null,
    @Json(name = "tokenName") val tokenName: String? = null,
    @Json(name = "apy") val apy: SerializedBigDecimal? = null,
    @Json(name = "decimals") val decimals: Int? = null,
    @Json(name = "isActive") val isActive: Boolean? = null,
    @Json(name = "chainId") val chainId: Int? = null,
    @Json(name = "maxFeeNative") val maxFeeNative: SerializedBigDecimal? = null,
    @Json(name = "maxFeeUSD") val maxFeeUSD: SerializedBigDecimal? = null,
    @Json(name = "hasActiveCampaign") val hasActiveCampaign: Boolean? = null,
    @Json(name = "rewardsApr") val rewardsApr: SerializedBigDecimal? = null,
    @Json(name = "totalApyEst") val totalApyEst: SerializedBigDecimal? = null,
)