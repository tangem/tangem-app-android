package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from `GET /v1/order` (findOrders) — array of orders matching the requested
 * `order_types` / `order_statuses` filters.
 *
 * Each order shares the same shape as the single-order [OrderResponse.Result].
 */
@JsonClass(generateAdapter = true)
data class FindOrdersResponse(
    @Json(name = "result") val result: List<OrderResponse.Result>,
)