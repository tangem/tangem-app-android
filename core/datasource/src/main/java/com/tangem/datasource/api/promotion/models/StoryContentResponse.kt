package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StoryContentResponse(
    @Json(name = "imageHost") val imageHost: String,
    @Json(name = "story") val story: StoryResponse,
) {

    @JsonClass(generateAdapter = true)
    data class StoryResponse(
        @Json(name = "id") val id: String,
        @Json(name = "title") val title: String?,
        @Json(name = "slides") val slides: List<StorySlideResponse>?,
    )

    @JsonClass(generateAdapter = true)
    data class StorySlideResponse(
        @Json(name = "id") val id: String,
    )
}