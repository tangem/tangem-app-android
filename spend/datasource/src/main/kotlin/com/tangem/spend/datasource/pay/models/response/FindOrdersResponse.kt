package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET /v1/order` (findOrders) — array of orders matching the requested
 * `order_types` / `order_statuses` filters.
 *
 * Each order shares the same shape as the single-order [OrderResponse.Result].
 */
@Serializable
data class FindOrdersResponse(
    @SerialName("result") val result: List<OrderResponse.Result>,
)