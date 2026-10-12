package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetPinCodeRequest(
    @SerialName("order_id") val orderId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("iv") val iv: String,
    @SerialName("pin") val pin: String,
)