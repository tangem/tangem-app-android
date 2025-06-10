package com.tangem.domain.tokens.model

import com.tangem.domain.models.quote.QuoteStatus

/**
 * Represents a domain model for a token (as part of a general list of tokens).
 *
 * @property id             The unique identifier of the token.
 * @property networks       List of networks associated with the token.
 * @property isAvailable    Indicates whether this token is supported in Tangem app.
 * @property quoteStatus    Equivalent prices for the token.
 * @property name           The name of the token.
 * @property symbol         The brief name of the token, e.g., "BTC".
 * @property iconUrl        URL of the token's icon.
 */
data class Token(
    val id: String,
    val networks: List<Network>,
    val isAvailable: Boolean,
    val quoteStatus: QuoteStatus?,
    val name: String,
    val symbol: String,
    val iconUrl: String,
) {

    /**
     * Represents a domain model for a network associated with a token.
     *
     * @property networkId           The unique identifier of the network.
     * @property standardType        The type of blockchain associated with the network.
     * @property name                The full name of the network.
     * @property address             The contract address of the token on the current network.
     *                              It is null for the main currencies of the network.
     * @property iconUrl             URL of the network's icon.
     * @property decimalCount        The decimal count associated with the token on the network.
     */
    data class Network(
        val networkId: String,
        val standardType: String,
        val name: String,
        val address: String?,
        val iconUrl: String,
        val decimalCount: Int?,
    )
}