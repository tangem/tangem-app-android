package com.tangem.domain.mcp.models

/**
 * Who is asking for access behind a user code.
 *
 * [clientId], [scope] and [state] are echoed back when the verification is approved — the app never
 * picks a client or narrows a scope itself.
 */
data class McpDeviceVerification(
    val clientId: String,
    val clientOrigin: String?,
    val clientName: String?,
    val isClientNameVerified: Boolean?,
    val scope: String,
    val state: String,
    val userCode: String,
)