package com.tangem.features.txhistory.utils

import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus

data class TxHistoryListState(
    val status: PaginationStatus<*> = PaginationStatus.None,
    val uiBatches: List<Batch<Int, List<TxHistoryItemsUM.TxHistoryItemUM>>> = emptyList(),
    val legacyUiBatches: List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>> = emptyList(),
)