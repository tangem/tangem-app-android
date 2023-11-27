package com.tangem.datasource.api.express.models.request

import com.squareup.moshi.Json

data class AssetsRequestBody(
    @Json(name = "tokensList") val tokensList: List<LeastTokenInfo>?,
)