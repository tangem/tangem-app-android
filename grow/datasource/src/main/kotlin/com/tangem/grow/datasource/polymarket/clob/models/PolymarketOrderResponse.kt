package com.tangem.grow.datasource.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * CLOB `GET /data/order/{id}`. [status] and the size units are described by
 * [com.tangem.grow.datasource.polymarket.clob.PolymarketClobOrderFormat]; [price]
 * is a decimal string.
 */
@JsonClass(generateAdapter = true)
data class PolymarketOrderResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "price") val price: String?,
    @Json(name = "original_size") val originalSize: String?,
    @Json(name = "size_matched") val sizeMatched: String?,
)