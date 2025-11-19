package com.tangem.domain.news.models

import kotlinx.serialization.Serializable

/**
 * Represents an short article info in news.
 *
[REDACTED_AUTHOR]
 * @param id - unique identifier of the article

 * @param score - score of article (ex. 6.9)
 * @param locale - language of the article
 * @param isTrending - flag for trending article
 * @param categories - list of included categories
 * @param relatedTokens - tokens, which were mentioned in article
 * @param title - article title
 * @param newsUrl - link for article to share
 */
@Serializable
data class ShortArticle(
    val id: Int,
    val createdAt: String,
    val score: Float,
    val locale: String,
    val categories: List<ArticleCategory>,
    val relatedTokens: List<RelatedToken>,
    val isTrending: Boolean,
    val title: String,
    val newsUrl: String,
)