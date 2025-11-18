package com.tangem.datasource.api.news.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsCategoriesResponse(
    @Json(name = "items") val items: List<NewsCategoryDto>,
)