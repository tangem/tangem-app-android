package com.tangem.domain.tokens.model

/**
 * Found token model
 *
 * @property id                 id
 * @property name               name
 * @property symbol             symbol
 * @property decimals           decimals
 * @property contractAddress    contractAddress
 */
data class FoundToken(
    val id: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val contractAddress: String,
)