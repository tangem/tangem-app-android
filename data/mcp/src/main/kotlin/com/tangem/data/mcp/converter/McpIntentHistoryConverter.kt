package com.tangem.data.mcp.converter

import com.tangem.data.mcp.api.models.response.McpIntentHistoryItemResponse
import com.tangem.data.mcp.api.models.response.McpIntentHistoryResponse
import com.tangem.domain.mcp.models.McpIntentHistoryItem
import com.tangem.domain.mcp.models.McpIntentHistoryPage
import com.tangem.domain.mcp.models.McpIntentStatus
import com.tangem.domain.mcp.models.McpRequest.Intent.Source
import java.time.Instant

private const val SOURCE_TRANSACTION = "transaction"
private const val SOURCE_EXPRESS = "express"
private const val TYPE_SEND = "send"
private const val TYPE_EXCHANGE = "exchange"

private const val STATUS_CREATED = "CREATED"
private const val STATUS_COMPLETED = "COMPLETED"

/**
 * Unsupported source/type/status combinations are dropped because the app has no screen to open them with.
 */
internal fun McpIntentHistoryResponse.toDomain(): McpIntentHistoryPage = McpIntentHistoryPage(
    items = items.mapNotNull(McpIntentHistoryItemResponse::toDomainOrNull),
    nextPage = (meta.page + 1).takeIf { meta.hasNext },
)

private fun McpIntentHistoryItemResponse.toDomainOrNull(): McpIntentHistoryItem? {
    val intentSource = when {
        source == SOURCE_TRANSACTION && type == TYPE_SEND -> Source.Transaction(Source.Transaction.Type.SEND)
        source == SOURCE_EXPRESS && type == TYPE_EXCHANGE -> Source.Express(Source.Express.Type.EXCHANGE)
        else -> return null
    }
    val intentStatus = when (status) {
        STATUS_CREATED -> McpIntentStatus.Created
        STATUS_COMPLETED -> McpIntentStatus.Completed
        else -> return null
    }
    val createdAtMillis = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrNull() ?: return null

    return McpIntentHistoryItem(
        intentId = intentId,
        source = intentSource,
        status = intentStatus,
        sessionId = sessionId,
        createdAtMillis = createdAtMillis,
    )
}