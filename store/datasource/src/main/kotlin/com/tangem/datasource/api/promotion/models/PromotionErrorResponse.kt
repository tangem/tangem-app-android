package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Problem+json error body; [detail] carries request specifics such as the campaign id. */
@JsonClass(generateAdapter = true)
data class PromotionErrorResponse(
    @Json(name = "title") val title: String?,
    @Json(name = "detail") val detail: String?,
)