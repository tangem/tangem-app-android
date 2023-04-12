package com.tangem.tap.features.tokens.domain.models

import com.tangem.blockchain.common.Blockchain

/**
 * Domain model of token for tokens list screen
 *
 * @property id       token id
 * @property name     token name
 * @property symbol   token brief name. Example, "BTC"
 * @property iconUrl  token icon url
 * @property networks token networks
 *
* [REDACTED_AUTHOR]
 */
internal data class Token(
    val id: String,
    val name: String,
    val symbol: String,
    val iconUrl: String,
    val networks: List<Network>,
) {

    /**
     * Domain model of network for tokens list screen
     *
     * @property id           network id
     * @property blockchain   blockchain
     * @property address      address. If address equals null, it means it is the main network of token
     * @property iconUrl      network icon url
     * @property decimalCount decimal count
     *
* [REDACTED_AUTHOR]
     */
    data class Network(
        val id: String,
        val blockchain: Blockchain,
        val address: String?,
        val iconUrl: String,
        val decimalCount: Int?,
    )
}
