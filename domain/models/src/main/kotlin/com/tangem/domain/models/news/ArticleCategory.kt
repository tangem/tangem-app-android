package com.tangem.domain.models.news

import kotlinx.serialization.Serializable

/**
 * Represents an article category.
 *
[REDACTED_AUTHOR]
 * @param id - unique identifier of the category
 * @param name - category name
 */
@Serializable
data class ArticleCategory(
    val id: Int,
    val name: String,
)