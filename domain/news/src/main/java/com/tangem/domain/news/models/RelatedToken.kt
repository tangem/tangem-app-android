package com.tangem.domain.news.models

import kotlinx.serialization.Serializable

/**
 * Represents related tokens for article.
 *
[REDACTED_AUTHOR]
 * @param id - unique identifier of the token
 * @param symbol - token symbol(BTC, ETH etc.)
 * @param name - token name
 */
@Serializable
data class RelatedToken(
    val id: String,
    val symbol: String,
    val name: String,
)