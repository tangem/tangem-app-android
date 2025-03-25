package com.tangem.features.send.v2.send.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

/**
 * Send screen analytics
 */
internal sealed class SendAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = SEND_CATEGORY, event = event, params = params) {

    /** Recipient address screen opened */
    data object AddressScreenOpened : SendAnalyticEvents(event = "Address Screen Opened")

    /** Amount screen opened */
    data object AmountScreenOpened : SendAnalyticEvents(event = "Amount Screen Opened")

    /** Fee screen opened */
    data object FeeScreenOpened : SendAnalyticEvents(event = "Fee Screen Opened")

    /** Confirmation screen opened */
    data object ConfirmationScreenOpened : SendAnalyticEvents(event = "Confirm Screen Opened")

    /** If transaction delays notification is present */
    data class NoticeTransactionDelays(
        val token: String,
    ) : SendAnalyticEvents(
        event = "Notice - Transaction Delays Are Possible",
        params = mapOf(TOKEN_PARAM to token),
    )

    /** If error occurs during send transactions */
    data class TransactionError(val token: String) : SendAnalyticEvents(
        event = "Error - Transaction Rejected",
        params = mapOf(TOKEN_PARAM to token),
    )

    companion object {
        const val SEND_CATEGORY = "Token / Send"
    }
}