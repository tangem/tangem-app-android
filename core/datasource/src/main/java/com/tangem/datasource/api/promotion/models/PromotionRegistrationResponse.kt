package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PromotionRegistrationResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: RegistrationData,
) {

    @JsonClass(generateAdapter = true)
    data class RegistrationData(
        @Json(name = "campaignId") val campaignId: String,
        @Json(name = "registeredAt") val registeredAt: String?,
        @Json(name = "tokenReward") val tokenReward: RegisteredTokenRewardDto,
    )

    @JsonClass(generateAdapter = true)
    data class RegisteredTokenRewardDto(
        @Json(name = "tokenAddress") val tokenAddress: String,
        @Json(name = "networkId") val networkId: String,
        @Json(name = "tokenId") val tokenId: String,
    )
}