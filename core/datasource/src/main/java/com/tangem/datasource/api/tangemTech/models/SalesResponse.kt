package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json

/**
 * Sales response
 */
data class SalesResponse(
    @Json(name = "sales") val sales: List<Sales>,
)

/**
 * Sales info
 *
 * @property id sales id
 * @property state state as order, sold-out, pre-order
 * @property product product that is sales
 * @property notification optional notification for product
 */
data class Sales(
    @Json(name = "id") val id: String,
    @Json(name = "state") val state: String,
    @Json(name = "product") val product: Product,
    @Json(name = "notification") val notification: Notification?,
)

/**
 * Product
 *
 * @property id product id
 * @property code code that shows what product it is
 * @property name product name
 */
data class Product(
    @Json(name = "id") val id: String,
    @Json(name = "code") val code: String,
    @Json(name = "name") val name: String,
)

data class Notification(
    @Json(name = "type") val type: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
)