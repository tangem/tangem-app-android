package com.tangem.features.mcp.impl.pair.model

import com.tangem.core.ui.extensions.stringReference

/**
 * MCP copy has no Lokalise keys yet, and the agent metadata has no backend contract either —
 * `McpRequest.Pair` carries only the user code. Kept in one place so replacing the literals with
 * `resourceReference`s stays a single-file change.
 */
internal object McpPairTexts {

    val connectionTitle = stringReference("New connection")
    val decline = stringReference("Decline")
    val connectFailed = stringReference("Failed to connect the agent")
    val agentName = stringReference("MCP Agent")

    val pushNotificationsWarning = stringReference(
        "You'll get a notification to approve each transaction. Without notifications, the agent can't connect.",
    )
}