package com.tangem.tap.features.customtoken.impl.domain.models

/**
 * Found token model
 *
 * @property id       id
 * @property name     name
 * @property symbol   symbol
 * @property isActive flag that determines status of token
 * @property network  network
 *
[REDACTED_AUTHOR]
 */
data class FoundToken(
    val id: String,
    val name: String,
    val symbol: String,
    val isActive: Boolean,
    val network: Network,
) {

    /**
     * Found token network
     *
     * @property id           id
     * @property address      address
     * @property decimalCount decimal count
     */
    data class Network(val id: String, val address: String, val decimalCount: String)
}