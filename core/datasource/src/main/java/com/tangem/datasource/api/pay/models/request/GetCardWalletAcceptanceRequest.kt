package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetCardWalletAcceptanceRequest(
    @Json(name = "type") val type: String = "card_wallet",
    @Json(name = "customer_wallet_address") val customerWalletAddress: String,
    @Json(name = "card_wallet_address") val cardWalletAddress: String,
)