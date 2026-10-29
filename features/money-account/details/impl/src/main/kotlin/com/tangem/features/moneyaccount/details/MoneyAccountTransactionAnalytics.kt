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
    status = onChainLeg()?.status.toAnalyticsStatus(),
)

private fun TxHistoryInfo.toAccountTransactionType(): String {
    val isSend = when (this) {
        is OnChainTx.BSDK -> txInfo.isOutgoing
        is OnChainTx.TangemPay -> txInfo !is TangemPayTxHistoryItem.Collateral
        is ExpressTx.Swap -> isOutgoing
        is ExpressTx.Onramp -> false
        is JointOperationTx -> true
    }
    return if (isSend) "send" else "deposit"
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