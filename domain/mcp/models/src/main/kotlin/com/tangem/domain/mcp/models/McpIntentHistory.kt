package com.tangem.domain.mcp.models

/** An intent an MCP agent raised, as the history lists it: who asked for what and where it stands. */
data class McpIntentHistoryItem(
    val intentId: String,
    val source: McpRequest.Intent.Source,
    val status: McpIntentStatus,
    val sessionId: String,
    val createdAtMillis: Long,
)

enum class McpIntentStatus {
    /** Waiting for the owner to act on it. */
    Created,

    /** The owner has already handled it. */
    Completed,
}

/**
 * One page of the intent history.
 *
 * [nextOffset] is where the following page starts on the service, or `null` once the last page is read. It is not
 * `offset + items.size`, because items the app does not support are dropped from [items].
 */
data class McpIntentHistoryPage(
    val items: List<McpIntentHistoryItem>,
    val nextOffset: Int?,
)