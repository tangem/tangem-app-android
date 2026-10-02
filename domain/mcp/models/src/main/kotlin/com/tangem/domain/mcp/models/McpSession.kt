package com.tangem.domain.mcp.models

/** An authorization issued to an MCP client. */
data class McpSession(
    val id: String,
    val clientId: String,
    val clientOrigin: String?,
    val clientName: String?,
    val scopes: List<String>,
    val clientIp: String?,
    val clientCountry: String?,
    val clientUserAgent: String?,
)