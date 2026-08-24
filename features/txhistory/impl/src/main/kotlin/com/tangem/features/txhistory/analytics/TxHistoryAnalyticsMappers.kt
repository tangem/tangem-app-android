package com.tangem.features.txhistory.analytics

import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.txhistory.converter.ExpressExchangeStatusToUiStatusConverter
import com.tangem.features.txhistory.converter.ExpressOnrampStatusToUiStatusConverter
import com.tangem.features.txhistory.converter.toUiStatus

/**
 * "Type" analytics param of the [TxHistoryAnalyticsEvent] family — the operation kind, independent of the token it
 * moves. Mirrors the domain [TxInfo.TransactionType] / [ExpressTx] split; anything not explicitly named by the
 * analytics spec (Send/Receive/Swap/Staking/Onramp/Approve) falls back to a best-effort label rather than a fixed enum,
 * since the spec's own value list is open-ended ("...").
 */
internal fun TxHistoryInfo.toAnalyticsType(): String = when (this) {
    is ExpressTx.Swap -> "Swap"
    is ExpressTx.Onramp -> "Onramp"
    is OnChainTx.TangemPay -> txInfo.toAnalyticsType()
    is OnChainTx.BSDK -> txInfo.type.toAnalyticsType(isOutgoing = txInfo.isOutgoing)
}

private fun TangemPayTxHistoryItem.toAnalyticsType(): String = when (this) {
    is TangemPayTxHistoryItem.Payment -> "Payment"
    is TangemPayTxHistoryItem.Collateral -> "Collateral"
    is TangemPayTxHistoryItem.Spend -> "Spend"
    is TangemPayTxHistoryItem.Fee -> "Fee"
}

private fun TxInfo.TransactionType.toAnalyticsType(isOutgoing: Boolean): String = when (this) {
    TxInfo.TransactionType.Transfer -> if (isOutgoing) "Send" else "Receive"
    is TxInfo.TransactionType.Approve -> "Approve"
    TxInfo.TransactionType.Swap -> "Swap"
    is TxInfo.TransactionType.Staking -> "Staking"
    is TxInfo.TransactionType.YieldSupply -> "Yield Supply"
    TxInfo.TransactionType.GaslessFee -> "Gasless Fee"
    is TxInfo.TransactionType.Operation -> name
    TxInfo.TransactionType.UnknownOperation -> "Unknown"
}

/**
 * "Status" analytics param — the same Confirmed/Pending/Failed bucket the detail header shows, plus "Unknown" for a
 * row the analytics mapping cannot yet classify (TangemPay; the detail card itself has no renderer for it either).
 */
internal fun TxHistoryInfo.toAnalyticsStatus(): String = when (this) {
    is OnChainTx.BSDK -> txInfo.status.toUiStatus()
    is OnChainTx.TangemPay -> null
    is ExpressTx.Swap -> ExpressExchangeStatusToUiStatusConverter().convert(tx.status)
    is ExpressTx.Onramp -> ExpressOnrampStatusToUiStatusConverter().convert(tx.status)
}.toAnalyticsStatusString()

private fun Status?.toAnalyticsStatusString(): String = when (this) {
    is Status.Confirmed -> "Confirmed"
    is Status.Failed -> "Failed"
    is Status.Unconfirmed -> "Pending"
    null -> "Unknown"
}

/** "Title" analytics param — the [type] label, qualified with the outcome once the transaction is settled. */
internal fun analyticsTitle(type: String, status: String): String = when (status) {
    "Failed" -> "$type Failed"
    "Pending" -> "$type Pending"
    else -> type
}