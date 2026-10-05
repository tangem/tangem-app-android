package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetTokenByCustomerWalletRequest(
    @SerialName("auth_type") val authType: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("signature") val signature: String,
    @SerialName("message_format") val messageFormat: String,
)