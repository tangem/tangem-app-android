package com.tangem.feature.swap.models.request

import com.squareup.moshi.Json

internal data class PairsRequestBody(
    @Json(name = "from")
    val from: List<LeastTokenInfo>,

    @Json(name = "to")
    val to: List<LeastTokenInfo>,
)
