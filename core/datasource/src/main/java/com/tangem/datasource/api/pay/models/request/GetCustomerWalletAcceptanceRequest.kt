package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetCustomerWalletAcceptanceRequest(
    @Json(name = "type") val type: String = "customer_wallet",
    @Json(name = "customer_wallet_address") val customerWalletAddress: String,
    @Json(name = "card_wallet_address") val cardWalletAddress: String,
)