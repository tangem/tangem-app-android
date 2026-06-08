package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExpressPagination(
    @Json(name = "endCursor")
    val endCursor: String?,
    @Json(name = "startDeltaCursor")
    val startDeltaCursor: String?,
    @Json(name = "hasNextPage")
    val hasMore: Boolean,
)

@JsonClass(generateAdapter = true)
data class ExpressPaginationDelta(
    @Json(name = "startCursor")
    val startCursor: String?,
    @Json(name = "hasMore")
    val hasMore: Boolean,
)