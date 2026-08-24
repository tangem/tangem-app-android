package com.tangem.features.txhistory.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AnalyticsParam.Key.TYPE

private const val TRANSACTION_HISTORY_CATEGORY = "Transaction History"
private const val WALLET_ID = "Wallet Id"
private const val TITLE = "Title"

/** New "Transaction History" events introduced for the in-app transaction detail screen ([REDACTED_TASK_KEY]). */
internal sealed class TxHistoryAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(TRANSACTION_HISTORY_CATEGORY, event, params) {

    /** Sent every time the transaction detail screen is opened, however the user got there. */
    class TransactionDetailScreenOpened(
        walletId: String,
        token: String,
        blockchain: String,
        type: String,
        status: String,
        title: String,
    ) : TxHistoryAnalyticsEvent(
        event = "Transaction Detail Screen Opened",
        params = mapOf(
            WALLET_ID to walletId,
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
            TYPE to type,
            STATUS to status,
            TITLE to title,
        ),
    )

    /** "Transaction ID" row of the detail header's overflow menu. */
    class ButtonCopyTransactionId(
        walletId: String,
        token: String,
        blockchain: String,
        type: String,
    ) : TxHistoryAnalyticsEvent(
        event = "Button - Copy Transaction ID",
        params = mapOf(
            WALLET_ID to walletId,
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
            TYPE to type,
        ),
    )
}