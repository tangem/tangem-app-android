package com.tangem.features.tangempay.txhistory

import com.tangem.domain.visa.model.TangemPayTxHistoryItem

internal interface TangemPayTxHistoryUiActions {
    fun onTransactionClick(item: TangemPayTxHistoryItem)
}