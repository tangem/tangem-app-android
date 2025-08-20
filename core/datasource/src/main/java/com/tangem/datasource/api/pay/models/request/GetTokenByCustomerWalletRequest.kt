package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetTokenByCustomerWalletRequest(
    @Json(name = "auth_type") val authType: String = "customer_wallet",
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "signature") val signature: String,
    @Json(name = "message_format") val messageFormat: String,
)