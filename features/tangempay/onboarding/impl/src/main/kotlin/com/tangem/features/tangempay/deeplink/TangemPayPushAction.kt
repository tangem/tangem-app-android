package com.tangem.features.tangempay.deeplink

import com.tangem.domain.visa.model.TangemPayTxHistoryItem

internal sealed class TangemPayPushAction {

    data object CardReady : TangemPayPushAction()

    data class TransactionSpend(
        val transaction: TangemPayTxHistoryItem.Spend,
        val customerId: String,
    ) : TangemPayPushAction()

    data class CollateralTransaction(
        val transaction: TangemPayTxHistoryItem.Collateral,
        val customerId: String,
    ) : TangemPayPushAction()
}