package com.tangem.domain.mcp.models

/** What is known about the AI agent behind an MCP client. */
data class McpAgent(
    val clientId: String,
    /** Host the client's metadata came from: the only verified part of its identity, so show it first. */
    val origin: String?,
    /** Name the client gave itself, not verified: a label, not an identity. */
    val name: String?,
    val iconUrl: String?,
) {

    companion object {

        /** An agent nothing is known about: the sheets show their generic name and icon for it. */
        fun unknown(clientId: String) = McpAgent(clientId = clientId, origin = null, name = null, iconUrl = null)
    }
}