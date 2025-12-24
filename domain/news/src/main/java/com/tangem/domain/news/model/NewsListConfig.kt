package com.tangem.domain.news.model

import kotlinx.serialization.Serializable

/**
 * Represents config for request of news list.
 *
[REDACTED_AUTHOR]
 *
 * @param language device locale (ex: en, ru).
 * @param snapshot id snapshot (`meta.asOf`) to stabilize responses.
 * @param tokenIds filter by tokens.
 * @param categoryIds filter by category.
 */
@Serializable
data class NewsListConfig(
    val language: String,
    val snapshot: String?,
    val tokenIds: List<String> = emptyList(),
    val categoryIds: List<Int> = emptyList(),
)