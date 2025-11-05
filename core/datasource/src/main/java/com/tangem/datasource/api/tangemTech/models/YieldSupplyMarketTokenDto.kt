package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldSupplyMarketTokenDto(
    @Json(name = "tokenAddress") val tokenAddress: String? = null,
    @Json(name = "tokenSymbol") val tokenSymbol: String? = null,
    @Json(name = "tokenName") val tokenName: String? = null,
    @Json(name = "apy") val apy: BigDecimal? = null,
    @Json(name = "decimals") val decimals: Int? = null,
    @Json(name = "isActive") val isActive: Boolean? = null,
    @Json(name = "chainId") val chainId: Int? = null,
    @Json(name = "maxFeeNative") val maxFeeNative: BigDecimal? = null,
    @Json(name = "maxFeeUSD") val maxFeeUSD: BigDecimal? = null,
)