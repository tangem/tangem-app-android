package com.tangem.data.mcp.converter

import com.tangem.data.mcp.api.models.response.McpIntentHistoryItemResponse
import com.tangem.data.mcp.api.models.response.McpIntentHistoryResponse
import com.tangem.domain.mcp.models.McpIntentHistoryItem
import com.tangem.domain.mcp.models.McpIntentHistoryPage
import com.tangem.domain.mcp.models.McpIntentStatus
import com.tangem.domain.mcp.models.McpRequest.Intent.Source
import java.util.concurrent.TimeUnit

private const val SOURCE_TRANSACTION = "transaction"
private const val SOURCE_EXPRESS = "express"

private const val STATUS_CREATED = "CREATED"
private const val STATUS_COMPLETED = "COMPLETED"

/**
 * The service names the source only, and each source has a single supported operation, so it also says which one
 * the intent is. An intent of a source or status the app does not know is dropped: there is no screen to open it with.
 */
internal fun McpIntentHistoryResponse.toDomain(): McpIntentHistoryPage {
    val consumed = offset + intents.size

    return McpIntentHistoryPage(
        items = intents.mapNotNull(McpIntentHistoryItemResponse::toDomainOrNull),
        nextOffset = consumed.takeIf { intents.isNotEmpty() && it < total },
    )
}

private fun McpIntentHistoryItemResponse.toDomainOrNull(): McpIntentHistoryItem? {
    val intentSource = when (source) {
        SOURCE_TRANSACTION -> Source.Transaction(Source.Transaction.Type.SEND)
        SOURCE_EXPRESS -> Source.Express(Source.Express.Type.EXCHANGE)
        else -> return null
    }
    val intentStatus = when (status) {
        STATUS_CREATED -> McpIntentStatus.Created
        STATUS_COMPLETED -> McpIntentStatus.Completed
        else -> return null
    }

    return McpIntentHistoryItem(
        intentId = intentId,
        source = intentSource,
        status = intentStatus,
        sessionId = sessionId,
        createdAtMillis = TimeUnit.SECONDS.toMillis(createdAt),
    )
}