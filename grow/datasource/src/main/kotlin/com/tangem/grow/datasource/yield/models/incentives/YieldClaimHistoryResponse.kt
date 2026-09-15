package com.tangem.grow.datasource.yield.models.incentives

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YieldClaimHistoryResponse(
    @Json(name = "chainId") val chainId: Int? = null,
    @Json(name = "userAddress") val userAddress: String? = null,
    @Json(name = "moduleAddress") val moduleAddress: String? = null,
    @Json(name = "claims") val claims: List<YieldClaimHistoryEntryDto>? = null,
)

@JsonClass(generateAdapter = true)
data class YieldClaimHistoryEntryDto(
    @Json(name = "tokenAddress") val tokenAddress: String? = null,
    @Json(name = "amount") val amount: String? = null,
    @Json(name = "blockNumber") val blockNumber: Long? = null,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "campaignId") val campaignId: String? = null,
    @Json(name = "root") val root: String? = null,
    @Json(name = "isLatest") val isLatest: Boolean? = null,
)