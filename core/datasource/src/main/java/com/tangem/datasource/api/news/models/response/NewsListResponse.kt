package com.tangem.datasource.api.news.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsListResponse(
    @Json(name = "meta") val meta: NewsListMetaDto,
    @Json(name = "items") val items: List<NewsArticleDto>,
)

@JsonClass(generateAdapter = true)
data class NewsListMetaDto(
    @Json(name = "page") val page: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "total") val total: Long,
    @Json(name = "hasNext") val hasNext: Boolean,
    @Json(name = "asOf") val asOf: String,
)