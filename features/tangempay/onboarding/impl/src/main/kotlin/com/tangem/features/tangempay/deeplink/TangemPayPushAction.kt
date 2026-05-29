package com.tangem.features.tangempay.deeplink

import com.tangem.domain.visa.model.TangemPayTxHistoryItem

internal sealed class TangemPayPushAction {

    data object CardReady : TangemPayPushAction()

    data class TransactionSpend(
        val transaction: TangemPayTxHistoryItem,
        val customerId: String,
    ) : TangemPayPushAction()

    data object TopUp : TangemPayPushAction()

    data class CollateralTransaction(
        val transaction: TangemPayTxHistoryItem,
        val customerId: String,
    ) : TangemPayPushAction()
}