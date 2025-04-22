package com.tangem.datasource.api.visa.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetAccessTokenByCardWalletRequest(
    @Json(name = "auth_type") val authType: String = "card_wallet",
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "signature") val signature: String,
)