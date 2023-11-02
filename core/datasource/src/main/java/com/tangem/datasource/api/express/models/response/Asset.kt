package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json

data class Asset(
    @Json(name = "contractAddress")
    val contractAddress: String,

    @Json(name = "network")
    val network: String,

    @Json(name = "token")
    val token: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "decimals")
    val decimals: Int,

    @Json(name = "isActive")
    val isActive: Boolean,

    @Json(name = "exchangeAvailable")
    val exchangeAvailable: Boolean,
)