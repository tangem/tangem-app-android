package com.tangem.datasource.api.visa.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateNoneByCardWalletRequest(
    @Json(name = "auth_type") val authType: String = "card_wallet",
    @Json(name = "card_wallet_address") val cardWalletAddress: String,
    @Json(name = "card_id") val cardId: String,
)