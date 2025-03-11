package com.tangem.datasource.api.express.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PairsRequestBody(
    @Json(name = "from")
    val from: List<LeastTokenInfo>,

    @Json(name = "to")
    val to: List<LeastTokenInfo>,
)