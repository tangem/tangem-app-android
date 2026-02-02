package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PromoBannerV2Response(
    @Json(name = "promotions")
    val promotions: List<PromoBannerResponse>,
)