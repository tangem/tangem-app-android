package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetActionsResponse(
    @Json(name = "data")
    val data: List<ActionDTO>,
    @Json(name = "hasNextPage")
    val hasNextPage: Boolean,
    @Json(name = "limit")
    val limit: Int,
    @Json(name = "page")
    val page: Int,
)