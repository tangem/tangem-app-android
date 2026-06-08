package com.tangem.core.ui.test

object TransactionHistoryItemTestTags {
    const val ITEM = "TRANSACTION_HISTORY_ITEM"
    const val TITLE = "TRANSACTION_HISTORY_ITEM_TITLE"
    const val AMOUNT = "TRANSACTION_HISTORY_ITEM_AMOUNT"
    const val CURRENCY = "TRANSACTION_HISTORY_ITEM_CURRENCY"

    /** Status is conveyed visually (icon + color), so it is exposed via a status-suffixed tag. */
    const val STATUS_PREFIX = "TRANSACTION_HISTORY_ITEM_STATUS_"
    const val STATUS_CONFIRMED = STATUS_PREFIX + "CONFIRMED"
}