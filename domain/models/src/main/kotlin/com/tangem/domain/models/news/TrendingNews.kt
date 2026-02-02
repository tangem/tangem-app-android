package com.tangem.domain.models.news

import kotlinx.serialization.Serializable

/**
 * Represents the result of fetching news.
 * Can contain either data or an error.
 */
@Serializable
sealed interface TrendingNews {
    @Serializable
    data class Data(val articles: List<ShortArticle>) : TrendingNews

    @Serializable
    data class Error(val error: NewsError) : TrendingNews
}