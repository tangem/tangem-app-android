package com.tangem.datasource.api.news.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsTrendingResponse(
    @Json(name = "meta") val meta: NewsTrendingMetaDto,
    @Json(name = "items") val items: List<NewsArticleDto>,
)

@JsonClass(generateAdapter = true)
data class NewsTrendingMetaDto(
    @Json(name = "limit") val limit: Int,
)