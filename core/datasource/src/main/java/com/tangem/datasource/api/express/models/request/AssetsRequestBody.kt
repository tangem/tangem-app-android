package com.tangem.datasource.api.express.models.request

import com.squareup.moshi.Json

data class AssetsRequestBody(
    @Json(name = "filter") val filter: List<LeastTokenInfo>?,
)