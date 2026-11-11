package com.tangem.features.tangempay.txhistory

import com.tangem.domain.tangempay.model.TangemPayTxHistoryItem

internal interface TangemPayTxHistoryUiActions {
    fun onTransactionClick(item: TangemPayTxHistoryItem)
}