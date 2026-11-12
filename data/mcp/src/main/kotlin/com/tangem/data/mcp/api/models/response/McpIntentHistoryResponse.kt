package com.tangem.data.mcp.api.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One page of the intents the agents raised for a wallet, as the Notification Service keeps them. */
@Serializable
internal data class McpIntentHistoryResponse(
    @SerialName("intents") val intents: List<McpIntentHistoryItemResponse>,
    /** How many intents the wallet has in all, not only on this page. */
    @SerialName("total") val total: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("offset") val offset: Int,
)

@Serializable
internal data class McpIntentHistoryItemResponse(
    @SerialName("intentId") val intentId: String,
    /** The service that owns the intent: `transaction` or `express`. */
    @SerialName("source") val source: String,
    /** `CREATED` or `COMPLETED`. */
    @SerialName("status") val status: String,
    @SerialName("sessionId") val sessionId: String,
    /** When the intent was created, in epoch seconds. */
    @SerialName("createdAt") val createdAt: Long,
)