package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PromotionsResponse(
    @Json(name = "promotions") val promotions: List<PromotionDto>,
) {

    @JsonClass(generateAdapter = true)
    data class PromotionDto(
        @Json(name = "name") val name: String,
        @Json(name = "all") val all: All?,
    ) {

        @JsonClass(generateAdapter = true)
        data class All(
            @Json(name = "timeline") val timeline: Timeline,
            @Json(name = "tokens") val tokens: List<PromoToken>?,
            @Json(name = "status") val status: String,
            @Json(name = "link") val link: String?,
        )

        @JsonClass(generateAdapter = true)
        data class Timeline(
            @Json(name = "start") val start: String,
            @Json(name = "end") val end: String,
        )

        @JsonClass(generateAdapter = true)
        data class PromoToken(
            @Json(name = "tokenAddress") val tokenAddress: String,
            @Json(name = "tokenSymbol") val tokenSymbol: String,
            @Json(name = "tokenName") val tokenName: String,
            @Json(name = "networkId") val networkId: String,
        )
    }
}