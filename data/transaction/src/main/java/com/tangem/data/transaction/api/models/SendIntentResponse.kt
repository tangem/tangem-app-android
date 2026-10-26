package com.tangem.data.transaction.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SendIntentResponse(
    @SerialName("intentId") val intentId: String,
    @SerialName("status") val status: SendIntentStatus = SendIntentStatus.UNKNOWN,
    @SerialName("networkId") val networkId: String,
    @SerialName("fromAddress") val fromAddress: String,
    @SerialName("toAddress") val toAddress: String,
    @SerialName("tokenId") val tokenId: String,
    /** Decimal number in whole units of the token. */
    @SerialName("amount") val amount: String,
    @SerialName("memo") val memo: String?,
    @SerialName("comment") val comment: String?,
    /** Hash of the transaction the app reported for the intent. */
    @SerialName("txHash") val txHash: String?,
    @SerialName("clientId") val clientId: String?,
    @SerialName("sessionId") val sessionId: String,
)