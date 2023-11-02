package com.tangem.feature.swap.models.request

import com.squareup.moshi.Json

internal data class LeastTokenInfo(
    @Json(name = "contractAddress")
    val contractAddress: String,

    @Json(name = "network")
    val network: String,
)