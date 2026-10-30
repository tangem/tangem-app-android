package com.tangem.domain.mcp.models.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.mcp.models.McpSession

private const val MCP_SESSIONS_CATEGORY = "MCP Sessions"

private const val AGENT_NAME = "Agent Name"
private const val UNKNOWN_AGENT_NAME = "Unknown"

sealed class McpSessionsAnalyticsEvent(
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(MCP_SESSIONS_CATEGORY, event, params) {

    class AgentDisconnected(session: McpSession) : McpSessionsAnalyticsEvent(
        event = "Agent Disconnected",
        params = mapOf(AGENT_NAME to (session.clientOrigin ?: session.clientName ?: UNKNOWN_AGENT_NAME)),
    )
}