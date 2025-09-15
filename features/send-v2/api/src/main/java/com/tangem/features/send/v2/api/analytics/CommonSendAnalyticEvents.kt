package com.tangem.features.send.v2.api.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

/**
 * Send analytics
 */
sealed class CommonSendAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    /** Recipient address screen opened */
    data class AddressScreenOpened(
        val categoryName: String,
    ) : CommonSendAnalyticEvents(category = categoryName, event = "Address Screen Opened")

    /** Amount screen opened */
    data class AmountScreenOpened(
        val categoryName: String,
    ) : CommonSendAnalyticEvents(category = categoryName, event = "Amount Screen Opened")

    /** Fee screen opened */
    data class FeeScreenOpened(
        val categoryName: String,
    ) : CommonSendAnalyticEvents(category = categoryName, event = "Fee Screen Opened")

    /** Confirmation screen opened */
    data class ConfirmationScreenOpened(
        val categoryName: String,
    ) : CommonSendAnalyticEvents(category = categoryName, event = "Confirm Screen Opened")

    /** If transaction delays notification is present */
    data class NoticeTransactionDelays(
        val categoryName: String,
        val token: String,
    ) : CommonSendAnalyticEvents(
        category = categoryName,
        event = "Notice - Transaction Delays Are Possible",
        params = mapOf(TOKEN_PARAM to token),
    )

    /** If error occurs during send transactions */
    data class TransactionError(
        val categoryName: String,
        val token: String,
        val blockchain: String,
    ) : CommonSendAnalyticEvents(
        category = categoryName,
        event = "Error - Transaction Rejected",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    /** Close button clicked */
    data class CloseButtonClicked(
        val categoryName: String,
        val source: SendScreenSource,
        val isFromSummary: Boolean,
        val isValid: Boolean,
    ) : CommonSendAnalyticEvents(
        category = categoryName,
        event = "Button - Close",
        params = mapOf(
            SOURCE to source.name,
            "FromSummary" to if (isFromSummary) "Yes" else "No",
            "isValid" to if (isValid) "Yes" else "No",
        ),
    )

    /** Share button clicked */
    data class ShareButtonClicked(
        val categoryName: String,
    ) : CommonSendAnalyticEvents(
        category = categoryName,
        event = "Button - Share",
    )

    /** Expore button clicked */
    data class ExploreButtonClicked(
        val categoryName: String,
    ) : CommonSendAnalyticEvents(
        category = categoryName,
        event = "Button - Explore",
    )

    /** Screen reopened from confirmation screen */
    data class ScreenReopened(
        val categoryName: String,
        val source: SendScreenSource,
    ) : CommonSendAnalyticEvents(
        category = categoryName,
        event = "Screen Reopened",
        params = mapOf(SOURCE to source.name),
    )

    /** Fee screen is closed with non empty nonce */
    data class NonceInserted(
        val categoryName: String,
        val token: String,
        val blockchain: String,
    ) : CommonSendAnalyticEvents(
        category = categoryName,
        event = "Nonce Inserted",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    /** Token chosen to convert with sending */
    data class TokenChosen(
        val categoryName: String,
        val token: String,
        val blockchain: String,
    ) : CommonSendAnalyticEvents(
        category = categoryName,
        event = "Token chosen",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    companion object {
        const val SEND_CATEGORY = "Token / Send"
        const val NFT_SEND_CATEGORY = "NFT"
    }

    enum class SendScreenSource {
        Address,
        Amount,
        Fee,
        Confirm,
    }
}