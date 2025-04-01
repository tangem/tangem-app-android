package com.tangem.features.send.v2.send.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
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

    /** Transaction send screen opened */
    data class TransactionScreenOpened(
        val token: String,
        val feeType: AnalyticsParam.FeeType,
    ) : SendAnalyticEvents(
        event = "Transaction Sent Screen Opened",
        params = mapOf(
            TOKEN_PARAM to token,
            FEE_TYPE to feeType.value,
        ),
    )

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

    /** Close button clicked */
    data class CloseButtonClicked(
        val source: SendScreenSource,
        val isFromSummary: Boolean,
        val isValid: Boolean,
    ) : SendAnalyticEvents(
        event = "Button - Close",
        params = mapOf(
            SOURCE to source.name,
            "FromSummary" to if (isFromSummary) "Yes" else "No",
            "isValid" to if (isValid) "Yes" else "No",
        ),
    )

    /** Share button clicked */
    data object ShareButtonClicked : SendAnalyticEvents(event = "Button - Share")

    /** Expore button clicked */
    data object ExploreButtonClicked : SendAnalyticEvents(event = "Button - Explore")

    /** Screen reopened from confirmation screen */
    data class ScreenReopened(val source: SendScreenSource) : SendAnalyticEvents(
        event = "Screen Reopened",
        params = mapOf(SOURCE to source.name),
    )

    companion object {
        const val SEND_CATEGORY = "Token / Send"
    }

    internal enum class SendScreenSource {
        Address,
        Amount,
        Fee,
        Confirm,
    }
}