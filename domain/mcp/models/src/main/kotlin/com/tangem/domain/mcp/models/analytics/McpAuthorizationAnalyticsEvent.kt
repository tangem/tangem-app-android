package com.tangem.domain.mcp.models.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.domain.mcp.models.McpDeviceVerification

private const val MCP_AUTHORIZATION_CATEGORY = "MCP Authorization"

private const val AGENT_NAME = "Agent Name"
private const val UNKNOWN_AGENT_NAME = "Unknown"

/** Pairing of an MCP agent with a wallet, from the authorization link to the agent being connected. */
sealed class McpAuthorizationAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(MCP_AUTHORIZATION_CATEGORY, event, params) {

    class ConnectionReceived : McpAuthorizationAnalyticsEvent(event = "Connection Received")

    class AgentConnectionRequested(verification: McpDeviceVerification) : McpAuthorizationAnalyticsEvent(
        event = "Agent Connection Requested",
        params = verification.agentNameParams(),
    )

    class ButtonApprove(verification: McpDeviceVerification) : McpAuthorizationAnalyticsEvent(
        event = "Button - Approve",
        params = verification.agentNameParams(),
    )

    class AgentConnected(verification: McpDeviceVerification) : McpAuthorizationAnalyticsEvent(
        event = "Agent Connected",
        params = verification.agentNameParams(),
    )

    /** Sent for the Decline button and for the sheet closed without an answer alike. */
    class ButtonDecline(verification: McpDeviceVerification) : McpAuthorizationAnalyticsEvent(
        event = "Button - Decline",
        params = verification.agentNameParams(),
    )

    class AuthorizationFailed(errorCode: String) : McpAuthorizationAnalyticsEvent(
        event = "Authorization Failed",
        params = mapOf(ERROR_CODE to errorCode),
    )

    class NoticePushPermissionRequired : McpAuthorizationAnalyticsEvent(event = "Notice - Push Permission Required")
}

// The origin goes first, as in the intent events: it is the verified part of the agent's identity.
private fun McpDeviceVerification.agentNameParams(): Map<String, String> =
    mapOf(AGENT_NAME to (clientOrigin ?: clientName ?: UNKNOWN_AGENT_NAME))