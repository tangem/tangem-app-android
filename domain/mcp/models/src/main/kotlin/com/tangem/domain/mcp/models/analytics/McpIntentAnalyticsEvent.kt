package com.tangem.domain.mcp.models.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.REASON
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.models.McpRequest

private const val MCP_INTENT_CATEGORY = "MCP Intent"

private const val INTENT_TYPE = "Intent Type"
private const val INTENT_ID = "Intent Id"
private const val AGENT_NAME = "Agent Name"
private const val INTENT_MODIFIED = "Intent Modified"

private const val UNKNOWN_AGENT_NAME = "Unknown"

sealed class McpIntentAnalyticsEvent(
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(MCP_INTENT_CATEGORY, event, params) {

    class IntentReceived(intentId: String, source: McpRequest.Intent.Source) : McpIntentAnalyticsEvent(
        event = "Intent Received",
        params = mapOf(
            INTENT_TYPE to IntentType.of(source).value,
            SOURCE to source.analyticsValue(),
            INTENT_ID to intentId,
        ),
    )

    /** [blockchain] and [token] are of the currency the intent spends, the FROM side of an exchange. */
    class IntentScreenOpened(
        intentId: String,
        type: IntentType,
        agent: McpAgent,
        blockchain: String,
        token: String,
    ) : McpIntentAnalyticsEvent(
        event = "Intent Screen Opened",
        params = mapOf(
            INTENT_TYPE to type.value,
            AGENT_NAME to (agent.origin ?: agent.name ?: UNKNOWN_AGENT_NAME),
            BLOCKCHAIN to blockchain,
            TOKEN_PARAM to token,
            INTENT_ID to intentId,
        ),
    )

    /** Sent when a processed or unreachable intent is shown instead of the offer. */
    class IntentLoadFailed(
        intentId: String,
        type: IntentType,
        reason: Reason,
        errorCode: String? = null,
    ) : McpIntentAnalyticsEvent(
        event = "Intent Load Failed",
        params = buildMap {
            put(INTENT_TYPE, type.value)
            put(REASON, reason.value)
            errorCode?.let { put(ERROR_CODE, it) }
            put(INTENT_ID, intentId)
        },
    )

    class ButtonContinue(intentId: String, type: IntentType) : McpIntentAnalyticsEvent(
        event = "Button - Continue",
        params = mapOf(INTENT_TYPE to type.value, INTENT_ID to intentId),
    )

    /** Only a send intent is completed by the app: Express links an exchange to its intent on its own. */
    class IntentCompleted(
        intentId: String,
        blockchain: String,
        token: String,
        isModified: Boolean,
    ) : McpIntentAnalyticsEvent(
        event = "Intent Completed",
        params = mapOf(
            INTENT_TYPE to IntentType.Send.value,
            BLOCKCHAIN to blockchain,
            TOKEN_PARAM to token,
            INTENT_MODIFIED to if (isModified) "Yes" else "No",
            INTENT_ID to intentId,
        ),
    )

    class IntentReportFailed(intentId: String, errorCode: String) : McpIntentAnalyticsEvent(
        event = "Intent Report Failed",
        params = mapOf(
            INTENT_TYPE to IntentType.Send.value,
            ERROR_CODE to errorCode,
            INTENT_ID to intentId,
        ),
    )

    enum class IntentType(val value: String) {
        Exchange("exchange"),
        Send("send"),
        ;

        companion object {

            fun of(source: McpRequest.Intent.Source): IntentType = when (source) {
                is McpRequest.Intent.Source.Express -> when (source.type) {
                    McpRequest.Intent.Source.Express.Type.EXCHANGE -> Exchange
                }
                is McpRequest.Intent.Source.Transaction -> when (source.type) {
                    McpRequest.Intent.Source.Transaction.Type.SEND -> Send
                }
            }
        }
    }

    enum class Reason(val value: String) {
        NotFound("Not Found"),
        WrongWallet("Wrong Wallet"),
        Unreachable("Unreachable"),
        AlreadyProcessed("Already Processed"),
        TokenMissing("Token Missing"),
        InvalidAmount("Invalid Amount"),
        SendNotOpened("Send Not Opened"),
        Unknown("Unknown"),
    }
}

/** The `source` value of the deep link that carried the intent. */
private fun McpRequest.Intent.Source.analyticsValue(): String = when (this) {
    is McpRequest.Intent.Source.Express -> "express"
    is McpRequest.Intent.Source.Transaction -> "transaction"
}