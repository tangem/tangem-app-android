package com.tangem.features.tangempay.utils

import com.tangem.domain.visa.model.TangemPayTxHistoryItem

internal interface TangemPayTxHistoryUiActions {
    fun onTransactionClick(item: TangemPayTxHistoryItem)
}