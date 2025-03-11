package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PromotionInfoResponse(
    @Json(name = "name") val name: String,
    @Json(name = "all") val bannerState: BannerState?,
) {

    @JsonClass(generateAdapter = true)
    data class BannerState(
        @Json(name = "timeline") val timeline: Timeline,
        @Json(name = "status") val status: String,
        @Json(name = "link") val link: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Timeline(
        @Json(name = "start") val start: String,
        @Json(name = "end") val end: String,
    )
}