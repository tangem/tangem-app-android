package com.tangem.domain.models.news

import kotlinx.serialization.Serializable

/**
 * Represents an related article, which was base to build detailed article.
 *
[REDACTED_AUTHOR]
 * @param id - unique identifier of the article
 * @param title - article title
 * @param media - object of media name and identifier
 * @param locale - language of the article
 * @param publishedAt - date of article publishing
 * @param url - link to source of original article
 * @param imageUrl - image from source of original article
 */
@Serializable
data class RelatedArticle(
    val id: Int,
    val title: String,
    val media: Media,
    val locale: String,
    val publishedAt: String,
    val url: String,
    val imageUrl: String?,
)

@Serializable
data class Media(
    val id: Int,
    val name: String,
)