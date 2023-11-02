package com.tangem.feature.swap.models.request

import com.squareup.moshi.Json

internal data class AssetsRequestBody(
    @Json(name = "filter") val filter: List<LeastTokenInfo>?,
)
