package com.tangem.datasource.api.onramp.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.express.models.response.ExpressPagination
import com.tangem.datasource.api.express.models.response.ExpressPaginationDelta

@JsonClass(generateAdapter = true)
data class OnrampHistoryResponse(
    @Json(name = "items")
    val items: List<OnrampItemResponse>,
    @Json(name = "pagination")
    val pagination: ExpressPagination,
)

@JsonClass(generateAdapter = true)
data class OnrampHistoryDeltaResponse(
    @Json(name = "items")
    val items: List<OnrampItemResponse>,
    @Json(name = "pagination")
    val pagination: ExpressPaginationDelta,
)