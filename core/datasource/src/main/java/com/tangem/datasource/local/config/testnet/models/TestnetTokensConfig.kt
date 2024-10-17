package com.tangem.datasource.local.config.testnet.models

import com.squareup.moshi.Json

/**
 * Config model with testnet tokens list
 *
 * @property tokens testnet tokens list
 */
// TODO: use CoinsResponse
data class TestnetTokensConfig(
    @Json(name = "coins") val tokens: List<Token>,
) {

    /**
     * Testnet token model
     *
     * @property id       token id
     * @property name     token name
     * @property symbol   token brief name. Example, "BTC"
     * @property networks networks of testnet token
     */
    data class Token(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "networks") val networks: List<Network>? = null,
    ) {

        /**
         * Network model of testnet token
         *
         * @property id           network id
         * @property address      network address
         * @property decimalCount decimal count
         */
        data class Network(
            @Json(name = "networkId") val id: String,
            @Json(name = "contractAddress") val address: String?,
            @Json(name = "decimalCount") val decimalCount: Int?,
        )
    }
}