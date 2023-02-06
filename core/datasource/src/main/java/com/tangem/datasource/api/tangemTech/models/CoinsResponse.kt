package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import java.math.BigDecimal

data class CoinsResponse(
    @Json(name = "imageHost") val imageHost: String?,
    @Json(name = "coins") val coins: List<Coin>,
    @Json(name = "total") val total: Int,
) {

    data class Coin(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "active") val active: Boolean,
        @Json(name = "networks") val networks: List<Network> = listOf(),
    ) {

        data class Network(
            @Json(name = "networkId") val networkId: String,
            @Json(name = "contractAddress") val contractAddress: String? = null,
            @Json(name = "decimalCount") val decimalCount: BigDecimal? = null,
            @Json(name = "exchangeable") val exchangeable: Boolean? = false,
        )
    }
}
