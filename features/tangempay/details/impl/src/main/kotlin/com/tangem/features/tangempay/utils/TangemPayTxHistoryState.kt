package com.tangem.features.tangempay.utils

import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus

data class TangemPayTxHistoryState(
    val status: PaginationStatus<*> = PaginationStatus.None,
    val uiBatches: List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>> = listOf(),
)