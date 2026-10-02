package com.tangem.data.mcp.converter

import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.lib.auth.api.models.response.McpDeviceVerificationResponse
import com.tangem.lib.auth.api.models.response.McpSessionResponse
import com.tangem.lib.auth.api.models.response.McpDeviceVerificationResult as McpDeviceVerificationResultResponse

internal fun McpDeviceVerificationResponse.toDomain(): McpDeviceVerification = McpDeviceVerification(
    clientId = clientId,
    clientOrigin = clientOrigin,
    clientName = clientName,
    isClientNameVerified = isClientNameVerified,
    scope = scope,
    state = state,
    userCode = userCode,
)

internal fun McpDeviceVerificationResultResponse.toDomain(): McpDeviceVerificationResult =
    McpDeviceVerificationResult(isSuccess = isSuccess)

internal fun McpSessionResponse.toDomain(): McpSession = McpSession(
    id = id,
    clientId = clientId,
    clientOrigin = clientOrigin,
    clientName = clientName,
    scopes = scopes,
    clientIp = clientIp,
    clientCountry = clientCountry,
    clientUserAgent = clientUserAgent,
)