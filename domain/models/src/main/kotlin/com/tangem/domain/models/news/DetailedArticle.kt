package com.tangem.domain.models.news

import kotlinx.serialization.Serializable

/**
 * Represents an detailed article via news.
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
 * @param shortContent - short description
 * @param content - main text of the article
 * @param originalArticles - original articles, which were base to build detailed article.
 * @param isLiked - flag indicating whether the news is liked or not
 */
@Serializable
data class DetailedArticle(
    val id: Int,
    val createdAt: String,
    val score: Float,
    val locale: String,
    val isTrending: Boolean,
    val categories: List<ArticleCategory>,
    val relatedTokens: List<RelatedToken>,
    val title: String,
    val newsUrl: String,
    val shortContent: String,
    val content: String,
    val originalArticles: List<OriginalArticle>,
    val isLiked: Boolean,
)