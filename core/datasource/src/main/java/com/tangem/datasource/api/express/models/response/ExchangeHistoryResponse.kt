package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeHistoryResponse(
    @Json(name = "items")
    val items: List<ExchangeItemResponse>,
    @Json(name = "pagination")
    val pagination: ExpressPagination,
)

@JsonClass(generateAdapter = true)
data class ExchangeHistoryDeltaResponse(
    @Json(name = "items")
    val items: List<ExchangeItemResponse>,
    @Json(name = "pagination")
    val pagination: ExpressPaginationDelta,
)