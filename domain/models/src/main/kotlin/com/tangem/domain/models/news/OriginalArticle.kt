package com.tangem.domain.models.news

import kotlinx.serialization.Serializable

/**
 * Represents an original article, which was base to build detailed article.
 *
[REDACTED_AUTHOR]
 * @param id - unique identifier of the article
 * @param title - article title
 * @param sourceName - name of original article source
 * @param locale - language of the article
 * @param publishedAt - date of article publishing
 * @param url - link to source of original article
 * @param imageUrl - image from source of original article
 */
@Serializable
data class OriginalArticle(
    val id: Int,
    val title: String,
    val sourceName: String,
    val locale: String,
    val publishedAt: String,
    val url: String,
    val imageUrl: String?,
)