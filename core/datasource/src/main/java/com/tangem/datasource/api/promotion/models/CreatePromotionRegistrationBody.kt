package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreatePromotionRegistrationBody(
    @Json(name = "campaignId") val campaignId: String,
    @Json(name = "walletIds") val walletIds: List<String>,
    @Json(name = "tokenReward") val tokenReward: TokenRewardDto,
) {

    @JsonClass(generateAdapter = true)
    data class TokenRewardDto(
        @Json(name = "tokenAddress") val tokenAddress: String,
        @Json(name = "networkId") val networkId: String,
        @Json(name = "userAddress") val userAddress: String,
        @Json(name = "tokenId") val tokenId: String,
    )
}