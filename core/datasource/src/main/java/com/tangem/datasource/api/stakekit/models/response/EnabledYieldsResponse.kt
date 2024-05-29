package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.stakekit.models.response.model.Yield

@JsonClass(generateAdapter = true)
data class EnabledYieldsResponse(
    @Json(name = "data")
    val data: List<Yield>,
    @Json(name = "hasNextPage")
    val hasNextPage: Boolean,
    @Json(name = "limit")
    val limit: Int,
    @Json(name = "page")
    val page: Int,
)