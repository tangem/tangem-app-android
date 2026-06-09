package com.tangem.features.txhistory.utils

import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus

data class TxHistoryListState(
    val status: PaginationStatus<*> = PaginationStatus.None,
    val rawBatches: List<Batch<Int, PaginationWrapper<TxInfo>>> = emptyList(),
    val uiBatches: List<Batch<Int, List<TxHistoryItemsUM.TxHistoryItemUM>>> = emptyList(),
    val legacyUiBatches: List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>> = emptyList(),
)