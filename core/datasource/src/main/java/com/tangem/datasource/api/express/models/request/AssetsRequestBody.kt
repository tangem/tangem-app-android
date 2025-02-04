package com.tangem.datasource.api.express.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AssetsRequestBody(
    @Json(name = "tokensList") val tokensList: List<LeastTokenInfo>?,
)