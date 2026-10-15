package com.tangem.data.mcp.converter

import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.data.mcp.api.models.response.McpDeviceVerificationResponse
import com.tangem.data.mcp.api.models.response.McpSessionResponse
import com.tangem.utils.logging.TangemLogger
import java.time.Instant
import com.tangem.data.mcp.api.models.response.McpDeviceVerificationResult as McpDeviceVerificationResultResponse

private const val TAG = "MCP"

internal fun McpDeviceVerificationResponse.toDomain(): McpDeviceVerification = McpDeviceVerification(
    clientId = clientId,
    clientOrigin = clientOrigin,
    clientName = clientName,
    clientIconUrl = clientIconUrl,
    scope = scope,
    state = state,
    userCode = userCode,
)

internal fun McpDeviceVerificationResultResponse.toDomain(): McpDeviceVerificationResult =
    McpDeviceVerificationResult(isSuccess = isSuccess)

internal fun McpSessionResponse.toDomain(): McpSession = McpSession(
    id = id,
    walletId = UserWalletId(stringValue = walletId),
    createdAtMillis = createdAt.toEpochMillis(),
    clientId = clientId,
    clientOrigin = clientOrigin,
    clientName = clientName,
    clientIconUrl = clientIconUrl,
    scopes = scopes,
    clientIp = clientIp,
    clientCountry = clientCountry,
    clientUserAgent = clientUserAgent,
)

private fun String.toEpochMillis(): Long = runCatching { Instant.parse(this).toEpochMilli() }
    .onFailure { TangemLogger.withTag(TAG).e("Failed to parse the session creation time '$this'", it) }
    .getOrDefault(defaultValue = 0L)