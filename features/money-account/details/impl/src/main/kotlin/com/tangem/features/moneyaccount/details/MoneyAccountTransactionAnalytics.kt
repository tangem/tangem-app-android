package com.tangem.features.moneyaccount.details

import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.visa.model.TangemPayTxHistoryItem

internal fun TxHistoryInfo.toTransactionInListClicked() = TangemPayAnalyticsEvents.TransactionInListClicked(
    product = TangemPayAnalyticsEvents.Product.ACCOUNT,
    type = toAccountTransactionType(),
    status = toAnalyticsStatus(),
)

private fun TxHistoryInfo.toAccountTransactionType(): String {
    val isSend = when (this) {
        is OnChainTx.BSDK -> txInfo.isOutgoing
        is OnChainTx.TangemPay -> txInfo.isOutgoing()
        is ExpressTx.Swap -> isOutgoing
        is ExpressTx.Onramp -> false
        is JointOperationTx -> true
    }
    return if (isSend) "send" else "deposit"
}

private fun TangemPayTxHistoryItem.isOutgoing(): Boolean = when (this) {
    is TangemPayTxHistoryItem.Collateral -> type == TangemPayTxHistoryItem.Type.Withdrawal
    is TangemPayTxHistoryItem.Spend,
    is TangemPayTxHistoryItem.Payment,
    is TangemPayTxHistoryItem.Fee,
    -> true
}

private fun TxHistoryInfo.toAnalyticsStatus(): String = when (this) {
    is OnChainTx.TangemPay -> (txInfo as? TangemPayTxHistoryItem.Spend)?.status.toAnalyticsStatus()
    is OnChainTx.BSDK,
    is ExpressTx,
    is JointOperationTx,
    -> onChainLeg()?.status.toAnalyticsStatus()
}

private fun TxHistoryInfo.onChainLeg(): TxInfo? {
    val leg = when (this) {
        is OnChainTx -> this
        is ExpressTx -> txInfo
        is JointOperationTx -> txInfo
    }
    return (leg as? OnChainTx.BSDK)?.txInfo
}

private fun TxInfo.TransactionStatus?.toAnalyticsStatus(): String = when (this) {
    TxInfo.TransactionStatus.Confirmed -> "completed"
    TxInfo.TransactionStatus.Unconfirmed -> "pending"
    TxInfo.TransactionStatus.Failed -> "declined"
    null -> "unknown"
}

private fun TangemPayTxHistoryItem.Status?.toAnalyticsStatus(): String = when (this) {
    TangemPayTxHistoryItem.Status.PENDING,
    TangemPayTxHistoryItem.Status.RESERVED,
    -> "pending"
    TangemPayTxHistoryItem.Status.COMPLETED -> "completed"
    TangemPayTxHistoryItem.Status.DECLINED,
    TangemPayTxHistoryItem.Status.REVERSED,
    -> "declined"
    TangemPayTxHistoryItem.Status.UNKNOWN,
    null,
    -> "unknown"
}