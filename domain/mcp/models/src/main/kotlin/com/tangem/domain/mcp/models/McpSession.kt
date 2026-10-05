package com.tangem.domain.mcp.models

import com.tangem.domain.models.wallet.UserWalletId

/** An authorization issued to an MCP client. */
data class McpSession(
    val id: String,
    /** Wallet the authorization was granted for, as picked by the owner when the agent paired. */
    val walletId: UserWalletId,
    val createdAtMillis: Long,
    val clientId: String,
    val clientOrigin: String?,
    val clientName: String?,
    val clientIconUrl: String?,
    val scopes: List<String>,
    val clientIp: String?,
    val clientCountry: String?,
    val clientUserAgent: String?,
)