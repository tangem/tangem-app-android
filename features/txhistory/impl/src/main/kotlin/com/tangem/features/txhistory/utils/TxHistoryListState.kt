package com.tangem.features.txhistory.utils

import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.annotations.RemoveWithToggle

@Deprecated("Remove with toggle [TxHistoryFeatureToggles.isNewTxHistoryEnabled]. Used only by TxHistoryListManager.")
@RemoveWithToggle("AND_15767_NEW_TX_HISTORY_ENABLED")
data class TxHistoryListState(
    val status: PaginationStatus<*> = PaginationStatus.None,
    val rawBatches: List<Batch<Int, PaginationWrapper<TxInfo>>> = emptyList(),
    val uiBatches: List<Batch<Int, List<TxHistoryItemsUM.TxHistoryItemUM>>> = emptyList(),
    val legacyUiBatches: List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>> = emptyList(),
)