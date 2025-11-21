package com.tangem.datasource.api.news.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsDetailsResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "score") val score: Double,
    @Json(name = "language") val language: String,
    @Json(name = "isTrending") val isTrending: Boolean,
    @Json(name = "categories") val categories: List<NewsCategoryDto>,
    @Json(name = "relatedTokens") val relatedTokens: List<NewsRelatedTokenDto>,
    @Json(name = "title") val title: String,
    @Json(name = "newsUrl") val newsUrl: String,
    @Json(name = "shortContent") val shortContent: String,
    @Json(name = "content") val content: String,
    @Json(name = "originalArticles") val originalArticles: List<NewsOriginalArticleDto>,
)