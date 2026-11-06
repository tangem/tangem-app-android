package com.tangem.data.txhistory.repository

import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.utils.logging.TangemLogger

/**
 * Prepends the still-unconfirmed [recentTransactions] the API page doesn't know about yet, newest first.
 */
internal fun PaginationWrapper<TxInfo>.withRecentTransactions(
    recentTransactions: List<TxInfo>,
): PaginationWrapper<TxInfo> {
    val recentItems = recentTransactions
        .filter { it.status == TxInfo.TransactionStatus.Unconfirmed }
        .sortedByDescending { it.timestampInMillis }
        .filter { recent -> items.none { it.txHash == recent.txHash } }

    if (recentItems.isEmpty()) {
        TangemLogger.d("Nothing to add to TxHistory")
        return this
    }

    TangemLogger.d(
        "Recent transactions were added to TxHistory: ${recentItems.joinToString(
            prefix = "[",
            postfix = "]",
        ) { it.txHash }}",
    )
    return copy(items = recentItems + items)
}