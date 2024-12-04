package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json

data class Asset(
    @Json(name = "contractAddress")
    val contractAddress: String,

    @Json(name = "network")
    val network: String,

    @Json(name = "exchangeAvailable")
    val exchangeAvailable: Boolean,

    @Json(name = "onrampAvailable")
    val onrampAvailable: Boolean?,
)
