package com.tangem.grow.datasource.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketPlaceOrderResponse(
    @Json(name = "success") val isSuccess: Boolean,
    @Json(name = "orderID") val orderId: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "makingAmount") val makingAmount: String?,
    @Json(name = "takingAmount") val takingAmount: String?,
    @Json(name = "transactionsHashes") val transactionHashes: List<String>?,
    @Json(name = "errorMsg") val errorMessage: String?,
)