package com.tangem.spend.datasource.visa.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetAccessTokenByCardIdRequest(
    @SerialName("auth_type") val authType: String = "card_id",
    @SerialName("session_id") val sessionId: String,
    @SerialName("signature") val signature: String,
    @SerialName("salt") val salt: String,
)