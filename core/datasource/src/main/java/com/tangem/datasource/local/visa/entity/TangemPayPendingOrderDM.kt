package com.tangem.datasource.local.visa.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TangemPayPendingOrderDM(
    @Json(name = "order_id") val orderId: String,
    @Json(name = "user_wallet_id") val userWalletId: String,
    @Json(name = "card_id") val cardId: String,
    @Json(name = "type") val type: String,
    @Json(name = "status") val status: String,
)