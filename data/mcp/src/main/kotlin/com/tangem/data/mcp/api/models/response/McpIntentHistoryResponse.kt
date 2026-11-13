package com.tangem.data.mcp.api.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One page of the intents the agents raised for a wallet, as the Notification Service keeps them. */
@Serializable
internal data class McpIntentHistoryResponse(
    @SerialName("items") val items: List<McpIntentHistoryItemResponse>,
    @SerialName("meta") val meta: McpIntentHistoryMetaResponse,
)

@Serializable
internal data class McpIntentHistoryMetaResponse(
    @SerialName("total") val total: Int,
    @SerialName("page") val page: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("hasNext") val hasNext: Boolean,
)

@Serializable
internal data class McpIntentHistoryItemResponse(
    @SerialName("intentId") val intentId: String,
    /** The service that owns the intent: `transaction` or `express`. */
    @SerialName("source") val source: String,
    @SerialName("type") val type: String,
    /** `CREATED` or `COMPLETED`. */
    @SerialName("status") val status: String,
    @SerialName("sessionId") val sessionId: String,
    @SerialName("createdAt") val createdAt: String,
)